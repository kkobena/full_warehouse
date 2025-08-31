package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.Authority;
import com.kobe.warehouse.domain.AppUser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/** A DTO representing a user, with only the public attributes. */
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank
    @Pattern(regexp = Constants.LOGIN_REGEX)
    @Size(min = 1, max = 70)
    private String login;

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Email
    @Size(min = 0, max = 254)
    private String email;

    @Size(max = 256)
    private String imageUrl;

    private boolean activated = false;

    @Size(min = 2, max = 10)
    private String langKey = "fr";

    private String createdBy;

    private LocalDateTime createdDate;

    private String lastModifiedBy;

    private LocalDateTime lastModifiedDate;

    private Set<String> authorities;

    private String fullName;

    private String abbrName;

    public UserDTO() {}

    public UserDTO(AppUser user) {
        id = user.getId();
        login = user.getLogin();
        firstName = user.getFirstName();
        lastName = user.getLastName();
        email = user.getEmail();
        activated = user.isActivated();
        imageUrl = user.getImageUrl();
        langKey = user.getLangKey();
        createdBy = user.getCreatedBy();
        createdDate = user.getCreatedDate();
        lastModifiedBy = user.getLastModifiedBy();
        lastModifiedDate = user.getLastModifiedDate();
        authorities = user.getAuthorities().stream().map(Authority::getName).collect(Collectors.toSet());
        fullName = String.format("%s %s", user.getFirstName(), user.getLastName());
        abbrName = String.format("%s. %s", user.getFirstName().charAt(0), user.getLastName());
    }

    public static UserDTO user(AppUser user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setEmail(user.getEmail());
        userDTO.setAbbrName(String.format("%s. %s", user.getFirstName().charAt(0), user.getLastName()));
        userDTO.setFullName(String.format("%s %s", user.getFirstName(), user.getLastName()));
        userDTO.setFirstName(user.getFirstName());
        userDTO.setLastName(user.getLastName());
        return userDTO;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotBlank @Pattern(regexp = Constants.LOGIN_REGEX) @Size(min = 1, max = 70) String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public @Size(max = 100) String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public @Size(max = 100) String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public @Email @Size(min = 0, max = 254) String getEmail() {
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

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
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

    public Set<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Set<String> authorities) {
        this.authorities = authorities;
    }

    public String getFullName() {
        return fullName;
    }

    public UserDTO setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getAbbrName() {
        return abbrName;
    }

    public UserDTO setAbbrName(String abbrName) {
        this.abbrName = abbrName;
        return this;
    }

    public String getLangKey() {
        if (langKey == null) {
            langKey = "fr";
        }
        return langKey;
    }

    public void setLangKey(String langKey) {
        this.langKey = langKey;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    // prettier-ignore
  @Override
  public String toString() {
    return "UserDTO{" + "id='" + id + '\'' + ", login='" + login + '\'' + "}";
  }
}
