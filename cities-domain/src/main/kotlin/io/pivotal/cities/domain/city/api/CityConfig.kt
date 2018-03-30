package io.pivotal.cities.domain.city.api

import io.pivotal.cities.domain.city.InternalCityConfig
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackageClasses = arrayOf(InternalCityConfig::class))
class CityConfig
