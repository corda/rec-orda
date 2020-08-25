package com.rec.flows

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensFlowHandler
import com.rec.flows.FlowTestHelpers.NodeHolding
import com.rec.flows.FlowTestHelpers.assertHasStatesInVault
import com.rec.flows.FlowTestHelpers.createFrom
import com.rec.flows.FlowTestHelpers.issueTokens
import com.rec.flows.FlowTestHelpers.prepareMockNetworkParameters
import com.rec.flows.MoveFlows.Initiator
import com.rec.states.EnergySource
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.TransactionResolutionException
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ExecutionException
import java.util.function.Consumer

class MoveFlowsTests {
    private val network: MockNetwork = MockNetwork(prepareMockNetworkParameters)
    private val alice: StartedMockNode = network.createNode()
    private val bob: StartedMockNode = network.createNode()
    private val carly: StartedMockNode = network.createNode()
    private val dan: StartedMockNode = network.createNode()
    private val source: EnergySource = EnergySource.values()[0]

    init {
        listOf(alice, bob, carly, dan).forEach(Consumer { it.registerInitiatedFlow(Initiator::class.java, MoveTokensFlowHandler::class.java) })
    }

    @Before
    fun setup() {
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test(expected = IllegalArgumentException::class)
    @Throws(Throwable::class)
    fun flowFailsWhenInitiatorIsMissingTransactionsTheyWereNotPartyTo() {

        val issuedTokens = issueTokens(
          alice, network, listOf(
          NodeHolding(bob, 10L),
          NodeHolding(dan, 20L))
        )

        val flow = Initiator(issuedTokens, listOf(createFrom(alice, dan, 30L, source)))

        val future = bob.startFlow(flow)
        network.runNetwork()

        try {
            future.get()
        } catch (ex: ExecutionException) {
            throw ex.cause!!
        }
    }

    @Test
    @Throws(Throwable::class)
    fun signedTransactionReturnedByTheFlowIsSignedByTheHolder() {
        val issuedTokens = issueTokens(alice, network, listOf(NodeHolding(bob, 10L)))

        val flow = Initiator(issuedTokens, listOf(createFrom(alice, carly, 10L, source)))

        val future = bob.startFlow(flow)
        network.runNetwork()

        val tx = future.get()!!

        tx.verifySignaturesExcept(listOf(
          alice.info.legalIdentities[0].owningKey,
          carly.info.legalIdentities[0].owningKey))
    }

    @Test
    @Throws(Throwable::class)
    fun flowRecordsATransactionInHolderTransactionStoragesOnly() {
        val issuedTokens = issueTokens(alice, network, listOf(NodeHolding(bob, 10L)))

        val flow = Initiator(issuedTokens, listOf(createFrom(alice, carly, 10L, source)))

        val future = bob.startFlow(flow)

        network.runNetwork()

        val tx = future.get()!!

        // We check the recorded transaction in both transaction storages.
        for (node in listOf(bob, carly)) {
            assertEquals(tx, node.services.validatedTransactions.getTransaction(tx.id))
        }
        for (node in listOf(alice, dan)) {
            assertNull(node.services.validatedTransactions.getTransaction(tx.id))
        }
    }

    @Test
    @Throws(Throwable::class)
    fun recordedTransactionHasASingleInputAndASingleOutput() {
        val issuedTokens = issueTokens(alice, network, listOf(NodeHolding(bob, 10L)))

        val expectedInput = issuedTokens[0].state.data

        val expectedOutput: FungibleToken = createFrom(alice, carly, 10L, source)

        val flow = Initiator(issuedTokens, listOf(expectedOutput))

        val future = bob.startFlow(flow)

        network.runNetwork()

        val tx = future.get()!!

        // We check the recorded transaction in both vaults.
        for (node in listOf(bob, carly)) {
            val recordedTx = node.services.validatedTransactions.getTransaction(tx.id)
            val txInputs = recordedTx!!.tx.inputs
            assertEquals(1, txInputs.size.toLong())
            assertEquals(expectedInput, node.services.toStateAndRef<ContractState>(txInputs[0]).state.data)
            val txOutputs = recordedTx.tx.outputs
            assertEquals(1, txOutputs.size.toLong())
            assertEquals(expectedOutput, txOutputs[0].data)
        }
        for (node in listOf(alice, dan)) {
            assertNull(node.services.validatedTransactions.getTransaction(tx.id))
        }
    }

    @Test
    @Throws(Throwable::class)
    fun recordedTransactionHasTwoInputsAnd1OutputSameIssuer() {
        val issuedTokens = issueTokens(alice, network, listOf(
          NodeHolding(bob, 10L),
          NodeHolding(bob, 20L))
        )

        val expectedInputs = issuedTokens.map { it.state.data }
        val expectedOutput = createFrom(alice, dan, 30L, source)
        val flow = Initiator(issuedTokens, listOf(expectedOutput))
        val future = bob.startFlow(flow)
        network.runNetwork()
        val tx = future.get()!!

        // We check the recorded transaction in 3 vaults.
        for (node in listOf(bob, dan)) {
            val recordedTx = node.services.validatedTransactions.getTransaction(tx.id)
            val txInputs = recordedTx!!.tx.inputs
            assertEquals(2, txInputs.size.toLong())

            assertEquals(expectedInputs, txInputs
              .map {
                  try {
                      return@map node.services.toStateAndRef<ContractState>(it).state.data
                  } catch (ex: TransactionResolutionException) {
                      throw RuntimeException(ex)
                  }
              })

            val txOutputs = recordedTx.tx.outputs
            assertEquals(1, txOutputs.size.toLong())
            assertEquals(expectedOutput, txOutputs[0].data)
        }
        assertNull(alice.services.validatedTransactions.getTransaction(tx.id))
    }

    @Test
    @Throws(Throwable::class)
    fun thereIsOneRecordedStateAfterMoveOnlyInRecipientIssuerKeepsOldState() {
        val issuedTokens = issueTokens(alice, network, listOf(NodeHolding(bob, 10L)))

        val expectedOutput: FungibleToken = createFrom(alice, carly, 10L, source)

        val flow = Initiator(issuedTokens, listOf(expectedOutput))

        val future = bob.startFlow(flow)

        network.runNetwork()

        future.get()

        // We check the states in vaults.
        assertHasStatesInVault(alice, issuedTokens.map { it.state.data })
        assertHasStatesInVault(bob, emptyList())
        assertHasStatesInVault(carly, listOf(expectedOutput))
    }

    @Test
    @Throws(Throwable::class)
    fun thereAreTwoRecordedStatesAfterMoveOnlyInRecipientDifferentIssuerIssuersKeepOldStates() {
        val issuedTokens1 = issueTokens(alice, network, listOf(NodeHolding(bob, 10L)))
        val issuedTokens = issuedTokens1 + issueTokens(carly, network, listOf(NodeHolding(bob, 20L)))

        val expectedOutput1: FungibleToken = createFrom(alice, dan, 10L, source)
        val expectedOutput2: FungibleToken = createFrom(carly, dan, 20L, source)

        val flow = Initiator(issuedTokens, listOf(expectedOutput1, expectedOutput2))

        val future = bob.startFlow(flow)

        network.runNetwork()
        future.get()

        // We check the states in vaults.
        assertHasStatesInVault(alice, listOf(issuedTokens[0].state.data))
        assertHasStatesInVault(bob, emptyList())
        assertHasStatesInVault(carly, listOf(issuedTokens[1].state.data))
        assertHasStatesInVault(dan, listOf(expectedOutput1, expectedOutput2))
    }

}