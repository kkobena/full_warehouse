package com.kobe.warehouse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_configuration")
public class AppConfiguration implements Serializable {

  private static final long serialVersionUID = 1L;

  @NotNull
  @Size(max = 50)
  @Id
  @Column(length = 50)
  private String name;

  @NotNull
  @Column(name = "value", nullable = false)
  private String value;

  @NotNull
  @Column(name = "description", nullable = false)
  private String description;

  @Column(name = "created")
  private LocalDateTime created;

  @Column(name = "updated")
  private LocalDateTime updated = LocalDateTime.now();

  @ManyToOne private User validatedBy;

  @Column(name = "other_value")
  private String otherValue;

  public String getOtherValue() {
    return otherValue;
  }

  public AppConfiguration setOtherValue(String otherValue) {
    this.otherValue = otherValue;
    return this;
  }

  public LocalDateTime getCreated() {
    return created;
  }

  public AppConfiguration setCreated(LocalDateTime created) {
    this.created = created;
    return this;
  }

  public LocalDateTime getUpdated() {
    return updated;
  }

  public AppConfiguration setUpdated(LocalDateTime updated) {
    this.updated = updated;
    return this;
  }

  public User getValidatedBy() {
    return validatedBy;
  }

  public AppConfiguration setValidatedBy(User validatedBy) {
    this.validatedBy = validatedBy;
    return this;
  }

  public String getName() {
    return name;
  }

  public AppConfiguration setName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public AppConfiguration setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getValue() {
    return value;
  }

  public AppConfiguration setValue(String value) {
    this.value = value;
    return this;
  }

  @Override
  public String toString() {
      String sb = "AppConfiguration{" + "name='" + name + '\''
          + ", value='" + value + '\''
          + ", description='" + description + '\''
          + '}';
    return sb;
  }
}
