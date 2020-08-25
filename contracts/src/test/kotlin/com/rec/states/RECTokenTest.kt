package com.rec.states

import com.r3.corda.lib.tokens.contracts.types.TokenType
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals


class RECTokenTest {

    @Test
    fun similarSourcesAreEqual() {
        assertEquals(RECToken(EnergySource.WIND), RECToken(EnergySource.WIND))
    }

    @Test
    fun differentSourcesAreNotEqual() {
        assertNotEquals(RECToken(EnergySource.WIND), RECToken(EnergySource.SOLAR))
    }

    @Test
    fun hashCodeIsConstant() {
        assertEquals(RECToken.hashCode(), RECToken.hashCode())
    }

    @Test
    fun equalsIsOkWithSame() {
        assertEquals(RECToken, RECToken)
    }

    @Test
    fun equalsIsDifferentWithNull() {
        assertNotEquals(RECToken, null)
    }

    @Test
    fun equalsIsDifferentWithOtherTokenType() {
        assertNotEquals(RECToken, TokenType(RECToken.IDENTIFIER, RECToken.FRACTION_DIGITS))
    }
}