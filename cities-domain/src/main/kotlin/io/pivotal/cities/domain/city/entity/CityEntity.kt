package io.pivotal.cities.domain.city.entity

import io.pivotal.cities.domain.city.api.dto.CityDto
import io.pivotal.cities.domain.city.api.dto.CreateCityDto
import io.pivotal.cities.domain.city.api.dto.UpdateCityDto
import io.pivotal.cities.domain.location.jpa.Coordinate
import java.time.LocalDateTime
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table


@Entity
@Table(name = "city")
internal data class CityEntity(
        @Id val id: String? = null,
        val name: String,
        val description: String? = null,
        @Embedded val location: Coordinate,
        val updatedAt: LocalDateTime = LocalDateTime.now(),
        val createdAt: LocalDateTime = LocalDateTime.now()) {

    fun toDto(): CityDto = CityDto(
            id = this.id!!,
            name = this.name,
            description = this.description,
            location = this.location.toDto(),
            updatedAt = this.updatedAt,
            createdAt = this.createdAt
    )

    companion object {

        fun fromDto(dto: CityDto) = CityEntity(
                id = dto.id,
                name = dto.name,
                description = dto.description,
                location = Coordinate.fromDto(dto.location),
                updatedAt = dto.updatedAt,
                createdAt = dto.createdAt)

        fun fromDto(dto: CreateCityDto) = CityEntity(
                id = dto.id,
                name = dto.name,
                description = dto.description,
                location = Coordinate.fromDto(dto.location))

        fun fromDto(dto: UpdateCityDto, defaultCity: CityEntity) = CityEntity(
                id = defaultCity.id!!,
                name = dto.name ?: defaultCity.name,
                description = dto.description ?: defaultCity.description,
                location = if (dto.location != null) Coordinate.fromDto(dto.location) else defaultCity.location,
                updatedAt = LocalDateTime.now(),
                createdAt = defaultCity.createdAt)

    }

}