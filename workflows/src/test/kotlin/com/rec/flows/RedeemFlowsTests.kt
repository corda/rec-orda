package com.rec.flows

import com.r3.corda.lib.tokens.workflows.flows.redeem.RedeemTokensFlowHandler
import com.rec.flows.FlowTestHelpers.NodeHolding
import com.rec.flows.FlowTestHelpers.assertHasStatesInVault
import com.rec.flows.FlowTestHelpers.createFrom
import com.rec.flows.FlowTestHelpers.issueTokens
import com.rec.flows.FlowTestHelpers.prepareMockNetworkParameters
import com.rec.flows.RedeemFlows.Initiator
import com.rec.states.EnergySource
import com.rec.states.FungibleRECToken
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.function.Consumer

class RedeemFlowsTests {
    private val network: MockNetwork = MockNetwork(prepareMockNetworkParameters)
    private val alice: StartedMockNode = network.createNode()
    private val bob: StartedMockNode = network.createNode()
    private val carly: StartedMockNode = network.createNode()
    private val dan: StartedMockNode = network.createNode()
    private val source: EnergySource = EnergySource.values()[0]

    init {
        listOf(alice, bob, carly, dan).forEach(Consumer { it.registerInitiatedFlow(Initiator::class.java, RedeemTokensFlowHandler::class.java) })
    }
    
