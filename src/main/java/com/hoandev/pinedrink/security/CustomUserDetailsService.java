package com.hoandev.pinedrink.security;

import com.hoandev.pinedrink.entity.Account;
import com.hoandev.pinedrink.entity.AccountRoleAssignment;
import com.hoandev.pinedrink.repository.AccountRepository;
import com.hoandev.pinedrink.repository.AccountRoleAssignmentRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Loads user details from the database during authentication.
 * Resolves roles via {@link AccountRoleAssignment} and maps them to granted authorities.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;
    private final AccountRoleAssignmentRepository assignmentRepository;

    public CustomUserDetailsService(AccountRepository accountRepository,
                                    AccountRoleAssignmentRepository assignmentRepository) {
        this.accountRepository = accountRepository;
        this.assignmentRepository = assignmentRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<GrantedAuthority> authorities = assignmentRepository
                .findByAccountId(account.getId())
                .stream()
                .filter(a -> "ACTIVE".equals(a.getStatus()))
                .filter(a -> a.getExpiresAt() == null || a.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(a -> new SimpleGrantedAuthority("ROLE_" + a.getRole().getCode()))
                .distinct()
                .collect(Collectors.toList());

        return new UserPrincipal(
                account.getId(), account.getUsername(), account.getEmail(),
                account.getPassword(), account.getStatus(), authorities
        );
    }
}
