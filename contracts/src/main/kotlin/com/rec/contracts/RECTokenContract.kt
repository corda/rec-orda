package com.rec.contracts

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.rec.states.RECToken
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class RECTokenContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.rec.contracts.RECTokenContract"
    }
    
    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        val inputs = tx.inputsOfType<RECToken>()
        val outputs = tx.outputsOfType<RECToken>()
        val hasAllPositiveQuantities = inputs.all { 0 < it.quantity } && outputs.all { 0 < it.quantity }

        requireThat { "All quantities must be above 0." using hasAllPositiveQuantities }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Issue : Commands
        class Move : Commands
        class Redeem : Commands
    }
}
