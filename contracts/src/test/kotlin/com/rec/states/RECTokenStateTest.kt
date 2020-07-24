package com.rec.states

import com.r3.corda.lib.tokens.contracts.types.TokenType
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals


class RECTokenStateTest {
    @Test
    fun hashCodeIsConstant() {
        assertEquals(RECTokenState.hashCode(), RECTokenState.hashCode())
    }

    @Test
    fun equalsIsOkWithSame() {
        assertEquals(RECTokenState, RECTokenState)
    }

    @Test
    fun equalsIsDifferentWithNull() {
        assertNotEquals(RECTokenState, null)
    }

    @Test
    fun equalsIsDifferentWithOtherTokenType() {
        assertNotEquals(RECTokenState, TokenType(RECTokenState.IDENTIFIER, RECTokenState.FRACTION_DIGITS))
    }
}