package io.aygh.unit.helper;

import io.aygh.exception.BusinessException;
import io.aygh.unit.repository.CustomUnitRepository;
import io.aygh.unit.repository.SystemUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CustomUnitValidation {

    private final CustomUnitRepository customUnitRepository;
    private final SystemUnitRepository systemUnitRepository;

    /**
     * A custom unit may not shadow a system unit, nor collide with another custom one.
     */
    public void validateUniqueName(String name, UUID excludeId) {
        systemUnitRepository.findByName(name).ifPresent(existing -> {
            throw new BusinessException("'" + name + "' is a system unit and cannot be redefined");
        });

        customUnitRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (!existing.getId().equals(excludeId)) {
                throw new BusinessException("Custom unit already exists with name: " + name);
            }
        });
    }
}
