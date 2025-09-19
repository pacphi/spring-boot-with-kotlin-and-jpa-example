package io.pivotal.cities.domain.city.repository

import io.pivotal.cities.domain.city.entity.CityEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import jakarta.transaction.Transactional

@Repository
@Transactional(Transactional.TxType.MANDATORY)
internal interface CityRepository : JpaRepository<CityEntity, String>