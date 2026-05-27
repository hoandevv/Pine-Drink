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
                .findActiveRoleCodesByAccountId(account.getId(), LocalDateTime.now())
                .stream()
                .map(roleCode -> new SimpleGrantedAuthority("ROLE_" + roleCode))
                .distinct()
                .collect(Collectors.toList());

        return new UserPrincipal(
                account.getId(), account.getUsername(), account.getEmail(),
                account.getPassword(), account.getStatus(), authorities
        );
    }

    /**
     * Loads user details by user ID (used for reset token authentication).
     *
     * @param userId the user ID to search for
     * @return the user details
     * @throws UsernameNotFoundException if the user is not found
     */
    public UserDetails loadUserById(String userId) throws UsernameNotFoundException {
        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        List<GrantedAuthority> authorities = assignmentRepository
                .findActiveRoleCodesByAccountId(account.getId(), LocalDateTime.now())
                .stream()
                .map(roleCode -> new SimpleGrantedAuthority("ROLE_" + roleCode))
                .distinct()
                .collect(Collectors.toList());

        return new UserPrincipal(
                account.getId(), account.getUsername(), account.getEmail(),
                account.getPassword(), account.getStatus(), authorities
        );
    }
}
