package io.pivotal.cities.domain.location.jpa

import io.pivotal.cities.domain.DoubleAttributeConverter
import io.pivotal.cities.domain.location.api.CoordinateDto
import jakarta.persistence.Convert
import jakarta.persistence.Embeddable

@Embeddable
internal data class Coordinate(
        @Convert(converter = DoubleAttributeConverter::class) val longitude: Double,
        @Convert(converter = DoubleAttributeConverter::class) val latitude: Double) {

    private constructor() : this(0.0, 0.0)

    fun toDto(): CoordinateDto = CoordinateDto(this.longitude, this.latitude)

    companion object {

        fun origin() = Coordinate()

        fun fromDto(dto: CoordinateDto): Coordinate = Coordinate(dto.longitude, dto.latitude)
    }
}