package com.rec.states

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class EnergySource {
    /* Must always have at least one value */
    SOLAR,
    WIND,
    HYDRO,
    TIDAL,
    GEOTHERMAL,
    BIOMASS,
}