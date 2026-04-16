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

import java.util.List;
import java.util.Locale;
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
        Authority authority = user.getAuthorities().stream().findFirst().orElseThrow(() -> new UsernameNotFoundException("User " + lowercaseLogin + " has no authorities"));
        List<SimpleGrantedAuthority> grantedAuthorities = SecurityUtils.mergeAuthorities(authority, navItemRoleRepository.findAllNavItemCodeByRoleNameAndCanExecuteTrueAndNavItemTargetType(authority.getName(), NavTargetType.ACTION))
            .stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

        return new User(user.getLogin(), user.getPassword(), grantedAuthorities);
    }
}
