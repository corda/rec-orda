package com.rec.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.workflows.flows.move.AbstractMoveTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokensHandler
import com.r3.corda.lib.tokens.workflows.utilities.sessionsForParties
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

object MoveFlows {
    /**
     * Started by a [FungibleToken.holder] to move multiple states where it is the only holder.
     * It is an [InitiatingFlow] flow and its counterpart, which already exists, is
     * [MoveFungibleTokensHandler], while not being automatically [InitiatedBy] it.
     * This constructor would be called by RPC or by [FlowLogic.subFlow]. In particular one that, given sums,
     * fetches states in the vault.
     */
    @InitiatingFlow
    @StartableByRPC
    class Initiator(
      private val inputTokens: List<StateAndRef<FungibleToken>>,
      private val outputTokens: List<FungibleToken>,
      override val progressTracker: ProgressTracker = tracker()
    ) : FlowLogic<SignedTransaction?>() {

        private val holder: AbstractParty

        init {
            val holders = inputTokens.map { it.state.data.holder }.distinct()
            if (holders.size != 1) throw IllegalArgumentException("There can be only one holder")
            holder = holders.single()
        }


        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction {
            progressTracker.currentStep = PREPARING_TO_PASS_ON
            // We don't want to sign transactions where our signature is not needed.
            if (holder != ourIdentity) throw FlowException("I must be a holder.")

            val allParticipants = outputTokens.map { it.holder }.distinct() + holder

            val participantSessions = this.sessionsForParties(allParticipants)

            return subFlow(object : AbstractMoveTokensFlow() {
                override val participantSessions = participantSessions

                override val observerSessions: List<FlowSession> = emptyList()

                override fun addMove(transactionBuilder: TransactionBuilder) {
                    addMoveTokens(transactionBuilder, inputTokens, outputTokens)
                }
            })
        }

        companion object {
            private val PREPARING_TO_PASS_ON = ProgressTracker.Step("Preparing to pass on to Tokens move flow.")
            private val PASSING_TO_SUB_MOVE: ProgressTracker.Step = ProgressTracker.Step("Passing on to Tokens move flow.")

            fun tracker(): ProgressTracker {
                return ProgressTracker(PREPARING_TO_PASS_ON, PASSING_TO_SUB_MOVE)
            }
        }
    }
}