package com.rec.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.workflows.flows.redeem.RedeemTokensFlow
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

object RedeemFlows {


    /**
     * Started by a $[FungibleToken.holder] to redeem multiple states of 1 issuer where it is one of the holders.
     * It is not $[InitiatingFlow].
     * This constructor would be called by RPC or by $[FlowLogic.subFlow].
     */

    // By requiring an exact list of states, this flow assures absolute precision at the expense of
    // user-friendliness.
    @InitiatingFlow
    @StartableByRPC
    class Initiator(
      private val inputTokens: List<StateAndRef<FungibleToken>>,
      override val progressTracker: ProgressTracker = tracker()) : FlowLogic<SignedTransaction?>() {

        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction {
            progressTracker.currentStep = PREPARING_TO_PASS_ON
            val allIssuers = inputTokens
              .map { it.state.data.issuer } // Remove duplicates as it would be an issue when initiating flows, at least.
            // We don't want to sign transactions where our signature is not needed.

            if (allIssuers.size != 1) throw FlowException("It can only redeem one issuer at a time.")

            val issuerSession = initiateFlow(allIssuers.iterator().next())

            progressTracker.currentStep = PASSING_TO_SUB_REDEEM

            return subFlow(RedeemTokensFlow(inputTokens, null, issuerSession))
        }

        companion object {
            private val PREPARING_TO_PASS_ON = ProgressTracker.Step("Preparing to pass on to Tokens redeem flow.")
            private val PASSING_TO_SUB_REDEEM = ProgressTracker.Step("Passing on to Tokens redeem flow.")
            fun tracker(): ProgressTracker {
                return ProgressTracker(PREPARING_TO_PASS_ON, PASSING_TO_SUB_REDEEM)
            }
        }
    }

}