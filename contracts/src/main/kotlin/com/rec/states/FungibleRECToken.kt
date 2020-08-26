package com.rec.states

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import net.corda.core.contracts.Amount
import net.corda.core.identity.AbstractParty

class FungibleRECToken(
        override val amount: Amount<IssuedTokenType>,
        override val holder: AbstractParty
) : FungibleToken(amount, holder) {

    init {
        assert(tokenType is RECToken)
    }

    override infix fun withNewHolder(newHolder: AbstractParty): FungibleRECToken = FungibleRECToken(amount, newHolder)

}

// ------------------------------------------------------
// Creates a tokens from (amounts of) issued token types.
// ------------------------------------------------------

/**
 * Creates a [FungibleToken] from an an amount of [IssuedTokenType].
 * E.g. Amount<IssuedTokenType<TokenType>> -> FungibleToken<TokenType>.
 */

infix fun Amount<IssuedTokenType>.heldBy(owner: AbstractParty): FungibleRECToken  = FungibleRECToken(this, owner)


//            val x: Amount<TokenType> = 10L of RECToken(source)
//            val y: Amount<IssuedTokenType> = 10L of RECToken(source) issuedBy ourIdentity
//            val z: FungibleToken = 10L of RECToken(source) issuedBy ourIdentity heldBy ourIdentity