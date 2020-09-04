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
import net.corda.node.services.Permissions.Companion.invokeRpc
import net.corda.node.services.Permissions.Companion.startFlow
import net.corda.testing.core.TestIdentity
import net.corda.testing.core.expect
import net.corda.testing.core.expectEvents
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.User
import org.junit.Test
import rx.Observable
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

class DriverBasedTest {
    private val prosumer = TestIdentity(CordaX500Name("Prosumer", "", "FR"))
    private val utilityProvider = TestIdentity(CordaX500Name("Utility Provider", "", "GB"))

    private val aliceUser = User("aliceUser", "testPassword1", permissions = setOf(
            startFlow<IssueFlows.Initiator>(),
            startFlow<MoveFlows.Initiator>(),
            startFlow<MoveFlows.Responder>(),
            startFlow<RedeemFlows.Responder>(),
            invokeRpc("vaultTrackBy"),
            invokeRpc("vaultQueryBy")
    ))

    private val bobUser = User("bobUser", "testPassword2", permissions = setOf(
            startFlow<MoveFlows.Initiator>(),
            startFlow<MoveFlows.Initiator>(),
            startFlow<RedeemFlows.Initiator>(),
            invokeRpc("vaultTrackBy"),
            invokeRpc("vaultQueryBy")
    ))

    @Test
    fun `issue integration test`() = withDriver {

        val (alice, bob) = startNodes(
                prosumer withUsers listOf(aliceUser),
                utilityProvider withUsers listOf(bobUser)
        )

        val aliceProxy: CordaRPCOps = alice.getProxy("aliceUser", "testPassword1")
        val bobProxy: CordaRPCOps = bob.getProxy("bobUser", "testPassword2")

        val aliceVaultUpdates: Observable<Vault.Update<FungibleRECToken>> = aliceProxy.vaultTrackBy<FungibleRECToken>().updates
//        val bobVaultUpdates: Observable<Vault.Update<FungibleRECToken>> = bobProxy.vaultTrackBy<FungibleRECToken>().updates


        // issue
        aliceProxy.startFlowDynamic(
                IssueFlows.Initiator::class.java,
                alice.nodeInfo.singleIdentity(),
                10L,
                EnergySource.WIND
        ).returnValue.getOrThrow()

        aliceVaultUpdates.expectEvents {
            expect { update ->
                val produced: FungibleRECToken = update.produced.first().state.data
                assertEquals(alice.nodeInfo.legalIdentities.single(), produced.holder)
                assertEquals(RECToken(EnergySource.WIND), produced.recToken)
                assertEquals(10L, produced.amount.quantity)
            }
        }
    }

    @Test
    fun `move integration test`() = withDriver {

        val (alice, bob) = startNodes(
                prosumer withUsers listOf(aliceUser),
                utilityProvider withUsers listOf(bobUser)
        )

        val aliceProxy: CordaRPCOps = alice.getProxy("aliceUser", "testPassword1")
        val bobProxy: CordaRPCOps = bob.getProxy("bobUser", "testPassword2")

        val aliceVaultUpdates: Observable<Vault.Update<FungibleRECToken>> = aliceProxy.vaultTrackBy<FungibleRECToken>().updates
        val bobVaultUpdates: Observable<Vault.Update<FungibleRECToken>> = bobProxy.vaultTrackBy<FungibleRECToken>().updates


        // issue
        aliceProxy.startFlowDynamic(
                IssueFlows.Initiator::class.java,
                alice.nodeInfo.singleIdentity(),
                10L,
                EnergySource.WIND
        ).returnValue.getOrThrow()

        // move
        val moveInput: List<StateAndRef<FungibleRECToken>> = aliceProxy.vaultQueryBy<FungibleRECToken>().states
        val moveOutput: List<FungibleRECToken> = moveInput.map { it.state.data withNewHolder bob.nodeInfo.singleIdentity() }

        aliceProxy.startFlowDynamic(
                MoveFlows.Initiator::class.java,
                moveInput,
                moveOutput
        ).returnValue.getOrThrow()

        bobVaultUpdates.expectEvents {
            expect { update ->
                val produced: FungibleRECToken = update.produced.first().state.data
                assertEquals(bob.nodeInfo.legalIdentities.single(), produced.holder)
                assertEquals(RECToken(EnergySource.WIND), produced.recToken)
                assertEquals(10L, produced.amount.quantity)
            }
        }

        aliceVaultUpdates.expectEvents {
            expect { update ->
                val produced: FungibleRECToken = update.produced.first().state.data
                assertEquals(alice.nodeInfo.legalIdentities.single(), produced.holder)
                assertEquals(RECToken(EnergySource.WIND), produced.recToken)
                assertEquals(10L, produced.amount.quantity)
            }
        }
    }

    @Test
    fun `redeem integration test`() = withDriver {

        val (alice, bob) = startNodes(
                prosumer withUsers listOf(aliceUser),
                utilityProvider withUsers listOf(bobUser)
        )

        val aliceProxy: CordaRPCOps = alice.getProxy("aliceUser", "testPassword1")
        val bobProxy: CordaRPCOps = bob.getProxy("bobUser", "testPassword2")

//        val aliceVaultUpdates: Observable<Vault.Update<FungibleRECToken>> = aliceProxy.vaultTrackBy<FungibleRECToken>().updates
        val bobVaultUpdates: Observable<Vault.Update<FungibleRECToken>> = bobProxy.vaultTrackBy<FungibleRECToken>().updates

        // issue
        aliceProxy.startFlowDynamic(
                IssueFlows.Initiator::class.java,
                alice.nodeInfo.singleIdentity(),
                10L,
                EnergySource.WIND
        ).returnValue.get(10L, TimeUnit.SECONDS)

        // move
        val moveInput: List<StateAndRef<FungibleRECToken>> = aliceProxy.vaultQueryBy<FungibleRECToken>().states
        val moveOutput: List<FungibleRECToken> = moveInput.map { it.state.data withNewHolder bob.nodeInfo.singleIdentity() }

        aliceProxy.startFlowDynamic(
                MoveFlows.Initiator::class.java,
                moveInput,
                moveOutput
        ).returnValue.getOrThrow()

        // need to wait until the token actually moves. Takes some time, could do Thread.sleep(2000L).
        bobVaultUpdates.expectEvents {
            expect { update ->
                val produced: FungibleRECToken = update.produced.first().state.data
                assertEquals(bob.nodeInfo.legalIdentities.single(), produced.holder)
                assertEquals(RECToken(EnergySource.WIND), produced.recToken)
                assertEquals(10L, produced.amount.quantity)
            }
        }

        // make sure you wait long enough otherwise bob's vault will be empty.
        // redeem
        val redeemInput: List<StateAndRef<FungibleRECToken>> = bobProxy.vaultQueryBy<FungibleRECToken>().states

        bobProxy.startFlowDynamic(
                RedeemFlows.Initiator::class.java,
                redeemInput
        ).returnValue.getOrThrow()
    }


}