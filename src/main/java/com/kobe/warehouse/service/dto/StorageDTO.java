package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Storage;

public class StorageDTO {

    private String name;
    private Integer id;
    private String storageType;
    private String magasinName;
    private Integer magasinId;

    public StorageDTO() {}

    public StorageDTO(Storage storage) {
        if (storage != null) {
            this.name = storage.getName();
            this.id = storage.getId();
            this.storageType = storage.getStorageType().getValue();
            Magasin magasin = storage.getMagasin();
            this.magasinName = magasin.getName();
            this.magasinId = magasin.getId();
        }

    }

    public String getName() {
        return name;
    }

    public StorageDTO setName(String name) {
        this.name = name;
        return this;
    }

    public Integer getId() {
        return id;
    }

    public StorageDTO setId(Integer id) {
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

    public Integer getMagasinId() {
        return magasinId;
    }

    public StorageDTO setMagasinId(Integer magasinId) {
        this.magasinId = magasinId;
        return this;
    }
}
