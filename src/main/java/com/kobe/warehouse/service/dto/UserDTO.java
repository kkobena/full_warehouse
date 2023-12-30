package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.Authority;
import com.kobe.warehouse.domain.User;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * A DTO representing a user, with only the public attributes.
 */
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Getter
    private Long id;

    @Getter
    @NotBlank
    @Pattern(regexp = Constants.LOGIN_REGEX)
    @Size(min = 1, max = 70)
    private String login;

    @Getter
    @Size(max = 100)
    private String firstName;

    @Getter
    @Size(max = 100)
    private String lastName;

    @Getter
    @Email
    @Size(min = 0, max = 254)
    private String email;

    @Getter
    @Size(max = 256)
    private String imageUrl;

    @Getter
    private boolean activated = false;

    @Size(min = 2, max = 10)
    private String langKey = "fr";

    @Getter
    private String createdBy;

    @Getter
    private LocalDateTime createdDate;

    @Getter
    private String lastModifiedBy;

    private LocalDateTime lastModifiedDate;

    @Getter
    private Set<String> authorities;
    @Getter
    private String fullName;
    @Getter
    private String abbrName;

    public UserDTO() {
    }

    public UserDTO(User user) {
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

    public static UserDTO user(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setEmail(user.getEmail());
        userDTO.setAbbrName(String.format("%s. %s", user.getFirstName().charAt(0), user.getLastName()));
        userDTO.setFullName(String.format("%s %s", user.getFirstName(), user.getLastName()));
        userDTO.setFirstName(user.getFirstName());
        userDTO.setLastName(userDTO.getLastName());
        return userDTO;
    }

    public UserDTO setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public UserDTO setAbbrName(String abbrName) {
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

    public String getLangKey() {
        if (langKey == null) {
            langKey = "fr";
        }
        return langKey;
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

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
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
        return "UserDTO{" +
            "id='" + id + '\'' +
            ", login='" + login + '\'' +
            "}";
    }
}
