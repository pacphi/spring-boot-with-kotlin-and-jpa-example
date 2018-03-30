package io.pivotal.cities.domain.city.api.dto

import io.pivotal.cities.domain.location.api.CoordinateDto
import javax.validation.constraints.NotEmpty
import javax.validation.Valid

data class CreateCityDto(
        @NotEmpty var id: String,
        @NotEmpty var name: String,
        var description: String? = null,
        @Valid var location: CoordinateDto)