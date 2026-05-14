package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Fournisseur;
import java.io.Serializable;

public class FournisseurDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    private Integer id;

    private String libelle;
    private String addresspostale;

    private String numFaxe;

    private String addressePostal;

    private String phone;

    private String mobile;

    private String site;
    private String code;
    private String email;
    private Integer odre = 100;

    private Integer parentId;
    private String parentLibelle;

    private Integer delaiLivraisonJours;
    private Integer frequenceCommandeJours;
    private String identifiantRepartiteur;
    private Integer joursCredit;
    private Integer joursCritique;
    private Long palierRfa;
    private Integer tauxRfa;
    private String urlPharmaMl;
    private String codeOfficePharmaMl;
    private String codeRecepteurPharmaMl;
    private String idRecepteurPharmaMl;

    public FournisseurDTO() {}

    public FournisseurDTO(Fournisseur fournisseur) {
        id = fournisseur.getId();
        libelle = fournisseur.getLibelle();
        addresspostale = fournisseur.getAddressePostal();
        numFaxe = fournisseur.getNumFaxe();
        addressePostal = fournisseur.getAddressePostal();
        phone = fournisseur.getPhone();
        mobile = fournisseur.getMobile();
        site = fournisseur.getSite();
        code = fournisseur.getCode();
        email = fournisseur.getEmail();
        odre = fournisseur.getOdre();
        identifiantRepartiteur = fournisseur.getIdentifiantRepartiteur();
        delaiLivraisonJours = fournisseur.getDelaiLivraisonJours();
        frequenceCommandeJours = fournisseur.getFrequenceCommandeJours();
        joursCredit = fournisseur.getJoursCredit();
        joursCritique = fournisseur.getJoursCritique();
        palierRfa = fournisseur.getPalierRfa();
        tauxRfa = fournisseur.getTauxRfa();
        urlPharmaMl = fournisseur.getUrlPharmaMl();
        codeOfficePharmaMl = fournisseur.getCodeOfficePharmaMl();
        codeRecepteurPharmaMl = fournisseur.getCodeRecepteurPharmaMl();
        idRecepteurPharmaMl = fournisseur.getIdRecepteurPharmaMl();
        Fournisseur parent = fournisseur.getParent();
        if (parent != null) {
            parentId = parent.getId();
            parentLibelle = parent.getLibelle();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getAddresspostale() {
        return addresspostale;
    }

    public void setAddresspostale(String addresspostale) {
        this.addresspostale = addresspostale;
    }

    public String getNumFaxe() {
        return numFaxe;
    }

    public void setNumFaxe(String numFaxe) {
        this.numFaxe = numFaxe;
    }

    public String getAddressePostal() {
        return addressePostal;
    }

    public void setAddressePostal(String addressePostal) {
        this.addressePostal = addressePostal;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getDelaiLivraisonJours() {
        return delaiLivraisonJours;
    }

    public void setDelaiLivraisonJours(Integer delaiLivraisonJours) {
        this.delaiLivraisonJours = delaiLivraisonJours;
    }

    public Integer getFrequenceCommandeJours() {
        return frequenceCommandeJours;
    }

    public void setFrequenceCommandeJours(Integer frequenceCommandeJours) {
        this.frequenceCommandeJours = frequenceCommandeJours;
    }

    public String getIdentifiantRepartiteur() {
        return identifiantRepartiteur;
    }

    public void setIdentifiantRepartiteur(String identifiantRepartiteur) {
        this.identifiantRepartiteur = identifiantRepartiteur;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public String getParentLibelle() {
        return parentLibelle;
    }

    public void setParentLibelle(String parentLibelle) {
        this.parentLibelle = parentLibelle;
    }

    public Integer getOdre() {
        return odre;
    }

    public void setOdre(Integer odre) {
        this.odre = odre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getJoursCredit() { return joursCredit; }
    public void setJoursCredit(Integer joursCredit) { this.joursCredit = joursCredit; }

    public Integer getJoursCritique() { return joursCritique; }
    public void setJoursCritique(Integer joursCritique) { this.joursCritique = joursCritique; }

    public Long getPalierRfa() { return palierRfa; }
    public void setPalierRfa(Long palierRfa) { this.palierRfa = palierRfa; }

    public Integer getTauxRfa() { return tauxRfa; }
    public void setTauxRfa(Integer tauxRfa) { this.tauxRfa = tauxRfa; }

    public String getUrlPharmaMl() { return urlPharmaMl; }
    public void setUrlPharmaMl(String urlPharmaMl) { this.urlPharmaMl = urlPharmaMl; }

    public String getCodeOfficePharmaMl() { return codeOfficePharmaMl; }
    public void setCodeOfficePharmaMl(String codeOfficePharmaMl) { this.codeOfficePharmaMl = codeOfficePharmaMl; }

    public String getCodeRecepteurPharmaMl() { return codeRecepteurPharmaMl; }
    public void setCodeRecepteurPharmaMl(String codeRecepteurPharmaMl) { this.codeRecepteurPharmaMl = codeRecepteurPharmaMl; }

    public String getIdRecepteurPharmaMl() { return idRecepteurPharmaMl; }
    public void setIdRecepteurPharmaMl(String idRecepteurPharmaMl) { this.idRecepteurPharmaMl = idRecepteurPharmaMl; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FournisseurDTO)) {
            return false;
        }

        return id != null && id.equals(((FournisseurDTO) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
