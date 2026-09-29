package com.kobe.warehouse.security;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Authority;
import com.kobe.warehouse.domain.enumeration.NavTargetType;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.repository.nav.NavItemRoleRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authenticate a user from the database.
 */
@Component("userDetailsService")
public class DomainUserDetailsService implements UserDetailsService {


    private final UserRepository userRepository;
    private final NavItemRoleRepository navItemRoleRepository;

    public DomainUserDetailsService(UserRepository userRepository, NavItemRoleRepository navItemRoleRepository) {
        this.userRepository = userRepository;
        this.navItemRoleRepository = navItemRoleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(final String login) {
        String lowercaseLogin = login.toLowerCase(Locale.FRENCH);
        return userRepository
            .findOneWithAuthoritiesByLogin(lowercaseLogin)
            .map(user -> createSpringSecurityUser(lowercaseLogin, user))
            .orElseThrow(() -> new UsernameNotFoundException("User " + lowercaseLogin + " was not found in the database"));
    }

    private User createSpringSecurityUser(String lowercaseLogin, AppUser user) {
        if (!user.isActivated()) {
            throw new UserNotActivatedException("User " + lowercaseLogin + " was not activated");
        }
        // Tous les rôles, et les actions de leur union : un utilisateur à deux rôles n'avait que
        // celles du premier.
        Set<String> roles = user.getAuthorities().stream().map(Authority::getName).collect(Collectors.toSet());
        if (roles.isEmpty()) {
            throw new UsernameNotFoundException("User " + lowercaseLogin + " has no authorities");
        }
        Set<String> authorities = new HashSet<>(roles);
        authorities.addAll(navItemRoleRepository.findExecutableCodesByRoles(roles, NavTargetType.ACTION));
        List<SimpleGrantedAuthority> grantedAuthorities = authorities.stream().map(SimpleGrantedAuthority::new).toList();

        return new User(user.getLogin(), user.getPassword(), grantedAuthorities);
    }
}
