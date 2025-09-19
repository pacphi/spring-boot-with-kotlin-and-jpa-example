package io.pivotal.cities.domain.city.api.dto

import io.pivotal.cities.domain.location.api.CoordinateDto
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.Valid

data class CreateCityDto(
        @NotEmpty var id: String,
        @NotEmpty var name: String,
        var description: String? = null,
        @Valid var location: CoordinateDto)