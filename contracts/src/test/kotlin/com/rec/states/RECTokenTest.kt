package com.rec.states

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import org.junit.Test
import kotlin.test.assertEquals

class RECTokenTest {
    private val alice = TestIdentity(CordaX500Name("Alice", "London", "GB")).party
    private val bob = TestIdentity(CordaX500Name("Bob", "London", "GB")).party

    @Test
    fun `it accepts a 0 amount`() {
        val token: FungibleToken = RECToken(alice, bob, 0)
        assertEquals(0, token.amount.quantity)
    }

    @Test
    fun `token type used is RECTokenType`() {
        val token: FungibleToken = RECToken(alice, bob, 0)
        assertEquals(RECTokenType(), token.tokenType)
    }

    @Test
    fun `holder is the only participant`() {
        val token: FungibleToken = RECToken(alice, bob, 0)
        assertEquals(bob, token.participants.single())
    }
}