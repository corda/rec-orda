package com.rec.states

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.getAttachmentIdForGenericParam
import net.corda.core.crypto.SecureHash

data class RECToken(val source: EnergySource) : TokenType(IDENTIFIER, FRACTION_DIGITS) {
    companion object {
        val contractAttachment: SecureHash = RECToken(EnergySource.WIND).getAttachmentIdForGenericParam()!!
        const val IDENTIFIER = "REC"
        const val FRACTION_DIGITS = 0
    }
}
