package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

public class MagasinDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;

    @NotNull
    private String name;

    private TypeMagasin typeMagasin;
    private String typeLibelle;
    private String phone;
    private String address;
    private String note;
    private String registre;
    private String compteContribuable;
    private String numComptable;
    private StorageDTO primaryStorage;
    private StorageDTO pointOfSale;
    private String welcomeMessage;
    private String fullName;
    private String compteBancaire;
    private String registreImposition;
    private String managerFirstName;
    private String email;
    private String managerLastName;

    public MagasinDTO() {}

    public MagasinDTO(Magasin magasin) {
        id = magasin.getId();
        name = magasin.getName();
        phone = magasin.getPhone();
        address = magasin.getAddress();
        note = magasin.getNote();
        registre = magasin.getRegistre();
        System.err.println("MagasinDTO Magasin registre=" + registre);
        compteContribuable = magasin.getCompteContribuable();
        numComptable = magasin.getNumComptable();
        primaryStorage = new StorageDTO(magasin.getPrimaryStorage());
        pointOfSale = new StorageDTO(magasin.getPointOfSale());
        welcomeMessage = magasin.getWelcomeMessage();
        fullName = magasin.getFullName();
        typeMagasin = magasin.getTypeMagasin();
        typeLibelle = typeMagasin.getLibelle();
        registreImposition = magasin.getRegistreImposition();
        compteBancaire = magasin.getCompteBancaire();
        managerFirstName = magasin.getManagerFirstName();
        managerLastName = magasin.getManagerLastName();
        email = magasin.getEmail();
    }

    public TypeMagasin getTypeMagasin() {
        return typeMagasin;
    }

    public MagasinDTO setTypeMagasin(TypeMagasin typeMagasin) {
        this.typeMagasin = typeMagasin;
        return this;
    }

    public String getTypeLibelle() {
        return typeLibelle;
    }

    public MagasinDTO setTypeLibelle(String typeLibelle) {
        this.typeLibelle = typeLibelle;
        return this;
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

    public StorageDTO getPrimaryStorage() {
        return primaryStorage;
    }

    public void setPrimaryStorage(StorageDTO primaryStorage) {
        this.primaryStorage = primaryStorage;
    }

    public StorageDTO getPointOfSale() {
        return pointOfSale;
    }

    public void setPointOfSale(StorageDTO pointOfSale) {
        this.pointOfSale = pointOfSale;
    }

    public String getManagerFirstName() {
        return managerFirstName;
    }

    public MagasinDTO setManagerFirstName(String managerFirstName) {
        this.managerFirstName = managerFirstName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public MagasinDTO setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getManagerLastName() {
        return managerLastName;
    }

    public MagasinDTO setManagerLastName(String managerLastName) {
        this.managerLastName = managerLastName;
        return this;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public MagasinDTO setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
        return this;
    }

    public String getRegistreImposition() {
        return registreImposition;
    }

    public MagasinDTO setRegistreImposition(String registreImposition) {
        this.registreImposition = registreImposition;
        return this;
    }

    public String getCompteBancaire() {
        return compteBancaire;
    }

    public MagasinDTO setCompteBancaire(String compteBancaire) {
        this.compteBancaire = compteBancaire;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
