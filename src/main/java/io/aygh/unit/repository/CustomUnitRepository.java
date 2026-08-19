package io.aygh.unit.repository;

import io.aygh.unit.entity.CustomUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomUnitRepository extends JpaRepository<CustomUnit, UUID> {

    Optional<CustomUnit> findByNameIgnoreCase(String name);
}
