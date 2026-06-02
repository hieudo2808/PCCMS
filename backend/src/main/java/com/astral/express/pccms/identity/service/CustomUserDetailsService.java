package com.astral.express.pccms.identity.service;

import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import com.astral.express.pccms.common.exception.BusinessException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static com.astral.express.pccms.common.exception.ErrorCode.ERR_ACC_002_USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        Users user = userRepository.findByEmailWithRoleAndPermissions(email)
                .orElseThrow(() -> new BusinessException(ERR_ACC_002_USER_NOT_FOUND));
        return new CustomUserDetails(user);
    }
}
