package io.pivotal.cities.domain.city.api

import io.pivotal.cities.domain.city.api.dto.CityDto
import io.pivotal.cities.domain.city.api.dto.CreateCityDto
import io.pivotal.cities.domain.city.api.dto.UpdateCityDto

interface CityService {

    fun retrieveCity(cityId: String): CityDto?

    fun retrieveCities(): List<CityDto>

    fun addCity(city: CreateCityDto): CityDto

    fun updateCity(id: String, city: UpdateCityDto): CityDto?
	
	fun deleteCity(id: String)
}