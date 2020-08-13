package com.rec.states

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.getAttachmentIdForGenericParam
import net.corda.core.crypto.SecureHash

class RECTokenType : TokenType(IDENTIFIER, FRACTION_DIGITS) {
    companion object {
        val contractAttachment: SecureHash = RECTokenType().getAttachmentIdForGenericParam()!!
        const val IDENTIFIER = "REC"
        const val FRACTION_DIGITS = 0
    }
}
