package com.rec.contracts

import com.rec.states.FungibleRECToken
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class FungibleRECTokenContract: Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.rec.contracts.FungibleRECTokenContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val inputs = tx.inputsOfType<FungibleRECToken>()
        val outputs = tx.outputsOfType<FungibleRECToken>()
        val hasAllPositiveQuantities = inputs.all { 0 < it.amount.quantity } && outputs.all { 0 < it.amount.quantity }

        requireThat { "All quantities must be above 0." using false }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Issue : Commands
        class Move : Commands
        class Redeem : Commands
    }
}