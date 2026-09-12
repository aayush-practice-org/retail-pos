package io.aygh.identity.controller;

import io.aygh.identity.dto.request.CbmsInternalRequest;
import io.aygh.identity.dto.request.CbmsInternalUpdateRequest;
import io.aygh.identity.dto.response.CbmsInternalResponse;
import io.aygh.identity.service.command.CbmsInternalCommandService;
import io.aygh.identity.service.query.CbmsInternalQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cbms-internal")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CbmsInternalController {

    private final CbmsInternalCommandService commandService;
    private final CbmsInternalQueryService queryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CbmsInternalResponse create(@Valid @RequestBody CbmsInternalRequest request) {
        return commandService.createCbmsInternal(request);
    }

    @PutMapping("/{id}")
    public CbmsInternalResponse update(@PathVariable Long id,
                                       @Valid @RequestBody CbmsInternalUpdateRequest request) {
        return commandService.updateCbmsInternal(id, request);
    }

    @GetMapping
    public List<CbmsInternalResponse> getAll() {
        return queryService.getAll();
    }

    @GetMapping("/{id}")
    public CbmsInternalResponse getById(@PathVariable Long id) {
        return queryService.getById(id);
    }
}
