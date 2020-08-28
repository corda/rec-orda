package com.rec.states

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import org.junit.Test

class FungibleRECTokenTest {

    private val alice = TestIdentity(CordaX500Name("Alice", "London", "GB")).party
    private val bob = TestIdentity(CordaX500Name("Bob", "New York", "US")).party
    private val source = EnergySource.values()[0]

    @Test
    fun `can create fungible RECToken with RECToken TokenType`() {
        1 of RECToken(source) issuedBy alice heldBy bob
    }

    @Test(expected = AssertionError::class)
    fun `cannot create fungible RECToken with other TokenType`() {
        1 of TokenType(RECToken.IDENTIFIER, RECToken.FRACTION_DIGITS) issuedBy alice heldBy bob
    }

    @Test
    fun `can change fungible RECToken holder`() {
        val fungibleToken = 1 of RECToken(source) issuedBy alice heldBy bob
        fungibleToken withNewHolder alice
    }

    @Test
    fun `can retrieve RECToken directly from fungible RECToken`() {
        val fungibleToken = 1 of RECToken(source) issuedBy alice heldBy bob
        fungibleToken.recToken
    }

}