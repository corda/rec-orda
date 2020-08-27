package com.rec.contracts

import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class RECTokenContract: Contract {
    companion object {
        const val ID = "com.rec.contracts.RECTokenContract"
    }

    override fun verify(tx: LedgerTransaction) {
    }
}
