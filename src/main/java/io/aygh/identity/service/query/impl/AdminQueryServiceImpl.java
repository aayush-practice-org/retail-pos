package io.aygh.identity.service.query.impl;

import io.aygh.identity.dto.response.AdminResponse;
import io.aygh.identity.entity.Admin;
import io.aygh.identity.entity.ProvisioningStatus;
import io.aygh.identity.helper.AdminResolver;
import io.aygh.identity.mapper.AdminMapper;
import io.aygh.identity.repository.AdminRepository;
import io.aygh.identity.service.query.AdminQueryService;
import io.aygh.shared.response.PagedResponse;
import io.aygh.shared.response.PaginationUtils;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminQueryServiceImpl implements AdminQueryService {

    private final AdminRepository adminRepository;
    private final AdminResolver adminResolver;
    private final AdminMapper adminMapper;

    @Override
    public AdminResponse findById(UUID id) {
        return adminMapper.toResponse(adminResolver.byId(id));
    }

    @Override
    public PagedResponse<AdminResponse> findAll(String search, ProvisioningStatus provisioningStatus,
                                                Pageable pageable) {
        // allOf rejects nulls, so an absent filter contributes no predicate at all
        // rather than one matching on null.
        List<Specification<Admin>> filters = Stream.of(matches(search), hasProvisioningStatus(provisioningStatus))
                .filter(Objects::nonNull)
                .toList();

        Page<Admin> page = adminRepository.findAll(Specification.allOf(filters), pageable);
        return PaginationUtils.toPagedResponse(page, page.map(adminMapper::toResponse).getContent());
    }

    /**
     * Free text over what someone would actually look a mart up by.
     */
    private static Specification<Admin> matches(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            // The account is a separate table; joining it here keeps the username
            // and email searchable without a second round trip per row.
            var user = root.join("user", JoinType.LEFT);
            return cb.or(
                    cb.like(cb.lower(root.get("companyName")), pattern),
                    cb.like(cb.lower(root.get("slug")), pattern),
                    cb.like(cb.lower(user.get("username")), pattern),
                    cb.like(cb.lower(user.get("email")), pattern));
        };
    }

    private static Specification<Admin> hasProvisioningStatus(ProvisioningStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("provisioningStatus"), status);
    }
}
