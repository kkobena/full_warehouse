package com.kobe.warehouse.service.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RetourDepotDTO {

    private Integer id;
    private LocalDateTime dateMtv;
    private Long userId;
    private String userFullName;
    private Long venteDepotId;
    private String venteDepotDate;
    @NotNull
    private Integer depotId;
    private String depotName;
    private List<RetourDepotItemDTO> retourDepotItems = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public RetourDepotDTO setId(Integer id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getDateMtv() {
        return dateMtv;
    }

    public RetourDepotDTO setDateMtv(LocalDateTime dateMtv) {
        this.dateMtv = dateMtv;
        return this;
    }

    public Long getUserId() {
        return userId;
    }

    public RetourDepotDTO setUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public RetourDepotDTO setUserFullName(String userFullName) {
        this.userFullName = userFullName;
        return this;
    }


    public Long getVenteDepotId() {
        return venteDepotId;
    }

    public RetourDepotDTO setVenteDepotId(Long venteDepotId) {
        this.venteDepotId = venteDepotId;
        return this;
    }

    public String getVenteDepotDate() {
        return venteDepotDate;
    }

    public RetourDepotDTO setVenteDepotDate(String venteDepotDate) {
        this.venteDepotDate = venteDepotDate;
        return this;
    }

    public Integer getDepotId() {
        return depotId;
    }

    public RetourDepotDTO setDepotId(Integer depotId) {
        this.depotId = depotId;
        return this;
    }

    public String getDepotName() {
        return depotName;
    }

    public RetourDepotDTO setDepotName(String depotName) {
        this.depotName = depotName;
        return this;
    }

    public List<RetourDepotItemDTO> getRetourDepotItems() {
        return retourDepotItems;
    }

    public RetourDepotDTO setRetourDepotItems(List<RetourDepotItemDTO> retourDepotItems) {
        this.retourDepotItems = retourDepotItems;
        return this;
    }
}
