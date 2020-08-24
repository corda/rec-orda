package com.rec.states

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import net.corda.core.identity.Party

data class RECToken(
        override val issuer: Party,
        override val holder: Party,
        val quantity: Long
//        val issuance: Issuance,
//        val uid: Long
) : FungibleToken(quantity of RECTokenType() issuedBy issuer, holder)