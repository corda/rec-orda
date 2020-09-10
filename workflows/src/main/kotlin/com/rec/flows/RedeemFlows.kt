package com.rec.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.flows.redeem.RedeemTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.redeem.RedeemTokensFlowHandler
import com.r3.corda.lib.tokens.workflows.flows.rpc.RedeemFungibleTokens
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

object RedeemFlows {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(private val amount: Amount<TokenType>, private val issuer: Party) : FlowLogic<SignedTransaction>() {
        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction = subFlow(RedeemFungibleTokens(amount, issuer))
    }
}