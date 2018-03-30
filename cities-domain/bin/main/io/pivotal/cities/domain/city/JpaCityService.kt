package io.pivotal.cities.domain.city

import io.pivotal.cities.domain.city.api.CityService
import io.pivotal.cities.domain.city.api.dto.CityDto
import io.pivotal.cities.domain.city.api.dto.CreateCityDto
import io.pivotal.cities.domain.city.api.dto.UpdateCityDto
import io.pivotal.cities.domain.city.entity.CityEntity
import io.pivotal.cities.domain.city.repository.CityRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
@Transactional
internal class JpaCityService(val cityRepo: CityRepository) : CityService {

	private val log = LoggerFactory.getLogger(JpaCityService::class.java)
	 
    override fun retrieveCity(cityId: String): CityDto? {
        log.debug("Retrieving city: {}", cityId)
        val currentCity = cityRepo.findById(cityId).orElse(null)
        return if (currentCity != null) currentCity.toDto()
		else null
    }

    override fun retrieveCities(): List<CityDto> {
        log.debug("Retrieving cities")

        return cityRepo.findAll().map { it.toDto() }
    }

    override fun updateCity(id: String, city: UpdateCityDto): CityDto? {
        log.debug("Updating city: {} with data: {}", id, city)

        val currentCity = cityRepo.findById(id).orElse(null)
        return if (currentCity != null) cityRepo.save(CityEntity.fromDto(city, currentCity)).toDto()
        else null
    }

    override fun addCity(city: CreateCityDto): CityDto {
        log.debug("Adding City: {}", city)

        return cityRepo.save(CityEntity.fromDto(city)).toDto()
    }
	
	override fun deleteCity(id: String) {
		log.debug("Deleting city with id: {}", id)
		
		cityRepo.deleteById(id)
	}
}