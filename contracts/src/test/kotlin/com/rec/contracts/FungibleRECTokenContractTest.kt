package com.rec.contracts

import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.utilities.getAttachmentIdForGenericParam
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.rec.states.EnergySource
import com.rec.states.RECToken
import com.rec.states.heldBy
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.transaction
import org.junit.Test

class FungibleRECTokenContractTest {

    private val ledgerServices = MockServices(listOf("com.r3.corda.lib.tokens.contracts"))
    private val alice = TestIdentity(CordaX500Name("Alice", "London", "GB")).party
    private val source = EnergySource.values()[0]
    private val issuedToken = RECToken(source) issuedBy alice

    @Test
    fun `issue tests`() {
        ledgerServices.transaction {
            // must include an attachment
            tweak {
                output(FungibleRECTokenContract.contractId, 10 of issuedToken heldBy alice)
                command(alice.owningKey, IssueTokenCommand(issuedToken, listOf(0)))
                `fails with`("Cannot find contract attachments for com.rec.contracts.FungibleRECTokenContract")
            }

            tweak {
                output(FungibleRECTokenContract.contractId, 10 of issuedToken heldBy alice)
                command(alice.owningKey, IssueTokenCommand(issuedToken, listOf(0)))
                attachment("com.rec.contracts.FungibleRECTokenContract", RECToken(source).getAttachmentIdForGenericParam()!!)
                verifies()
            }
        }
    }


}