package io.aygh.identity.repository;

import io.aygh.identity.entity.CbmsInternalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CbmsInternalRepository extends JpaRepository<CbmsInternalEntity, Long> {

    Optional<CbmsInternalEntity> findFirstByOrderByIdAsc();
}
