package io.pivotal.cities.web.resource

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.pivotal.cities.domain.city.api.dto.CityDto
import io.pivotal.cities.web.conversion.CoordinateType
import org.springframework.hateoas.RepresentationModel

data class CityResource
@JsonCreator
constructor(
        @JsonProperty("id") val _id: String,
        @JsonProperty("name") val name: String,
        @JsonProperty("desc") val description: String?,
        @JsonProperty("loc") val location: CoordinateType) : RepresentationModel<CityResource>() {

    companion object {

        fun fromDto(dto: CityDto): CityResource =
                CityResource(
                        _id = dto.id,
                        name = dto.name,
                        description = dto.description,
                        location = CoordinateType.fromDto(dto.location)
                )
    }
}