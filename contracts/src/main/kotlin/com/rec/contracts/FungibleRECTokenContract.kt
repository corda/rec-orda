package com.rec.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class FungibleRECTokenContract: Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.rec.contracts.FungibleRECTokenContract"
    }

    override fun verify(tx: LedgerTransaction) {
        TODO("Not yet implemented")
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Issue : Commands
        class Move : Commands
        class Redeem : Commands
    }
}