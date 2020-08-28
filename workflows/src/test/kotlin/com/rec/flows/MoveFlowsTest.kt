package com.rec.flows

import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensFlowHandler
import com.rec.flows.FlowTestHelpers.NodeHolding
import com.rec.flows.FlowTestHelpers.assertHasSourceInVault
import com.rec.flows.FlowTestHelpers.assertHasStatesInVault
import com.rec.flows.FlowTestHelpers.createFrom
import com.rec.flows.FlowTestHelpers.issueTokens
import com.rec.flows.FlowTestHelpers.prepareMockNetworkParameters
import com.rec.flows.MoveFlows.Initiator
import com.rec.states.EnergySource
import com.rec.states.FungibleRECToken
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

class MoveFlowsTest {
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
    fun `flow fails when initiator is missing transactions they were not party to`() {

        val issuedTokens = issueTokens(
                alice, network, listOf(
                NodeHolding(bob, 10L),
                NodeHolding(dan, 20L)), source
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
    fun `signed transaction returned by the flow is signed by the holder`() {
        val issuedTokens = issueTokens(alice, network, listOf(NodeHolding(bob, 10L)), source)

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
    fun `flow records a transaction in holder transaction storage only`() {
        val issuedTokens = issueTokens(alice, network, listOf(NodeHolding(bob, 10L)), source)

        val flow = Initiator(issuedTokens, listOf(createFrom(alice, carly, 10L, source)))

        val future = bob.startFlow(flow)

        network.runNetwork()

        val tx = future.get()!!

        // We check the recorded transaction in both transaction storage.
        for (node in listOf(bob, carly)) {
            assertEquals(tx, node.services.validatedTransactions.getTransaction(tx.id))
        }
        for (node in listOf(alice, dan)) {
            assertNull(node.services.validatedTransactions.getTransaction(tx.id))
        }
    }

    @Test
    @Throws(Throwable::class)
    fun `recorded transaction has a single input and a single output`() {
        val issuedTokens = issueTokens(alice, network, listOf(NodeHolding(bob, 10L)), source)

        val expectedInput = issuedTokens[0].state.data

        val expectedOutput: FungibleRECToken = createFrom(alice, carly, 10L, source)

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
    fun `recorded transaction has two inputs and one output same issuer`() {
        val issuedTokens = issueTokens(alice, network, listOf(
          NodeHolding(bob, 10L),
          NodeHolding(bob, 20L)), source
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
    fun `there is one recorded state after move only in recipient issuer keeps old state`() {
        val issuedTokens = issueTokens(alice, network, listOf(NodeHolding(bob, 10L)), source)

        val expectedOutput: FungibleRECToken = createFrom(alice, carly, 10L, source)

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
    fun `states all have the correct source`() {
        val issuedTokens = issueTokens(alice, network, listOf(NodeHolding(bob, 10L)), source)

        val expectedOutput: FungibleRECToken = createFrom(alice, carly, 10L, source)

        val flow = Initiator(issuedTokens, listOf(expectedOutput))

        val future = bob.startFlow(flow)

        network.runNetwork()

        future.get()

        // We check the states in vaults have the correct source.
        assertHasSourceInVault(alice, issuedTokens.map { it.state.data.recToken.source })
        assertHasSourceInVault(bob, emptyList())
        assertHasSourceInVault(carly, listOf(expectedOutput.recToken.source))
    }

    @Test
    @Throws(Throwable::class)
    fun `there are two recorded states after move only in recipient different issuer and issuers keep old states`() {
        val issuedTokens1 = issueTokens(alice, network, listOf(NodeHolding(bob, 10L)), source)
        val issuedTokens = issuedTokens1 + issueTokens(carly, network, listOf(NodeHolding(bob, 20L)), source)

        val expectedOutput1: FungibleRECToken = createFrom(alice, dan, 10L, source)
        val expectedOutput2: FungibleRECToken = createFrom(carly, dan, 20L, source)

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

    @Test
    @Throws(Throwable::class)
    fun `both states all have the correct source`() {
        val issuedTokens1 = issueTokens(alice, network, listOf(NodeHolding(bob, 10L)), source)
        val issuedTokens = issuedTokens1 + issueTokens(carly, network, listOf(NodeHolding(bob, 20L)), source)

        val expectedOutput1: FungibleRECToken = createFrom(alice, dan, 10L, source)
        val expectedOutput2: FungibleRECToken = createFrom(carly, dan, 20L, source)

        val flow = Initiator(issuedTokens, listOf(expectedOutput1, expectedOutput2))

        val future = bob.startFlow(flow)

        network.runNetwork()
        future.get()

        // We check the states in vaults have the correct source.
        assertHasSourceInVault(alice, listOf(issuedTokens[0].state.data.recToken.source))
        assertHasSourceInVault(bob, emptyList())
        assertHasSourceInVault(carly, listOf(issuedTokens[1].state.data.recToken.source))
        assertHasSourceInVault(dan, listOf(expectedOutput1.recToken.source, expectedOutput2.recToken.source))
    }
}