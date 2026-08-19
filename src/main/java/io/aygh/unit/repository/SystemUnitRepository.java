package io.aygh.unit.repository;

import io.aygh.unit.entity.SystemUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemUnitRepository extends JpaRepository<SystemUnit, UUID> {

    Optional<SystemUnit> findByName(String name);
}
