package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.service.InventaireService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

public class MagasinDTO implements Serializable {
    private final Logger LOG = LoggerFactory.getLogger(MagasinDTO.class);
    private static final long serialVersionUID = 1L;
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

    public MagasinDTO() {
    }

    public MagasinDTO(Magasin magasin) {
        this.id = magasin.getId();
        this.name = magasin.getName();
        this.phone = magasin.getPhone();
        this.address = magasin.getAddress();
        this.note = magasin.getNote();
        this.registre = magasin.getRegistre();
        this.compteContribuable = magasin.getCompteContribuable();
        this.numComptable = magasin.getNumComptable();
        LOG.info("=== {}{}",new Object[]{ magasin.getPrimaryStorage(),magasin.getPointOfSale()});
        this.primaryStorage = magasin.getPrimaryStorage();
        this.pointOfSale = magasin.getPointOfSale();
    }
}
