package com.rec.states

import com.r3.corda.lib.tokens.contracts.types.TokenType
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals


class RECTokenTest {

    @Test
    fun `similar sources are equal RECToken`() {
        assertEquals(RECToken(EnergySource.WIND), RECToken(EnergySource.WIND))
    }

    @Test
    fun `different sources are not equal RECToken`() {
        assertNotEquals(RECToken(EnergySource.WIND), RECToken(EnergySource.SOLAR))
    }

    @Test
    fun `hash code is constant`() {
        assertEquals(RECToken.hashCode(), RECToken.hashCode())
    }

    @Test
    fun `equals is ok with same`() {
        assertEquals(RECToken, RECToken)
    }

    @Test
    fun `equals is different with null`() {
        assertNotEquals(RECToken, null)
    }

    @Test
    fun `equals is different with other TokenType`() {
        assertNotEquals(RECToken, TokenType(RECToken.IDENTIFIER, RECToken.FRACTION_DIGITS))
    }
}