package com.rec.states

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.getAttachmentIdForGenericParam
import net.corda.core.crypto.SecureHash

class RECTokenState : TokenType(IDENTIFIER, FRACTION_DIGITS) {
    companion object {
        val contractAttachment: SecureHash = RECTokenState().getAttachmentIdForGenericParam()!!
        const val IDENTIFIER = "AIR"
        const val FRACTION_DIGITS = 0
    }
}
