package io.aygh.unit.resolver;

import io.aygh.exception.ResourceNotFoundException;
import io.aygh.unit.entity.CustomUnit;
import io.aygh.unit.entity.SystemUnit;
import io.aygh.unit.entity.UnitSource;
import io.aygh.unit.repository.CustomUnitRepository;
import io.aygh.unit.repository.SystemUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UnitResolver {

    private final SystemUnitRepository systemUnitRepository;
    private final CustomUnitRepository customUnitRepository;

    public SystemUnit resolveSystemUnit(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Base unit id cannot be null");
        }
        return systemUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SystemUnit", "id", id));
    }

    public CustomUnit resolveCustomUnit(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Custom unit id cannot be null");
        }
        return customUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CustomUnit", "id", id));
    }

    /**
     * Resolves a unit from either pool. A null source defaults to SYSTEM, so callers
     * that only ever deal with system units can leave it out.
     */
    public ResolvedUnit resolve(UUID id, UnitSource source) {
        if (source == UnitSource.CUSTOM) {
            CustomUnit unit = resolveCustomUnit(id);
            return new ResolvedUnit(
                    unit.getId(),
                    unit.getName(),
                    unit.getSymbol(),
                    unit.getMeasurementType(),
                    unit.getConversionFactor(),
                    UnitSource.CUSTOM
            );
        }

        SystemUnit unit = resolveSystemUnit(id);
        return new ResolvedUnit(
                unit.getId(),
                unit.getName(),
                unit.getSymbol(),
                unit.getMeasurementType(),
                unit.getConversionFactor(),
                UnitSource.SYSTEM
        );
    }
}
