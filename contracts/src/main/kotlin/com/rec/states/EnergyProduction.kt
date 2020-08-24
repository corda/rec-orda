package com.rec.states

import java.util.*

data class EnergyProduction(
        val source: EnergySource,
        val megawattHour: Long,
        val startDate: Date,
        val endDate: Date,
        val relatedToElectricity: Boolean
)
