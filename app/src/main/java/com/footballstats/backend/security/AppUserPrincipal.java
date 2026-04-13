package com.footballstats.backend.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class AppUserPrincipal implements UserDetails {

    private final Long userId;
    private final String email;
    private final String name;
    private final Collection<? extends GrantedAuthority> authorities;

    public AppUserPrincipal(Long userId, String email, String name, Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.authorities = authorities;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email;
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
        return true;
    }
}
