package com.rec.client.dto

import com.rec.states.EnergySource
import net.corda.core.identity.AbstractParty

data class IssueTokensDto (
        val holder: AbstractParty,
        val quantity: Long,
        val source: EnergySource
)