package com.rec.states

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class EnergySource {
    SOLAR,
    WIND,
    HYDRO,
    TIDAL,
    GEOTHERMAL,
    BIOMASS,
}