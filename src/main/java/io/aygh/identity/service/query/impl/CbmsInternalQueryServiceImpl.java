package io.aygh.identity.service.query.impl;

import io.aygh.exception.ResourceNotFoundException;
import io.aygh.identity.dto.response.CbmsInternalResponse;
import io.aygh.identity.mapper.CbmsInternalMapper;
import io.aygh.identity.repository.CbmsInternalRepository;
import io.aygh.identity.service.query.CbmsInternalQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
public class CbmsInternalQueryServiceImpl implements CbmsInternalQueryService {

    private final CbmsInternalRepository cbmsInternalRepository;
    private final CbmsInternalMapper cbmsInternalMapper;

    @Override
    public List<CbmsInternalResponse> getAll() {
        return cbmsInternalRepository.findAll().stream()
                .map(cbmsInternalMapper::toResponse)
                .toList();
    }

    @Override
    public CbmsInternalResponse getById(Long id) {
        return cbmsInternalRepository.findById(id)
                .map(cbmsInternalMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("CbmsInternal", "id", id));
    }
}
