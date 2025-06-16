package com.kobe.warehouse.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.Authority;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.security.SecurityUtils;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A DTO representing a user, with his authorities.
 */
public class AdminUserDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank
    @Pattern(regexp = Constants.LOGIN_REGEX)
    @Size(min = 1, max = 50)
    private String login;

    @Size(max = 70)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Email
    @Size(min = 5, max = 254)
    private String email;

    @Size(max = 256)
    private String imageUrl;

    private Boolean activated;

    @Size(min = 2, max = 10)
    private String langKey;

    private String createdBy;

    private LocalDateTime createdDate;

    private String lastModifiedBy;

    private LocalDateTime lastModifiedDate;

    private Set<String> authorities;
    private String fullName;
    private String abbrName;

    @JsonIgnore
    @Size(min = 6, max = 6)
    @Pattern(regexp = Constants.NUMERIC_PATTERN)
    private String actionAuthorityKey;

    private String roleName;

    public AdminUserDTO() {
        // Empty constructor needed for Jackson.
    }

    public AdminUserDTO(User user) {
        this.id = user.getId();
        this.login = user.getLogin();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.activated = user.isActivated();
        this.imageUrl = user.getImageUrl();
        this.langKey = user.getLangKey();
        this.createdBy = user.getCreatedBy();
        this.createdDate = user.getCreatedDate();
        this.lastModifiedBy = user.getLastModifiedBy();
        this.lastModifiedDate = user.getLastModifiedDate();
        this.fullName = String.format("%s %s", user.getFirstName(), user.getLastName());
        this.abbrName = String.format("%s. %s", user.getFirstName().charAt(0), user.getLastName());
        this.authorities = user.getAuthorities().stream().map(Authority::getName).collect(Collectors.toSet());
    }

    public AdminUserDTO(User user, Set<Authority> authorities0) {
        this.id = user.getId();
        this.login = user.getLogin();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.activated = user.isActivated();
        this.imageUrl = user.getImageUrl();
        this.langKey = user.getLangKey();
        this.createdBy = user.getCreatedBy();
        this.createdDate = user.getCreatedDate();
        this.lastModifiedBy = user.getLastModifiedBy();
        this.lastModifiedDate = user.getLastModifiedDate();
        this.fullName = String.format("%s %s", user.getFirstName(), user.getLastName());
        this.abbrName = String.format("%s. %s", user.getFirstName().charAt(0), user.getLastName());
        this.authorities = SecurityUtils.mergeAuthorities(authorities0);

        for (String authority : this.authorities) {
            if (SecurityUtils.isAdmin(authority)) {
                this.roleName = Constants.PR_MOBILE_ADMIN;
                break;
            }
            if (SecurityUtils.hasMobileAdminAccess(authority)) {
                this.roleName = authority;
                break;
            }
            if (SecurityUtils.hasUserMobileAccess(authority)) {
                this.roleName = authority;
                break;
            }
        }
    }

    private boolean isAdmin() {
        return SecurityUtils.hasCurrentUserThisAuthority(Constants.ROLE_ADMIN);
    }

    private String getMobileProfile(String privilege) {
        if (SecurityUtils.hasMobileAdminAccess(privilege)) {
            return Constants.PR_MOBILE_ADMIN;
        }
        if (SecurityUtils.hasMobileAccess(privilege)) {
            return Constants.PR_MOBILE_USER;
        }
        return null;
    }

    public @Size(min = 6, max = 6) String getActionAuthorityKey() {
        return actionAuthorityKey;
    }

    public AdminUserDTO setActionAuthorityKey(@Size(min = 6, max = 6) String actionAuthorityKey) {
        this.actionAuthorityKey = actionAuthorityKey;
        return this;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotBlank @Pattern(regexp = Constants.LOGIN_REGEX) @Size(min = 1, max = 50) String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public @Size(max = 50) String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public @Size(max = 50) String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public @Email @Size(min = 5, max = 254) String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public @Size(max = 256) String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean isActivated() {
        return activated;
    }

    public void setActivated(Boolean activated) {
        this.activated = activated;
    }

    public @Size(min = 2, max = 10) String getLangKey() {
        return langKey;
    }

    public void setLangKey(String langKey) {
        this.langKey = langKey;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public Set<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Set<String> authorities) {
        this.authorities = authorities;
    }

    public String getFullName() {
        return fullName;
    }

    public AdminUserDTO setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getAbbrName() {
        return abbrName;
    }

    public AdminUserDTO setAbbrName(String abbrName) {
        this.abbrName = abbrName;
        return this;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AdminUserDTO{"
            + "login='"
            + login
            + '\''
            + ", firstName='"
            + firstName
            + '\''
            + ", lastName='"
            + lastName
            + '\''
            + ", email='"
            + email
            + '\''
            + ", imageUrl='"
            + imageUrl
            + '\''
            + ", activated="
            + activated
            + ", langKey='"
            + langKey
            + '\''
            + ", createdBy="
            + createdBy
            + ", createdDate="
            + createdDate
            + ", lastModifiedBy='"
            + lastModifiedBy
            + '\''
            + ", lastModifiedDate="
            + lastModifiedDate
            + ", authorities="
            + authorities
            + "}";
    }
}
