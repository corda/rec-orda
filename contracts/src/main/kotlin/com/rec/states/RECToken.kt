package com.rec.states

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.rec.contracts.RECTokenContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(RECTokenContract::class)
data class RECToken(
        override val issuer: Party,
        override val holder: AbstractParty,
        val quantity: Long
//        val issuance: Issuance,
//        val uid: Long
) : FungibleToken(quantity of RECTokenType() issuedBy issuer, holder)