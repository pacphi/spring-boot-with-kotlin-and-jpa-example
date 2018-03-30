package io.pivotal.cities.domain.city.api.dto

import io.pivotal.cities.domain.location.api.CoordinateDto

data class UpdateCityDto(
        val name: String?,
        val description: String?,
        val location: CoordinateDto?)