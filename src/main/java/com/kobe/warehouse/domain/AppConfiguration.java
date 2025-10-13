package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ParametreValueType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_configuration")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class AppConfiguration implements Serializable {

    @Serial
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

    @ManyToOne
    @JoinColumn(name = "validated_by_id", referencedColumnName = "id")
    private AppUser validatedBy;

    @Column(name = "other_value")
    private String otherValue;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 20)
    private ParametreValueType valueType;

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

    public @NotNull ParametreValueType getValueType() {
        return valueType;
    }

    public AppConfiguration setValueType(@NotNull ParametreValueType valueType) {
        this.valueType = valueType;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public AppConfiguration setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public AppUser getValidatedBy() {
        return validatedBy;
    }

    public AppConfiguration setValidatedBy(AppUser validatedBy) {
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
        String sb =
            "AppConfiguration{" + "name='" + name + '\'' + ", value='" + value + '\'' + ", description='" + description + '\'' + '}';
        return sb;
    }
}
