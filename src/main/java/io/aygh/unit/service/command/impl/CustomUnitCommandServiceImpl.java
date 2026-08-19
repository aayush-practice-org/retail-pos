package io.aygh.unit.service.command.impl;

import io.aygh.unit.dto.request.CreateCustomUnitRequest;
import io.aygh.unit.dto.response.UnitResponse;
import io.aygh.unit.entity.CustomUnit;
import io.aygh.unit.helper.CustomUnitValidation;
import io.aygh.unit.mapper.CustomUnitMapper;
import io.aygh.unit.repository.CustomUnitRepository;
import io.aygh.unit.resolver.UnitResolver;
import io.aygh.unit.service.command.CustomUnitCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
class CustomUnitCommandServiceImpl implements CustomUnitCommandService {

    private final CustomUnitRepository customUnitRepository;
    private final CustomUnitMapper customUnitMapper;
    private final CustomUnitValidation customUnitValidation;
    private final UnitResolver unitResolver;

    @Override
    @Transactional
    public UnitResponse createCustomUnit(CreateCustomUnitRequest request) {
        log.info("Creating custom unit: {}", request.getName());
        customUnitValidation.validateUniqueName(request.getName(), null);

        CustomUnit customUnit = customUnitMapper.toEntity(request);

        return customUnitMapper.toUnitResponse(customUnitRepository.save(customUnit));
    }

    @Override
    @Transactional
    public UnitResponse updateCustomUnit(UUID id, CreateCustomUnitRequest request) {
        log.info("Updating custom unit id: {}", id);
        CustomUnit customUnit = unitResolver.resolveCustomUnit(id);

        customUnitValidation.validateUniqueName(request.getName(), id);

        customUnit.setName(request.getName());
        customUnit.setSymbol(request.getSymbol());
        customUnit.setMeasurementType(request.getMeasurementType());
        customUnit.setConversionFactor(request.getConversionFactor());

        return customUnitMapper.toUnitResponse(customUnitRepository.save(customUnit));
    }

    @Override
    @Transactional
    public void deleteCustomUnit(UUID id) {
        log.info("Deleting custom unit id: {}", id);
        customUnitRepository.delete(unitResolver.resolveCustomUnit(id));
    }
}
