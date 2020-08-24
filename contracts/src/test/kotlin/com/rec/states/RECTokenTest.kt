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

    // These tests are not necessary as this is the same as testing the `data class` construct.
    @Test
    fun `equals and hashcode identifies identical instances`() {
        val token1 = RECToken(alice, bob, 2)
        val token2 = RECToken(alice, bob, 2)

        assertEquals(token1, token2)
        assertEquals(token1.hashCode(), token2.hashCode())
    }

    @Test
    fun `equals and hashcode differentiate by issuer`() {
        val token1 = RECToken(alice, bob, 2)
        val token2 = RECToken(carly, bob, 2)
        Assert.assertNotEquals(token1, token2)
        Assert.assertNotEquals(token1.hashCode().toLong(), token2.hashCode().toLong())
    }

    @Test
    fun `equals and hashcode differentiate by owner`() {
        val token1 = RECToken(alice, bob, 2)
        val token2 = RECToken(alice, carly, 2)
        Assert.assertNotEquals(token1, token2)
        Assert.assertNotEquals(token1.hashCode().toLong(), token2.hashCode().toLong())
    }

    @Test
    fun `equals and hashcode differentiate by amount`() {
        val token1 = RECToken(alice, bob, 2)
        val token2 = RECToken(alice, bob, 3)
        Assert.assertNotEquals(token1, token2)
        Assert.assertNotEquals(token1.hashCode().toLong(), token2.hashCode().toLong())
    }
}