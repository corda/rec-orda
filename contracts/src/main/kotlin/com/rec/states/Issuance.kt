package com.rec.states

import java.util.*

data class Issuance(
        val production: EnergyProduction,
        val date: Date,
        val country: Locale
)
