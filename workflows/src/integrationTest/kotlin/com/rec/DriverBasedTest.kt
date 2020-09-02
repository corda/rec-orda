package com.rec

import com.rec.DriverTestHelpers.Companion.getProxy
import com.rec.DriverTestHelpers.Companion.startNodes
import com.rec.DriverTestHelpers.Companion.withDriver
import com.rec.DriverTestHelpers.Companion.withUsers
import com.rec.flows.IssueFlows
import com.rec.flows.MoveFlows
import com.rec.flows.RedeemFlows
import com.rec.states.EnergySource
import com.rec.states.FungibleRECToken
import com.rec.states.RECToken
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.messaging.vaultTrackBy
import net.corda.core.node.services.Vault
import net.corda.core.utilities.getOrThrow
import net.corda.node.services.Permissions
import net.corda.testing.core.TestIdentity
import net.corda.testing.core.expect
import net.corda.testing.core.expectEvents
import net.corda.testing.core.singleIdentity
import net.corda.testing.driver.NodeHandle
import net.corda.testing.node.User
import org.junit.Test
import rx.Observable
import kotlin.test.assertEquals

class DriverBasedTest {
    private val prosumer = TestIdentity(CordaX500Name("Prosumer", "", "FR"))
    private val utilityProvider = TestIdentity(CordaX500Name("Utility Provider", "", "GB"))

    private val aliceUser = User("aliceUser", "testPassword1", permissions = setOf(
            Permissions.startFlow<IssueFlows.Initiator>(),
            Permissions.startFlow<MoveFlows.Initiator>(),
            Permissions.invokeRpc("vaultTrackBy")
    ))

    private val bobUser = User("bobUser", "testPassword2", permissions = setOf(
            Permissions.startFlow<MoveFlows.Initiator>(),
            Permissions.startFlow<RedeemFlows.Initiator>(),
            Permissions.invokeRpc("vaultTrackBy")
    ))

    @Test
    fun `issue, move and redeem integration test`() = withDriver {

        val (alice, bob) = startNodes(
                prosumer withUsers listOf(aliceUser),
                utilityProvider withUsers listOf(bobUser)
        )

        val aliceProxy: CordaRPCOps = alice.getProxy("aliceUser", "testPassword1")
        val bobProxy: CordaRPCOps = bob.getProxy("bobUser", "testPassword2")

        val aliceVaultUpdates: Observable<Vault.Update<FungibleRECToken>> = aliceProxy.vaultTrackBy<FungibleRECToken>().updates

        issue(aliceProxy, alice, aliceVaultUpdates)


    }

    private fun issue(aliceProxy: CordaRPCOps, alice: NodeHandle, aliceVaultUpdates: Observable<Vault.Update<FungibleRECToken>>) {
        aliceProxy.startFlowDynamic(
                IssueFlows.Initiator::class.java,
                alice.nodeInfo.singleIdentity(),
                10L,
                EnergySource.WIND
        ).returnValue.getOrThrow()

        aliceVaultUpdates.expectEvents {
            expect { update ->
                val token: FungibleRECToken = update.produced.first().state.data
                assertEquals(RECToken(EnergySource.WIND), token.recToken)
                assertEquals(10L, token.amount.quantity)
            }
        }
    }


}