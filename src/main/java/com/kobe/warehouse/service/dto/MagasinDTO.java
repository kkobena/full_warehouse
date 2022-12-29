package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

public class MagasinDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Logger LOG = LoggerFactory.getLogger(MagasinDTO.class);
    private Long id;
    @NotNull
    private String name;
    private String phone;
    private String address;
    private String note;
    private String registre;
    private String compteContribuable;
    private String numComptable;
    private Storage primaryStorage;
    private Storage pointOfSale;
    private String welcomeMessage;

    public MagasinDTO() {
    }

    public MagasinDTO(Magasin magasin) {
        id = magasin.getId();
        name = magasin.getName();
        phone = magasin.getPhone();
        address = magasin.getAddress();
        note = magasin.getNote();
        registre = magasin.getRegistre();
        compteContribuable = magasin.getCompteContribuable();
        numComptable = magasin.getNumComptable();
        LOG.info("=== {}{}", new Object[]{magasin.getPrimaryStorage(), magasin.getPointOfSale()});
        primaryStorage = magasin.getPrimaryStorage();
        pointOfSale = magasin.getPointOfSale();
        welcomeMessage = magasin.getWelcomeMessage();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getRegistre() {
        return registre;
    }

    public void setRegistre(String registre) {
        this.registre = registre;
    }

    public String getCompteContribuable() {
        return compteContribuable;
    }

    public void setCompteContribuable(String compteContribuable) {
        this.compteContribuable = compteContribuable;
    }

    public String getNumComptable() {
        return numComptable;
    }

    public void setNumComptable(String numComptable) {
        this.numComptable = numComptable;
    }

    public Storage getPrimaryStorage() {
        return primaryStorage;
    }

    public void setPrimaryStorage(Storage primaryStorage) {
        this.primaryStorage = primaryStorage;
    }

    public Storage getPointOfSale() {
        return pointOfSale;
    }

    public void setPointOfSale(Storage pointOfSale) {
        this.pointOfSale = pointOfSale;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public MagasinDTO setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
        return this;
    }
}
