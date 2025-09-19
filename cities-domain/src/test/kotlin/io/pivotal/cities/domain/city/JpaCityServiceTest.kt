package io.pivotal.cities.domain.city

import io.pivotal.cities.domain.city.api.CityConfig
import io.pivotal.cities.domain.city.api.CityService
import io.pivotal.cities.domain.city.api.dto.CreateCityDto
import io.pivotal.cities.domain.city.api.dto.UpdateCityDto
import io.pivotal.cities.domain.city.entity.CityEntity
import io.pivotal.cities.domain.city.repository.CityRepository
import io.pivotal.cities.domain.location.api.CoordinateDto
import io.pivotal.cities.domain.location.jpa.Coordinate
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.slf4j.Logger
import org.springframework.beans.factory.InjectionPoint
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Scope
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = arrayOf(
        JpaCityServiceTest.Config::class,
        CityConfig::class))
@DataJpaTest
internal class JpaCityServiceTest {

    class Config {

        @Bean
        @Scope("prototype")
        fun logger(): Logger = mock(Logger::class.java)
    }

    @Autowired
    lateinit var service: CityService
    @Autowired
    lateinit var repository: CityRepository


    @Test
    fun `'retrieveCities' should retrieve empty list if repository doesn't contain entities`() {
        assertThat(service.retrieveCities()).isEmpty()
    }

    @Test
    fun `'retrieveCity' should return null if city for cityId doesnt exist`() {
        assertThat(service.retrieveCity("invalid")).isNull()
    }

    @Test
    fun `'retrieveCity' should map existing entity from repository`() {
        repository.save(CityEntity("city", "cityname", "description", Coordinate(1.0, -1.0)))

        val result = service.retrieveCity("city")
        SoftAssertions().apply {
            assertThat(result?.id).isNotNull
            assertThat(result?.name).isEqualTo("cityname")
            assertThat(result?.description).isEqualTo("description")
            assertThat(result?.location).isEqualTo(CoordinateDto(1.0, -1.0))
            assertAll()
        }
    }

    @Test
    fun `'retrieveCities' should map entity from repository`() {
        repository.save(CityEntity("city", "cityname", "description", Coordinate(1.0, -1.0)))
        val result = service.retrieveCities()

        SoftAssertions().apply {
            assertThat(result).hasSize(1)
            result.forEach {
                assertThat(it.id).isNotNull
                assertThat(it.name).isEqualTo("cityname")
                assertThat(it.description).isEqualTo("description")
                assertThat(it.location).isEqualTo(CoordinateDto(1.0, -1.0))
            }
            assertAll()
        }
    }

    @Test
    fun `'addCity' should return created entity`() {
        val (id, name, description, location) = service.addCity(CreateCityDto("id", "name", "description", CoordinateDto(1.0, 1.0)))
        SoftAssertions().apply {
            assertThat(id).isEqualTo("id")
            assertThat(name).isEqualTo("name")
            assertThat(description).isEqualTo("description")
            assertThat(location).isEqualTo(CoordinateDto(1.0, 1.0))
            assertAll()
        }
    }

    @Test
    fun `'updateCity' should update existing values`() {
        val existingCity = repository.save(CityEntity("city", "cityname", "description", Coordinate(1.0, -1.0))).toDto()

        Thread.sleep(1)

        val result = service.updateCity(existingCity.id, UpdateCityDto("new name", "new description", CoordinateDto(-1.0, -1.0)))

        SoftAssertions().apply {
            assertThat(result).isNotNull
            assertThat(result?.id).isEqualTo(existingCity.id)
            assertThat(result?.name).isEqualTo("new name")
            assertThat(result?.description).isEqualTo("new description")
            assertThat(result?.location).isEqualTo(CoordinateDto(-1.0, -1.0))
            assertThat(result?.updatedAt).isAfter(existingCity.updatedAt)
            assertThat(result?.createdAt).isEqualTo(existingCity.createdAt)
            assertAll()
        }
    }

    @Test
    fun `'updateCity' shouldn't update null values`() {
        val existingCity = repository.save(CityEntity(
                id = "city",
                name = "cityname",
                description = "description",
                location = Coordinate(1.0, -1.0),
                updatedAt = LocalDateTime.now().minusYears(1))).toDto()

        Thread.sleep(1)

        val result = service.updateCity(existingCity.id, UpdateCityDto(null, null, null))

        SoftAssertions().apply {
            assertThat(result).isNotNull
            assertThat(result?.id).isEqualTo(existingCity.id)
            assertThat(result?.name).isEqualTo("cityname")
            assertThat(result?.description).isEqualTo("description")
            assertThat(result?.location).isEqualTo(CoordinateDto(1.0, -1.0))
            assertThat(result?.updatedAt).isAfter(existingCity.updatedAt)
            assertThat(result?.createdAt).isEqualTo(existingCity.createdAt)
            assertAll()
        }
    }
}
