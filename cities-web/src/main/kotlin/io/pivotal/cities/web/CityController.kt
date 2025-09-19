package io.pivotal.cities.web

import io.pivotal.cities.domain.city.api.CityService
import io.pivotal.cities.domain.city.api.dto.CreateCityDto
import io.pivotal.cities.domain.city.api.dto.UpdateCityDto
import io.pivotal.cities.web.CITIES_PATH
import io.pivotal.cities.web.resource.CityResource
import org.slf4j.LoggerFactory
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.util.UriComponentsBuilder

@RestController
@RequestMapping(
        value = arrayOf(CITIES_PATH),
        produces = arrayOf(
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.TEXT_XML_VALUE,
                MediaType.APPLICATION_XML_VALUE))
class CityController(val cityService: CityService) {

	private val log = LoggerFactory.getLogger(CityController::class.java)

    @GetMapping
    fun retrieveCities(): HttpEntity<CollectionModel<CityResource>> {
        log.debug("Retrieving cities")

        val result = cityService.retrieveCities()
        return ResponseEntity.ok(CollectionModel.of(result.map { CityResource.fromDto(it) }))
    }


    @GetMapping("{id}")
    fun retrieveCity(@PathVariable("id") cityId: String): HttpEntity<CityResource> {
        log.debug("Retrieving city: {}", cityId)

        val result = cityService.retrieveCity(cityId)
        if (result != null) {
            val resource = CityResource.fromDto(result)
            resource.add(linkTo(methodOn(this::class.java).retrieveCity(result.id)).withSelfRel())
            return ResponseEntity.ok(resource)
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }

    @PostMapping(consumes = arrayOf(
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.TEXT_XML_VALUE,
            MediaType.APPLICATION_XML_VALUE))
    fun addCity(@RequestBody city: CreateCityDto, uriBuilder: UriComponentsBuilder): HttpEntity<CityResource> {
        log.debug("Request to add a city")

        val result = cityService.addCity(city)
        val resource = CityResource.fromDto(result)
        resource.add(linkTo(methodOn(this::class.java).retrieveCity(result.id)).withSelfRel())
        return ResponseEntity
                .created(uriBuilder.path("$CITIES_PATH/{id}").buildAndExpand(result.id).toUri())
                .body(resource)
    }

    @PutMapping("{id}")
    fun updateCity(@PathVariable("id") cityId: String, @RequestBody city: UpdateCityDto): HttpEntity<CityResource> {
        log.debug("Request to update city: {}", cityId)

        val result = cityService.updateCity(cityId, city)
        if (result != null) {
            val resource = CityResource.fromDto(result)
            resource.add(linkTo(methodOn(this::class.java).retrieveCity(result.id)).withSelfRel())
            return ResponseEntity.ok(resource)
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }

	@DeleteMapping("{id}")
    fun deleteCity(@PathVariable("id") cityId: String): HttpEntity<CityResource> {
        log.debug("Request to update city: {}", cityId)

        cityService.deleteCity(cityId)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}

