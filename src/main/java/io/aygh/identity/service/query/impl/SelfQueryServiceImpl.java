package io.aygh.identity.service.query.impl;

import io.aygh.exception.BusinessException;
import io.aygh.identity.dto.request.ChangePasswordRequest;
import io.aygh.identity.dto.response.SelfResponse;
import io.aygh.identity.entity.User;
import io.aygh.identity.helper.UserResolver;
import io.aygh.identity.mapper.UserMapper;
import io.aygh.identity.service.query.SelfQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 *  Whoever is holding the token, acting on their own account.
 *
 *  The account is always resolved from the token — never from a path variable —
 *  so one member of staff can never reach another's password.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
class SelfQueryServiceImpl implements SelfQueryService {

    private final UserResolver userResolver;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public SelfResponse getSelfInfo() {
        return userMapper.toSelfResponse(userResolver.resolveCurrent());
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = userResolver.resolveCurrent();

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            log.warn("Password change rejected for {} — current password did not match", user.getUsername());
            throw new BusinessException("Current password doesn't match. Try again");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessException("The new password must be different from the current one");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        log.info("Password changed for {}", user.getUsername());
    }
}