    @Before
    fun setup() {
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    @Throws(Throwable::class)
    fun signedTransactionReturnedByTheFlowIsSignedByBothTheIssuerAndTheHolder() {
        val tokens = issueTokens(
                alice, network, listOf(NodeHolding(bob, 10L)), source)
        val flow = Initiator(tokens)
        val future = bob.startFlow(flow)
        network.runNetwork()
        val tx = future.get()!!
        tx.verifySignaturesExcept(bob.info.legalIdentities[0].owningKey)
        tx.verifySignaturesExcept(alice.info.legalIdentities[0].owningKey)
    }

    @Test
    @Throws(Throwable::class)
    fun signedTransactionReturnedByTheFlowIsSignedByBothIssuersAndTheHolder() {
        val tokens1 = issueTokens(alice, network, listOf(NodeHolding(bob, 10L)), source)
        val tokens = tokens1 + issueTokens(carly, network, listOf(NodeHolding(bob, 20L)), source)

        val flow = Initiator(listOf(tokens[0]))

        val future = bob.startFlow(flow)

        network.runNetwork()

        val tx = future.get()!!


        tx.verifySignaturesExcept(listOf(
          bob.info.legalIdentities[0].owningKey,
          carly.info.legalIdentities[0].owningKey))
        tx.verifySignaturesExcept(listOf(
          alice.info.legalIdentities[0].owningKey,
          carly.info.legalIdentities[0].owningKey))
        tx.verifySignaturesExcept(listOf(
          alice.info.legalIdentities[0].owningKey,
          bob.info.legalIdentities[0].owningKey))
    }

    @Test
    @Throws(Throwable::class)
    fun flowRecordsATransactionInIssuerAndHolderTransactionStoragesOnly() {
        val tokens = issueTokens(alice, network, listOf(NodeHolding(bob, 10L)), source)
        val flow = Initiator(tokens)
        val future = bob.startFlow(flow)

        network.runNetwork()

        val tx = future.get()!!

        // We check the recorded transaction in both transaction storages.
        for (node in listOf(alice, bob)) {
            assertEquals(tx, node.services.validatedTransactions.getTransaction(tx.id))
        }
        for (node in listOf(carly, dan)) {
            assertNull(node.services.validatedTransactions.getTransaction(tx.id))
        }
    }

    @Test
    @Throws(Throwable::class)
    fun flowRecordsATransactionInBothIssuersAndHolderTransactionStoragesOnly() {
        val tokens1 = issueTokens(alice, network, listOf(NodeHolding(bob, 10L)), source)
        val tokens = tokens1 + (issueTokens(carly, network, listOf(NodeHolding(bob, 20L)), source))

        val flow = Initiator(listOf(tokens[0]))

        val future = bob.startFlow(flow)

        network.runNetwork()

        val tx = future.get()!!

        // We check the recorded transaction in both transaction storages.
        for (node in listOf(alice, bob)) {
            assertEquals(tx, node.services.validatedTransactions.getTransaction(tx.id))
        }
        assertNull(dan.services.validatedTransactions.getTransaction(tx.id))
    }

    @Test
    @Throws(Throwable::class)
    fun flowRecordsATransactionInIssuerAndBothHolderTransactionStorages() {
        val tokens = issueTokens(
                alice, network, listOf(
                NodeHolding(bob, 10L),
                NodeHolding(carly, 20L)), source
        )

        val flow = Initiator(listOf(tokens[0]))

        val future = bob.startFlow(flow)

        network.runNetwork()

        val tx = future.get()!!

        // We check the recorded transaction in transaction storages.
        for (node in listOf(alice, bob)) {
            assertEquals(tx, node.services.validatedTransactions.getTransaction(tx.id))
        }
        assertNull(dan.services.validatedTransactions.getTransaction(tx.id))
    }

    @Test
    @Throws(Throwable::class)
    fun recordedTransactionHasASingleInputTheFungibleRECTokenAndNoOutputs() {
        val expected = createFrom(alice, bob, 10L, source)

        val tokens: List<StateAndRef<FungibleRECToken>> = issueTokens(
                alice, network, listOf(NodeHolding(bob, 10L)), source
        )

        val flow = Initiator(tokens)

        val future = bob.startFlow(flow)

        network.runNetwork()

        val tx = future.get()!!

        // We check the recorded transaction in both vaults.
        for (node in listOf(alice, bob)) {
            val recordedTx = node.services.validatedTransactions.getTransaction(tx.id)
            val txInputs = recordedTx!!.tx.inputs
            assertEquals(1, txInputs.size.toLong())
            assertEquals(expected, node.services.toStateAndRef<ContractState>(txInputs[0]).state.data)
            assertTrue(recordedTx.tx.outputs.isEmpty())
        }
    }

    @Test
    @Throws(Throwable::class)
    fun thereIsNoRecordedStateAfterRedeem() {
        val tokens = issueTokens(alice, network, listOf(NodeHolding(bob, 10L)), source)

        val flow = Initiator(tokens)

        val future = bob.startFlow(flow)

        network.runNetwork()

        future.get()!!

        // We check the state was consumed in both vaults.
        assertHasStatesInVault(alice, emptyList())
        assertHasStatesInVault(alice, emptyList())
    }

    @Test
    @Throws(Throwable::class)
    fun recordedTransactionHasManyInputsTheFungibleRECTokensAndNoOutputs() {
        val expected = createFrom(alice, bob, 10L, source)

        val tokens: List<StateAndRef<FungibleRECToken>> = issueTokens(
                alice, network, listOf(
                NodeHolding(bob, 10L),
                NodeHolding(carly, 20L)), source
        )

        val flow = Initiator(listOf(tokens[0]))

        val future = bob.startFlow(flow)

        network.runNetwork()

        val tx = future.get()!!

        // We check the recorded transaction in the 3 vaults.
        for (node in listOf(alice, bob)) {
            val recordedTx = node.services.validatedTransactions.getTransaction(tx.id)
            val txInputs = recordedTx!!.tx.inputs
            assertEquals(1, txInputs.size.toLong())
            assertEquals(expected, node.services.toStateAndRef<ContractState>(txInputs[0]).state.data)
            assertTrue(recordedTx.tx.outputs.isEmpty())
        }
    }

    @Test
    @Throws(Throwable::class)
    fun thereAreNoRecordedStatesAfterRedeem() {
        val expected = createFrom(alice, carly, 20L, source)
        val tokens = issueTokens(
                alice, network, listOf(
                NodeHolding(bob, 10L),
                NodeHolding(carly, 20L)), source
        )

        val flow = Initiator(listOf(tokens[0]))

        val future = bob.startFlow(flow)

        network.runNetwork()

        future.get()!!

        // We check the recorded state in the 4 vaults.
        assertHasStatesInVault(alice, listOf(expected))
        assertHasStatesInVault(bob, emptyList())
        assertHasStatesInVault(carly, listOf(expected))
        assertHasStatesInVault(dan, emptyList())
    }


}