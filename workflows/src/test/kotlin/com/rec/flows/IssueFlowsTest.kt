package com.rec.flows

import com.google.common.collect.ImmutableList
import com.rec.flows.FlowTestHelpers.assertHasStatesInVault
import com.rec.flows.FlowTestHelpers.createFrom
import com.rec.flows.FlowTestHelpers.prepareMockNetworkParameters
import com.rec.flows.FlowTestHelpers.toPair
import com.rec.states.EnergySource
import com.rec.states.FungibleRECToken
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class IssueFlowsTest {
    private val network: MockNetwork = MockNetwork(prepareMockNetworkParameters)
    private val alice: StartedMockNode
    private val bob: StartedMockNode
    private val carly: StartedMockNode
    private val dan: StartedMockNode
    private val source: EnergySource = EnergySource.values()[0]

    @Before
    fun setup() {
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    @Throws(Exception::class)
    fun signedTransactionPickedThePreferredNotary() {
        val flow = IssueFlows.Initiator(bob.info.legalIdentities[0], 10L, source)
        val future = alice.startFlow(flow)
        network.runNetwork()
        val tx = future.get()
        Assert.assertEquals("O=App Notary, L=London, C=GB", tx.notary!!.name.toString())
    }

    @Test
    @Throws(Exception::class)
    fun signedTransactionReturnedByTheFlowIsSignedByTheIssuer() {
        val flow: IssueFlows.Initiator = IssueFlows.Initiator(bob.info.legalIdentities[0], 10L, source)
        val future = alice.startFlow(flow)
        network.runNetwork()
        val tx = future.get()
        tx.verifyRequiredSignatures()
    }

    @Test
    @Throws(Exception::class)
    fun flowRecordsATransactionInIssuerAndHolderTransactionStoragesOnly() {
        val flow: IssueFlows.Initiator = IssueFlows.Initiator(bob.info.legalIdentities[0], 10L, source)
        val future = alice.startFlow(flow)
        network.runNetwork()
        val tx = future.get()

        // We check the recorded transaction in both transaction storages.
        for (node in ImmutableList.of(alice, bob)) {
            Assert.assertEquals(tx, node.services.validatedTransactions.getTransaction(tx.id))
        }
        for (node in ImmutableList.of(carly, dan)) {
            Assert.assertNull(node.services.validatedTransactions.getTransaction(tx.id))
        }
    }

    @Test
    @Throws(Exception::class)
    fun flowRecordsATransactionInIssuerAndBothHolderTransactionStorages() {
        val flow: IssueFlows.Initiator = IssueFlows.Initiator(listOf(
          Pair(bob.info.legalIdentities[0], 10L),
          Pair(carly.info.legalIdentities[0], 20L)), source = source)
        val future = alice.startFlow(flow)
        network.runNetwork()
        val tx = future.get()

        // We check the recorded transaction in transaction storages.
        for (node in ImmutableList.of(alice, bob, carly)) {
            Assert.assertEquals(tx, node.services.validatedTransactions.getTransaction(tx.id))
        }
        Assert.assertNull(dan.services.validatedTransactions.getTransaction(tx.id))
    }

    @Test
    @Throws(Exception::class)
    fun recordedTransactionHasNoInputsAndASingleOutputTheFungibleRECToken() {
        val expected: FungibleRECToken = createFrom(alice, bob, 10L, source)
        val flow: IssueFlows.Initiator = IssueFlows.Initiator(
                expected.holder, expected.amount.quantity, source)
        val future = alice.startFlow(flow)
        network.runNetwork()
        val tx = future.get()

        // We check the recorded transaction in both vaults.
        for (node in listOf(alice, bob)) {
            val recordedTx = node.services.validatedTransactions.getTransaction(tx.id)
            Assert.assertNotNull(recordedTx)
            Assert.assertTrue(recordedTx!!.tx.inputs.isEmpty())
            val txOutputs = recordedTx.tx.outputs
            Assert.assertEquals(1, txOutputs.size.toLong())
            Assert.assertEquals(expected, txOutputs[0].data)
        }
    }

    @Test
    @Throws(Exception::class)
    fun thereIs1CorrectRecordedState() {
        val expected: FungibleRECToken = createFrom(alice, bob, 10L, source)
        val flow: IssueFlows.Initiator = IssueFlows.Initiator(
                expected.holder, expected.amount.quantity, source)
        val future = alice.startFlow(flow)
        network.runNetwork()
        future.get()

        // We check the recorded state in both vaults.
        assertHasStatesInVault(alice, ImmutableList.of(expected))
        assertHasStatesInVault(bob, ImmutableList.of(expected))
    }

    @Test
    @Throws(Exception::class)
    fun recordedTransactionHasNoInputsAndManyOutputsTheFungibleRECTokens() {
        val expected1: FungibleRECToken = createFrom(alice, bob, 10L, source)
        val expected2: FungibleRECToken = createFrom(alice, carly, 20L, source)
        val flow: IssueFlows.Initiator = IssueFlows.Initiator(listOf(
          expected1.toPair(),
          expected2.toPair()), source = source)
        val future = alice.startFlow(flow)
        network.runNetwork()
        val tx = future.get()

        // We check the recorded transaction in the 3 vaults.
        for (node in ImmutableList.of(alice, bob, carly)) {
            val recordedTx = node.services.validatedTransactions.getTransaction(tx.id)
            Assert.assertNotNull(recordedTx)
            Assert.assertTrue(recordedTx!!.tx.inputs.isEmpty())
            val txOutputs = recordedTx.tx.outputs
            Assert.assertEquals(2, txOutputs.size.toLong())
            Assert.assertEquals(expected1, txOutputs[0].data)
            Assert.assertEquals(expected2, txOutputs[1].data)
        }
    }

    @Test
    @Throws(Exception::class)
    fun thereAre2CorrectStatesRecordedByRelevance() {
        val expected1: FungibleRECToken = createFrom(alice, bob, 10L, source)
        val expected2: FungibleRECToken = createFrom(alice, carly, 20L, source)
        val flow: IssueFlows.Initiator = IssueFlows.Initiator(listOf(
          expected1.toPair(),
          expected2.toPair()), source = source)
        val future = alice.startFlow(flow)
        network.runNetwork()
        future.get()

        // We check the recorded state in the 4 vaults.
        assertHasStatesInVault(alice, ImmutableList.of(expected1, expected2))
        // Notice how bob did not save carly's state.
        assertHasStatesInVault(bob, ImmutableList.of(expected1))
        assertHasStatesInVault(carly, ImmutableList.of(expected2))
        assertHasStatesInVault(dan, emptyList())
    }

    @Test
    @Throws(Exception::class)
    fun recordedTransactionHasNoInputsAnd2OutputsOfSameHolderTheFungibleRECTokens() {
        val expected1: FungibleRECToken = createFrom(alice, bob, 10L, source)
        val expected2: FungibleRECToken = createFrom(alice, bob, 20L, source)
        val flow: IssueFlows.Initiator = IssueFlows.Initiator(listOf(
          expected1.toPair(),
          expected2.toPair()), source = source)
        val future = alice.startFlow(flow)
        network.runNetwork()
        val tx = future.get()

        // We check the recorded transaction in both vaults.
        for (node in ImmutableList.of(alice, bob)) {
            val recordedTx = node.services.validatedTransactions.getTransaction(tx.id)
            Assert.assertNotNull(recordedTx)
            Assert.assertTrue(recordedTx!!.tx.inputs.isEmpty())
            val txOutputs = recordedTx.tx.outputs
            Assert.assertEquals(2, txOutputs.size.toLong())
            Assert.assertEquals(expected1, txOutputs[0].data)
            Assert.assertEquals(expected2, txOutputs[1].data)
        }
    }

    @Test
    @Throws(Exception::class)
    fun thereAre2CorrectRecordedStatesAgain() {
        val expected1: FungibleRECToken = createFrom(alice, bob, 10L, source)
        val expected2: FungibleRECToken = createFrom(alice, bob, 20L, source)
        val flow: IssueFlows.Initiator = IssueFlows.Initiator(listOf(
          expected1.toPair(),
          expected2.toPair()), source = source)
        val future = alice.startFlow(flow)
        network.runNetwork()
        future.get()

        // We check the recorded state in the 4 vaults.
        assertHasStatesInVault(alice, ImmutableList.of(expected1, expected2))
        assertHasStatesInVault(bob, ImmutableList.of(expected1, expected2))
        assertHasStatesInVault(carly, emptyList())
        assertHasStatesInVault(dan, emptyList())
    }

    init {
        alice = network.createNode()
        bob = network.createNode()
        carly = network.createNode()
        dan = network.createNode()
    }
}