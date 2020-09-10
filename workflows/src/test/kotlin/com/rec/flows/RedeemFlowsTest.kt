package com.rec.flows

import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.redeem.RedeemTokensFlowHandler
import com.rec.flows.FlowTestHelpers.NodeHolding
import com.rec.flows.FlowTestHelpers.assertHasStatesInVault
import com.rec.flows.FlowTestHelpers.createFrom
import com.rec.flows.FlowTestHelpers.issueTokens
import com.rec.flows.FlowTestHelpers.prepareMockNetworkParameters
import com.rec.flows.RedeemFlows.Initiator
import com.rec.states.EnergySource
import com.rec.states.RECToken
import net.corda.core.contracts.ContractState
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.function.Consumer

class RedeemFlowsTest {
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
    fun `signed transaction returned by the flow is signed by both the issuer and the holder`() {
        issueTokens(alice, network, listOf(NodeHolding(bob, 10L)), source)
        val flow = Initiator(10L of RECToken(source), alice.info.legalIdentities.single())
        val future = bob.startFlow(flow)
        network.runNetwork()
        val tx = future.get()!!
        tx.verifySignaturesExcept(bob.info.legalIdentities[0].owningKey)
        tx.verifySignaturesExcept(alice.info.legalIdentities[0].owningKey)
    }

    @Test
    @Throws(Throwable::class)
    fun `signed transaction returned by the flow is signed by both issuers and the holder`() {
        val tokens1 = issueTokens(alice, network, listOf(NodeHolding(bob, 10L)), source)
        tokens1 + issueTokens(carly, network, listOf(NodeHolding(bob, 20L)), source)
        val flow = Initiator(10L of RECToken(source), alice.info.legalIdentities.single())
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
    fun `flow records a transaction in issuer and holder transaction storage only`() {
        issueTokens(alice, network, listOf(NodeHolding(bob, 10L)), source)
        val flow = Initiator(10L of RECToken(source), alice.info.legalIdentities.single())
        val future = bob.startFlow(flow)
        network.runNetwork()
        val tx = future.get()!!

        // We check the recorded transaction in both transaction storage.
        for (node in listOf(alice, bob)) {
            assertEquals(tx, node.services.validatedTransactions.getTransaction(tx.id))
        }
        for (node in listOf(carly, dan)) {
            assertNull(node.services.validatedTransactions.getTransaction(tx.id))
        }
    }

    @Test
    @Throws(Throwable::class)
    fun `flow records a transaction in both issuers and holder transaction storage only`() {
        val tokens1 = issueTokens(alice, network, listOf(NodeHolding(bob, 10L)), source)
        tokens1 + (issueTokens(carly, network, listOf(NodeHolding(bob, 20L)), source))

        val flow = Initiator(10L of RECToken(source), alice.info.legalIdentities.single())
        val future = bob.startFlow(flow)
        network.runNetwork()
        val tx = future.get()!!

        // We check the recorded transaction in both transaction storage.
        for (node in listOf(alice, bob)) {
            assertEquals(tx, node.services.validatedTransactions.getTransaction(tx.id))
        }
        assertNull(dan.services.validatedTransactions.getTransaction(tx.id))
    }

    @Test
    @Throws(Throwable::class)
    fun `flow records a transaction in issuer and both holder transaction storage`() {
        issueTokens(alice, network, listOf(NodeHolding(bob, 10L), NodeHolding(carly, 20L)), source)
        val flow = Initiator(10L of RECToken(source), alice.info.legalIdentities.single())
        val future = bob.startFlow(flow)
        network.runNetwork()
        val tx = future.get()!!

        // We check the recorded transaction in transaction storage.
        for (node in listOf(alice, bob)) {
            assertEquals(tx, node.services.validatedTransactions.getTransaction(tx.id))
        }
        assertNull(dan.services.validatedTransactions.getTransaction(tx.id))
    }

    @Test
    @Throws(Throwable::class)
    fun `recorded transaction has a single input the fungible RECToken and no outputs`() {
        val expected = createFrom(alice, bob, 10L, source)

        issueTokens(alice, network, listOf(NodeHolding(bob, 10L)), source)
        val flow = Initiator(10L of RECToken(source), alice.info.legalIdentities.single())

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
    fun `there is no recorded state after redeem`() {
        issueTokens(alice, network, listOf(NodeHolding(bob, 10L)), source)

        val flow = Initiator(10L of RECToken(source), alice.info.legalIdentities.single())

        val future = bob.startFlow(flow)

        network.runNetwork()

        future.get()!!

        // We check the state was consumed in both vaults.
        assertHasStatesInVault(alice, emptyList())
        assertHasStatesInVault(bob, emptyList())
    }

    @Test
    @Throws(Throwable::class)
    fun `recorded transaction has many inputs the fungible RECTokens and no outputs`() {
        val expected = createFrom(alice, bob, 10L, source)

        issueTokens(alice, network, listOf(NodeHolding(bob, 10L), NodeHolding(carly, 20L)), source)
        val flow = Initiator(10L of RECToken(source), alice.info.legalIdentities.single())
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
    fun `there are no recorded states after redeem`() {
        val expected = createFrom(alice, carly, 20L, source)
        val tokens = issueTokens(
                alice, network, listOf(
                NodeHolding(bob, 10L),
                NodeHolding(carly, 20L)), source
        )

        val flow = Initiator(10L of RECToken(source), alice.info.legalIdentities.single())

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