package com.rec.states

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.rec.contracts.FungibleRECTokenContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.identity.AbstractParty

@BelongsToContract(FungibleRECTokenContract::class)
class FungibleRECToken(
        override val amount: Amount<IssuedTokenType>,
        override val holder: AbstractParty
) : FungibleToken(amount, holder) {

    init {
        assert(tokenType is RECToken)
    }

    val recToken: RECToken = tokenType as RECToken

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