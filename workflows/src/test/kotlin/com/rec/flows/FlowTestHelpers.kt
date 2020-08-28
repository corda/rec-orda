package com.rec.flows

import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.rec.states.EnergySource
import com.rec.states.FungibleRECToken
import com.rec.states.RECToken
import com.rec.states.heldBy
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.testing.node.*
import java.io.File
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

object FlowTestHelpers {
    private fun propertiesFromConf(pathname: String): Map<String, String> {
        val tokenProperties = Properties()
        File(pathname).inputStream().let { tokenProperties.load(it) }
        return tokenProperties.entries
                .associateBy(
                        { it.key as String },
                        {
                            (it.value as String)
                                    .removeSurrounding("\"")
                                    .removeSurrounding("\'")
                        })
    }

    private val tokensConfig = propertiesFromConf("res/tokens-workflows.conf")

    val prepareMockNetworkParameters: MockNetworkParameters = MockNetworkParameters()
            .withNotarySpecs(listOf(
                    MockNetworkNotarySpec(CordaX500Name.parse("O=Unwanted Notary,L=London,C=GB")),
                    MockNetworkNotarySpec(CordaX500Name.parse(
                            tokensConfig["notary"]
                                    ?: error("no notary given in tokens-workflows.conf")))
            ))
            .withCordappsForAllNodes(listOf(
                    TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                    TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows")
                            .withConfig(tokensConfig),
                    TestCordapp.findCordapp("com.r3.corda.lib.tokens.money"),
                    TestCordapp.findCordapp("com.r3.corda.lib.tokens.selection"),
                    TestCordapp.findCordapp("com.rec.states"),
                    TestCordapp.findCordapp("com.rec.flows"))
            )

    fun createFrom(
            issuer: StartedMockNode,
            holder: StartedMockNode,
            quantity: Long,
            source: EnergySource
    ): FungibleRECToken = quantity of
            RECToken(source) issuedBy
            issuer.info.legalIdentities.first() heldBy
            holder.info.legalIdentities.first()

    fun FungibleRECToken.toPair() = Pair(this.holder, this.amount.quantity)


    fun assertHasStatesInVault(node: StartedMockNode, tokenStates: List<FungibleRECToken>) {
        val vaultTokens = node.transaction {
            node.services.vaultService.queryBy(FungibleRECToken::class.java).states
        }
        assertEquals(tokenStates.size, vaultTokens.size)
        tokenStates.indices.forEach {
            assertEquals(vaultTokens[it].state.data, tokenStates[it])
        }
    }

    fun assertHasSourceInVault(node: StartedMockNode, source: List<EnergySource>) {
        val vaultTokens = node.transaction {
            node.services.vaultService.queryBy(FungibleRECToken::class.java).states
        }
        vaultTokens.indices.forEach {
            assertEquals(vaultTokens[it].state.data.recToken.source, source[it])
        }
    }

    data class NodeHolding(val holder: StartedMockNode, val quantity: Long) {
        fun toPair(): Pair<AbstractParty, Long> {
            return Pair(holder.info.legalIdentities[0], quantity)
        }
    }

    fun issueTokens(node: StartedMockNode, network: MockNetwork, nodeHoldings: Collection<NodeHolding>, source: EnergySource): List<StateAndRef<FungibleRECToken>> {
        val flow = IssueFlows.Initiator(nodeHoldings.map { it.toPair() }, source)
        val future = node.startFlow(flow)
        network.runNetwork()
        val tx = future.get()
        return tx.toLedgerTransaction(node.services).outRefsOfType(FungibleRECToken::class.java)
    }

}