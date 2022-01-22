package com.kobe.warehouse.domain;



import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AppConfiguration setDescription(String description) {
        this.description = description;
        return this;
    }

    public AppConfiguration setName(String name) {
        this.name = name;
        return this;
    }

    public String getValue() {
        return value;
    }

    public AppConfiguration setValue(String value) {
        this.value = value;
        return this;
    }
}
