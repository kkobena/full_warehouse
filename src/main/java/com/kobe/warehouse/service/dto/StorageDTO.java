package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Storage;

public class StorageDTO {

    private String name;
    private Long id;
    private String storageType;
    private String magasinName;
    private Long magasinId;

    public StorageDTO() {}

    public StorageDTO(Storage storage) {
        this.name = storage.getName();
        this.id = storage.getId();
        this.storageType = storage.getStorageType().getValue();
        Magasin magasin = storage.getMagasin();
        this.magasinName = magasin.getName();
        this.magasinId = magasin.getId();
    }

    public String getName() {
        return name;
    }

    public StorageDTO setName(String name) {
        this.name = name;
        return this;
    }

    public Long getId() {
        return id;
    }

    public StorageDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public String getStorageType() {
        return storageType;
    }

    public StorageDTO setStorageType(String storageType) {
        this.storageType = storageType;
        return this;
    }

    public String getMagasinName() {
        return magasinName;
    }

    public StorageDTO setMagasinName(String magasinName) {
        this.magasinName = magasinName;
        return this;
    }

    public Long getMagasinId() {
        return magasinId;
    }

    public StorageDTO setMagasinId(Long magasinId) {
        this.magasinId = magasinId;
        return this;
    }
}
