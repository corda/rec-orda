package com.rec.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.rec.states.EnergySource
import com.rec.states.RECToken
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker


object IssueFlows {
    /**
     * Started by the [FungibleToken.issuer] to issue multiple states where it is the only issuer.
     * It is not an [InitiatingFlow] because it does not need to, it is [IssueTokens] that is initiating.
     * It may contain a given [Party] more than once, so that we can issue multiple states to a given holder.
     */
    @StartableByRPC
    class Initiator(
            private val heldQuantities: List<Pair<AbstractParty, Long>>,
            private val source: EnergySource,
            override val progressTracker: ProgressTracker = tracker()
    ) : FlowLogic<SignedTransaction>() {

        /**
         * The only constructor that can be called from the CLI.
         * Started by the issuer to issue a single state.
         */
        constructor(holder: AbstractParty, quantity: Long, source: EnergySource) : this(listOf(Pair(holder, quantity)), source)

        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction {
            progressTracker.currentStep = PREPARING_TO_PASS_ON
            // It is a design decision to have this flow initiated by the issuer.

            val outputTokens: List<FungibleToken> = heldQuantities.map { (holder, quantity) ->
                quantity of RECToken(source) issuedBy ourIdentity heldBy holder
            }

            progressTracker.currentStep = PASSING_TO_SUB_ISSUE
            val notarised = subFlow(IssueTokens(outputTokens))

            // We want our issuer to have a trace of the amounts that have been issued, whether it is a holder or not,
            // in order to know the total supply. Since the issuer is not in the participants, it needs to be done
            // manually. We do it after the sub flow as this is the better way to do, after notarisation, even if
            // here there is no notarisation.
            serviceHub.recordTransactions(StatesToRecord.ALL_VISIBLE, listOf(notarised))
            return notarised
        }

        companion object {
            private val PREPARING_TO_PASS_ON = ProgressTracker.Step("Preparing to pass on to Tokens issue flow.")
            private val PASSING_TO_SUB_ISSUE = ProgressTracker.Step("Passing on to Tokens issue flow.")
            fun tracker(): ProgressTracker {
                return ProgressTracker(PREPARING_TO_PASS_ON, PASSING_TO_SUB_ISSUE)
            }
        }
    }
}