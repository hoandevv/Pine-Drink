package com.hoandev.pinedrink.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class UserPrincipal implements UserDetails {

    private final String id;
    private final String username;
    private final String email;
    private final String password;
    private final String status;
    private final List<GrantedAuthority> authorities;

    public UserPrincipal(String id, String username, String email, String password,
                          String status, List<GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.status = status;
        this.authorities = authorities;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public List<String> getRoleAuthorities() {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .distinct()
                .collect(Collectors.toList());
    }

    public List<String> getPermissionAuthorities() {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("PERM_"))
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(status);
    }
}
