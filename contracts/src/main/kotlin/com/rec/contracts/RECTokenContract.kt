package com.rec.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
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
//        val command = tx.commands.requireSingleCommand<Commands>()
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Issue : Commands
        class Move : Commands
        class Redeem : Commands
    }
}
