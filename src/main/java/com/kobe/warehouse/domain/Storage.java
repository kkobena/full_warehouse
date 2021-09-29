package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.domain.enumeration.StorageType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
@Table(name = "storage")
@JsonIgnoreProperties(value = {"magasin"})
public class Storage implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "storage_type", nullable = false)
    private StorageType storageType;
    @NotNull
    @Column(name = "name", nullable = false, unique = true)
    private String name;
    @ManyToOne(optional = false)
    @NotNull
    private Magasin magasin;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Magasin getMagasin() {
        return magasin;
    }

    public void setMagasin(Magasin magasin) {
        this.magasin = magasin;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Storage{");
        sb.append("id=").append(id);
        sb.append(", storageType=").append(storageType);
        sb.append(", name='").append(name).append('\'');
        sb.append(", magasin=").append(magasin);
        sb.append('}');
        return sb.toString();
    }
}
