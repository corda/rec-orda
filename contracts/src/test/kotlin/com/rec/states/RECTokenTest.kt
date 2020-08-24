package com.rec.states

import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import org.junit.Assert
import org.junit.Test
import kotlin.test.assertEquals

class RECTokenTest {
    private val alice = TestIdentity(CordaX500Name("Alice", "London", "GB")).party
    private val bob = TestIdentity(CordaX500Name("Bob", "London", "GB")).party
    private val carly = TestIdentity(CordaX500Name("Carly", "London", "GB")).party

    @Test
    fun `it accepts a 0 amount`() {
        val token = RECToken(alice, bob, 0)
        assertEquals(0, token.quantity)
    }
}