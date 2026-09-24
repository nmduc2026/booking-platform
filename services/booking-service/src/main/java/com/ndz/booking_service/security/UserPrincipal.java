package com.ndz.booking_service.security;

import com.ndz.booking_service.entity.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final Role role;
    private final List<UUID> shopIds;

    public UserPrincipal(UUID id, String email, Role role, List<UUID> shopIds) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.shopIds = shopIds == null ? List.of() : List.copyOf(shopIds);
    }

    public UUID getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public List<UUID> getShopIds() {
        return shopIds;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
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
