package io.aygh.inventory.service.command.impl;

import io.aygh.inventory.dto.request.UnitRequest;
import io.aygh.inventory.dto.response.UnitResponse;
import io.aygh.inventory.entity.Unit;
import io.aygh.inventory.helper.InventoryResolver;
import io.aygh.inventory.helper.InventoryValidation;
import io.aygh.inventory.mapper.UnitMapper;
import io.aygh.inventory.repository.UnitRepository;
import io.aygh.inventory.service.command.UnitCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UnitCommandServiceImpl implements UnitCommandService {

    private final UnitRepository unitRepository;
    private final InventoryResolver resolver;
    private final InventoryValidation validation;
    private final UnitMapper unitMapper;

    @Override
    public UnitResponse create(UnitRequest request) {
        validation.requireUnitNameAvailable(request.name(), null);
        validation.requireUnitSymbolAvailable(request.symbol(), null);

        Unit unit = unitMapper.toEntity(request);
        Unit saved = unitRepository.save(unit);

        log.info("Added unit '{}' ({}) to the dictionary", saved.getName(), saved.getSymbol());
        return unitMapper.toResponse(saved);
    }

    @Override
    public UnitResponse update(Long id, UnitRequest request) {
        Unit unit = resolver.unit(id);

        validation.rejectSystemUnitChange(unit, "edited");
        validation.requireUnitNameAvailable(request.name(), id);
        validation.requireUnitSymbolAvailable(request.symbol(), id);

        unitMapper.applyUpdate(request, unit);
        Unit saved = unitRepository.save(unit);

        log.info("Updated unit '{}'", saved.getName());
        return unitMapper.toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        Unit unit = resolver.unit(id);

        validation.rejectSystemUnitChange(unit, "removed");
        validation.requireUnitUnused(unit);

        unitRepository.delete(unit);
        log.info("Removed unit '{}'", unit.getName());
    }
}
