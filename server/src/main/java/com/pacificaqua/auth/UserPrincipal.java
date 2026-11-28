package com.pacificaqua.auth;

import com.pacificaqua.domain.User;
import com.pacificaqua.domain.enums.UserRole;
import com.pacificaqua.domain.enums.UserStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserPrincipal implements UserDetails {
  private final Long id;
  private final String name;
  private final String phone;
  private final String passwordHash;
  private final UserRole role;
  private final UserStatus status;

  public UserPrincipal(User user) {
    this.id = user.getId();
    this.name = user.getName();
    this.phone = user.getPhone();
    this.passwordHash = user.getPasswordHash();
    this.role = user.getRole();
    this.status = user.getStatus();
  }

  public Long getId() { return id; }
  public String getName() { return name; }
  public UserRole getRole() { return role; }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  @Override
  public String getPassword() { return passwordHash; }

  @Override
  public String getUsername() { return phone; }

  @Override
  public boolean isAccountNonExpired() { return true; }

  @Override
  public boolean isAccountNonLocked() { return status == UserStatus.ACTIVE; }

  @Override
  public boolean isCredentialsNonExpired() { return true; }

  @Override
  public boolean isEnabled() { return status == UserStatus.ACTIVE; }
}
