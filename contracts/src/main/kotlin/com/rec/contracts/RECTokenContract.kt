package com.rec.contracts

import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class RECTokenContract: Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        val contractId: String = this::class.java.enclosingClass.canonicalName
    }

    override fun verify(tx: LedgerTransaction) {
        return
    }
}