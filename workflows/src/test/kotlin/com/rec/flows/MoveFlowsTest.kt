package com.rec.flows

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensFlowHandler
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import com.rec.flows.FlowTestHelpers.NodeHolding
import com.rec.flows.FlowTestHelpers.assertHasSourceInVault
import com.rec.flows.FlowTestHelpers.issueTokens
import com.rec.flows.FlowTestHelpers.partyAndAmountOf
import com.rec.flows.FlowTestHelpers.prepareMockNetworkParameters
import com.rec.flows.MoveFlows.Initiator
import com.rec.states.EnergySource
import com.rec.states.RECToken.Companion.recToken
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.function.Consumer
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    @Test
    @Throws(Throwable::class)
    fun `can move tokens from bob to dan`() {
        issueTokens(alice, network, listOf(NodeHolding(bob, 10L), NodeHolding(dan, 20L)), source)
        val flow = Initiator(partyAndAmountOf(source, NodeHolding(dan, 10L)))
        val future = bob.startFlow(flow)
        network.runNetwork()
        future.get()
    }

    @Test
    @Throws(Throwable::class)
    fun `signed transaction returned by the flow is signed by the holder`() {
        issueTokens(alice, network, listOf(NodeHolding(bob, 10L)), source)
        val flow = Initiator(partyAndAmountOf(source, NodeHolding(carly, 10L)))
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
        issueTokens(alice, network, listOf(NodeHolding(bob, 10L)), source)
        val flow = Initiator(partyAndAmountOf(source, NodeHolding(carly, 10L)))
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
    fun `both states all have the correct source`() {
        val issuedTokens1 = issueTokens(alice, network, listOf(NodeHolding(bob, 10L)), source)
        val issuedTokens = issuedTokens1 + issueTokens(carly, network, listOf(NodeHolding(bob, 20L)), source)

        val expectedOutput1: List<PartyAndAmount<TokenType>> = partyAndAmountOf(source, NodeHolding(dan, 10L))
        val expectedOutput2: List<PartyAndAmount<TokenType>>  = partyAndAmountOf(source, NodeHolding(dan, 20L))

        val flow = Initiator(expectedOutput1 + expectedOutput2)

        val future = bob.startFlow(flow)

        network.runNetwork()
        future.get()

        // We check the states in vaults have the correct source.
        assertHasSourceInVault(alice, listOf(issuedTokens[0].state.data.recToken.source))
        assertHasSourceInVault(bob, emptyList())
        assertHasSourceInVault(carly, listOf(issuedTokens[1].state.data.recToken.source))
        assertHasSourceInVault(dan, listOf(source, source))
    }
}