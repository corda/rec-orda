package com.rec.contracts

import com.r3.corda.lib.tokens.contracts.FungibleTokenContract
import com.r3.corda.lib.tokens.contracts.commands.TokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.contracts.Attachment
import net.corda.core.contracts.CommandWithParties
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef

class FungibleRECTokenContract: FungibleTokenContract() {
    companion object {
        // Used to identify our contract when building a transaction.
        val contractId: String = this::class.java.enclosingClass.canonicalName
    }

    override fun verifyIssue(issueCommand: CommandWithParties<TokenCommand>, inputs: List<IndexedState<FungibleToken>>, outputs: List<IndexedState<FungibleToken>>, attachments: List<Attachment>, references: List<StateAndRef<ContractState>>) {
        super.verifyIssue(issueCommand, inputs, outputs, attachments, references)
        // require that right to issue is Approved
    }
}