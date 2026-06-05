package com.astral.express.pccms.identity.service;

import com.astral.express.pccms.user.entity.Users;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomUserDetails implements UserDetails {
    UUID id;
    String email;
    String password;
    boolean active;
    Set<GrantedAuthority> authorities;

    public CustomUserDetails(Users user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.active = user.getStatusCode() == com.astral.express.pccms.user.entity.UserStatus.ACTIVE;
        Set<GrantedAuthority> granted = new HashSet<>();
        granted.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getCode()));
        user.getRole().getPermissions()
                .forEach(p -> granted.add(new SimpleGrantedAuthority(p.getCode())));
        this.authorities = granted;
    }

    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return password; }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override public boolean isEnabled() { return active; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
}
