package com.rec.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction

object MoveFlows {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(private val partiesAndAmounts: List<PartyAndAmount<TokenType>>) : FlowLogic<SignedTransaction?>() {

        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction = subFlow(MoveFungibleTokens(partiesAndAmounts))
    }
}