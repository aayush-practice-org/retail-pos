package io.aygh.identity.service.query.impl;

import io.aygh.identity.dto.response.RoleOptionResponse;
import io.aygh.identity.dto.response.StaffResponse;
import io.aygh.identity.entity.User;
import io.aygh.identity.entity.UserRole;
import io.aygh.identity.helper.UserResolver;
import io.aygh.identity.helper.UserValidation;
import io.aygh.identity.mapper.UserMapper;
import io.aygh.identity.repository.UserRepository;
import io.aygh.identity.service.query.StaffQueryService;
import io.aygh.shared.response.PagedResponse;
import io.aygh.shared.response.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class StaffQueryServiceImpl implements StaffQueryService {

    /** Bound when no role filter is applied — the query never reads it. */
    private static final UserRole ANY_ROLE = UserRole.ADMIN;

    private final UserRepository userRepository;
    private final UserResolver userResolver;
    private final UserValidation userValidation;
    private final UserMapper userMapper;

    @Override
    public PagedResponse<StaffResponse> findAll(String search, UserRole role, Boolean isActive, Pageable pageable) {
        Page<User> staff = userRepository.search(
                search, role == null, role == null ? ANY_ROLE : role, isActive, pageable);

        List<StaffResponse> content = staff.getContent().stream()
                .map(userMapper::toStaffResponse)
                .toList();

        return PaginationUtils.toPagedResponse(staff, content);
    }

    @Override
    public StaffResponse findById(UUID id) {
        return userMapper.toStaffResponse(userResolver.resolve(id));
    }

    @Override
    public List<RoleOptionResponse> assignableRoles() {
        return userValidation.assignableRoles().stream()
                .map(userMapper::toRoleOption)
                .toList();
    }
}
