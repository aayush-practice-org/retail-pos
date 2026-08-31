package io.aygh.identity.service.query.impl;

import io.aygh.exception.BusinessException;
import io.aygh.identity.dto.response.StaffResponse;
import io.aygh.identity.entity.User;
import io.aygh.identity.entity.UserRole;
import io.aygh.identity.entity.UserStatus;
import io.aygh.identity.helper.UserResolver;
import io.aygh.identity.mapper.StaffMapper;
import io.aygh.identity.repository.UserRepository;
import io.aygh.identity.service.query.StaffQueryService;
import io.aygh.shared.UserHolder;
import io.aygh.shared.response.PagedResponse;
import io.aygh.shared.response.PaginationUtils;
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
public class StaffQueryServiceImpl implements StaffQueryService {

    private final UserRepository userRepository;
    private final UserResolver userResolver;
    private final StaffMapper staffMapper;

    @Override
    public StaffResponse findById(UUID id) {
        return staffMapper.toResponse(userResolver.byIdInTenant(id, currentTenantId()));
    }

    @Override
    public PagedResponse<StaffResponse> findAll(String search, UserRole role, UserStatus status, Pageable pageable) {
        List<Specification<User>> filters = Stream.of(
                        inTenant(currentTenantId()), isStaff(), matches(search), hasRole(role), hasStatus(status))
                .filter(Objects::nonNull)
                .toList();

        Page<User> page = userRepository.findAll(Specification.allOf(filters), pageable);
        return PaginationUtils.toPagedResponse(page, page.map(staffMapper::toResponse).getContent());
    }

    // ── Filters ───────────────────────────────────────────────────────────

    private static Specification<User> inTenant(UUID tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    /**
     * Keeps the admin's own account out of its staff list.
     */
    private static Specification<User> isStaff() {
        return (root, query, cb) -> root.get("role").in(UserRole.assignableByAdmin());
    }

    /**
     * Free text over the three fields someone would actually search a person by.
     */
    private static Specification<User> matches(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("username")), pattern),
                cb.like(cb.lower(root.get("email")), pattern),
                cb.like(cb.lower(root.get("fullName")), pattern));
    }

    private static Specification<User> hasRole(UserRole role) {
        return role == null ? null : (root, query, cb) -> cb.equal(root.get("role"), role);
    }

    private static Specification<User> hasStatus(UserStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static UUID currentTenantId() {
        UUID tenantId = UserHolder.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Staff accounts belong to a mart — sign in as that mart's admin");
        }
        return tenantId;
    }
}
