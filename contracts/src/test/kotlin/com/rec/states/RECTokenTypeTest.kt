package com.rec.states

import com.r3.corda.lib.tokens.contracts.types.TokenType
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals


class RECTokenTypeTest {
    @Test
    fun hashCodeIsConstant() {
        assertEquals(RECTokenType.hashCode(), RECTokenType.hashCode())
    }

    @Test
    fun equalsIsOkWithSame() {
        assertEquals(RECTokenType, RECTokenType)
    }

    @Test
    fun equalsIsDifferentWithNull() {
        assertNotEquals(RECTokenType, null)
    }

    @Test
    fun equalsIsDifferentWithOtherTokenType() {
        assertNotEquals(RECTokenType, TokenType(RECTokenType.IDENTIFIER, RECTokenType.FRACTION_DIGITS))
    }
}