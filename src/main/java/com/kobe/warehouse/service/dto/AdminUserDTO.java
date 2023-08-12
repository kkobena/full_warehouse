package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.Authority;
import com.kobe.warehouse.domain.User;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.*;
import lombok.Getter;

/** A DTO representing a user, with his authorities. */
@Getter
public class AdminUserDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long id;

  @NotBlank
  @Pattern(regexp = Constants.LOGIN_REGEX)
  @Size(min = 1, max = 50)
  private String login;

  @Size(max = 50)
  private String firstName;

  @Size(max = 50)
  private String lastName;

  @Email
  @Size(min = 5, max = 254)
  private String email;

  @Size(max = 256)
  private String imageUrl;

  private boolean activated = false;

  @Size(min = 2, max = 10)
  private String langKey;

  private String createdBy;

  private LocalDateTime createdDate;

  private String lastModifiedBy;

  private LocalDateTime lastModifiedDate;

  private Set<String> authorities;
  private String fullName;
  private String abbrName;

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
    this.authorities =
        user.getAuthorities().stream().map(Authority::getName).collect(Collectors.toSet());
    this.fullName = String.format("%s %s", user.getFirstName(), user.getLastName());
    this.abbrName = String.format("%s. %s", user.getFirstName().charAt(0), user.getLastName());
  }

  public AdminUserDTO setFullName(String fullName) {
    this.fullName = fullName;
    return this;
  }

  public AdminUserDTO setAbbrName(String abbrName) {
    this.abbrName = abbrName;
    return this;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setLogin(String login) {
    this.login = login;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public void setActivated(boolean activated) {
    this.activated = activated;
  }

  public void setLangKey(String langKey) {
    this.langKey = langKey;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public void setAuthorities(Set<String> authorities) {
    this.authorities = authorities;
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
