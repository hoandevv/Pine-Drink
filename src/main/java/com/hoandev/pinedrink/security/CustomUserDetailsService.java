package com.hoandev.pinedrink.security;

import com.hoandev.pinedrink.entity.Account;
import com.hoandev.pinedrink.entity.AccountRoleAssignment;
import com.hoandev.pinedrink.repository.AccountRepository;
import com.hoandev.pinedrink.repository.AccountRoleAssignmentRepository;
import com.hoandev.pinedrink.service.PermissionCacheService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Nạp thông tin người dùng từ database khi xác thực.
 * Lấy role đang hoạt động và permission của account để tạo {@link UserPrincipal}.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;
    private final AccountRoleAssignmentRepository assignmentRepository;
    private final PermissionCacheService permissionCacheService;

    public CustomUserDetailsService(AccountRepository accountRepository, AccountRoleAssignmentRepository assignmentRepository, PermissionCacheService permissionCacheService) {
        this.accountRepository = accountRepository;
        this.assignmentRepository = assignmentRepository;
        this.permissionCacheService = permissionCacheService;
    }

    /**
     * Tìm account theo username và trả về principal cho Spring Security.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return buildPrincipal(account);
    }

    public UserPrincipal loadPrincipalById(String userId) throws UsernameNotFoundException {
        return buildPrincipal(loadAccountById(userId));
    }

    public Account loadAccountById(String userId) throws UsernameNotFoundException {
        return accountRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
    }

    public UserPrincipal buildPrincipal(Account account) {
        List<String> roleAuthorities = assignmentRepository
                .findActiveRoleCodesByAccountId(account.getId(), LocalDateTime.now())
                .stream()
                .map(roleCode -> "ROLE_" + roleCode)
                .distinct()
                .toList();

        return buildPrincipal(account, roleAuthorities);
    }
    /**
     * Tạo principal từ account bao gồm ROLE và PERMISSION authorities.
     */
    public UserPrincipal buildPrincipal(Account account, List<String> roleAuthorities) {
        List<String> permissionAuthorities = permissionCacheService.getPermissionAuthorities(account.getId());
        List<GrantedAuthority> authorities = Stream.concat(roleAuthorities.stream(), permissionAuthorities.stream())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new UserPrincipal(
                account.getId(), account.getUsername(), account.getEmail(),
                account.getPassword(), account.getStatus(), authorities
        );
    }

    /**
     * Tìm user theo ID, dùng cho các luồng xác thực bằng token.
     *
     * @param userId ID người dùng cần tìm
     * @return thông tin đăng nhập của user
     * @throws UsernameNotFoundException nếu không tìm thấy user
     */
    public UserDetails loadUserById(String userId) throws UsernameNotFoundException {
        return loadPrincipalById(userId);
    }
}
