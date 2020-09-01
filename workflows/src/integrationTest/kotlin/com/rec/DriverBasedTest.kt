package com.rec

import com.rec.flows.IssueFlows
import com.rec.flows.MoveFlows
import com.rec.states.EnergySource
import com.rec.states.FungibleRECToken
import com.rec.states.RECToken
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.vaultTrackBy
import net.corda.core.node.services.Vault
import net.corda.core.utilities.getOrThrow
import net.corda.node.services.Permissions.Companion.invokeRpc
import net.corda.node.services.Permissions.Companion.startFlow
import net.corda.testing.core.*
import net.corda.testing.driver.DriverDSL
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import net.corda.testing.node.NotarySpec
import net.corda.testing.node.User
import org.junit.Test
import rx.Observable
import java.util.concurrent.Future
import kotlin.test.assertEquals
import com.rec.flows.FlowTestHelpers.prepareMockNetworkParameters as parameters

class DriverBasedTest {
    private val bankA = TestIdentity(CordaX500Name("BankA", "", "GB"))
    private val bankB = TestIdentity(CordaX500Name("BankB", "", "US"))

    @Test
    fun `node test`() = withDriver {
        // Start a pair of nodes and wait for them both to be ready.
        val (partyAHandle, partyBHandle) = startNodes(bankA, bankB)

        // From each node, make an RPC call to retrieve another node's name from the network map, to verify that the
        // nodes have started and can communicate.

        // This is a very basic test: in practice tests would be starting flows, and verifying the states in the vault
        // and other important metrics to ensure that your CorDapp is working as intended.
        assertEquals(bankB.name, partyAHandle.resolveName(bankB.name))
        assertEquals(bankA.name, partyBHandle.resolveName(bankA.name))
    }

    // Runs a test inside the Driver DSL, which provides useful functions for starting nodes, etc.
    private fun withDriver(test: DriverDSL.() -> Unit) = driver(
            DriverParameters(
                    isDebug = true,
                    startNodesInProcess = true,
                    networkParameters = parameters.networkParameters,
                    cordappsForAllNodes = parameters.cordappsForAllNodes,
                    notarySpecs = parameters.notarySpecs.map { NotarySpec(it.name) }
            )
    ) { test() }

    // Makes an RPC call to retrieve another node's name from the network map.
    private fun NodeHandle.resolveName(name: CordaX500Name) = rpc.wellKnownPartyFromX500Name(name)!!.name

    // Resolves a list of futures to a list of the promised values.
    private fun <T> List<Future<T>>.waitForAll(): List<T> = map { it.getOrThrow() }

    // Starts multiple nodes simultaneously, then waits for them all to be ready.
    private fun DriverDSL.startNodes(vararg identities: TestIdentity) = identities
            .map { startNode(providedName = it.name) }
            .waitForAll()

    @Test
    fun `issue test`() = withDriver {
        val aliceUser = User(
                "aliceUser",
                "testPassword1",
                permissions = setOf(startFlow<IssueFlows.Initiator>(), invokeRpc("vaultTrackBy")))

        val bobUser = User(
                "bobUser",
                "testPassword2",
                permissions = setOf(startFlow<MoveFlows.Initiator>(), invokeRpc("vaultTrackBy")))

        val (alice, bob) = listOf(
                startNode(providedName = ALICE_NAME, rpcUsers = listOf(aliceUser)),
                startNode(providedName = BOB_NAME, rpcUsers = listOf(bobUser))
        ).map { it.getOrThrow() }

        val aliceClient = CordaRPCClient(alice.rpcAddress)
        val aliceProxy: CordaRPCOps = aliceClient.start("aliceUser", "testPassword1").proxy

        val bobClient = CordaRPCClient(bob.rpcAddress)
        val bobProxy: CordaRPCOps = bobClient.start("bobUser", "testPassword2").proxy

        val bobVaultUpdates: Observable<Vault.Update<FungibleRECToken>> = bobProxy.vaultTrackBy<FungibleRECToken>().updates
//        val aliceVaultUpdates: Observable<Vault.Update<FungibleRECToken>> = aliceProxy.vaultTrackBy<FungibleRECToken>().updates

        aliceProxy.startFlowDynamic(
                IssueFlows.Initiator::class.java,
                bob.nodeInfo.singleIdentity(),
                10L,
                EnergySource.WIND
        ).returnValue.getOrThrow()

        bobVaultUpdates.expectEvents {
            expect { update ->
                println("Bob got vault update of $update")
                val token: FungibleRECToken = update.produced.first().state.data
                assertEquals(RECToken(EnergySource.WIND), token.recToken)
                assertEquals(10L, token.amount.quantity)
            }
        }

    }
}