package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class TiersPayantDto implements Serializable {
    private  Long id;
    private  String name;
    private  String fullName;
    private  Integer nbreBons;
    private  Long montantMaxParFcture;
    private  Integer nbreFacture;
    private  String codeOrganisme;
    private  String codeRegroupement;
    private  Long consoMensuelle;
    private  Boolean plafondAbsolu;
    private  String adresse;
    private  String telephone;
    private  String telephoneFixe;
    private  String email;
    private  Boolean toBeExclude;
    private  Long plafondConso;
    private  Long plafondClient;
    private  TiersPayantStatut statut;
    private  TiersPayantCategorie categorie;
    private  Long remiseForfaitaire;
    private  Integer nbreBordereaux;
    private  Instant created;
    private  Instant updated;
    private GroupeTiersPayant groupeTiersPayant;

    public GroupeTiersPayant getGroupeTiersPayant() {
        return groupeTiersPayant;
    }

    public TiersPayantDto setGroupeTiersPayant(GroupeTiersPayant groupeTiersPayant) {
        this.groupeTiersPayant = groupeTiersPayant;
        return this;
    }

    public TiersPayantDto() {

    }

    public TiersPayantDto setId(Long id) {
        this.id = id;
        return this;
    }

    public TiersPayantDto setName(String name) {
        this.name = name;
        return this;
    }

    public TiersPayantDto setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public TiersPayantDto setNbreBons(Integer nbreBons) {
        this.nbreBons = nbreBons;
        return this;
    }

    public TiersPayantDto setMontantMaxParFcture(Long montantMaxParFcture) {
        this.montantMaxParFcture = montantMaxParFcture;
        return this;
    }

    public TiersPayantDto setNbreFacture(Integer nbreFacture) {
        this.nbreFacture = nbreFacture;
        return this;
    }

    public TiersPayantDto setCodeOrganisme(String codeOrganisme) {
        this.codeOrganisme = codeOrganisme;
        return this;
    }

    public TiersPayantDto setCodeRegroupement(String codeRegroupement) {
        this.codeRegroupement = codeRegroupement;
        return this;
    }

    public TiersPayantDto setConsoMensuelle(Long consoMensuelle) {
        this.consoMensuelle = consoMensuelle;
        return this;
    }

    public TiersPayantDto setPlafondAbsolu(Boolean plafondAbsolu) {
        this.plafondAbsolu = plafondAbsolu;
        return this;
    }

    public TiersPayantDto setAdresse(String adresse) {
        this.adresse = adresse;
        return this;
    }

    public TiersPayantDto setTelephone(String telephone) {
        this.telephone = telephone;
        return this;
    }

    public TiersPayantDto setTelephoneFixe(String telephoneFixe) {
        this.telephoneFixe = telephoneFixe;
        return this;
    }

    public TiersPayantDto setEmail(String email) {
        this.email = email;
        return this;
    }

    public TiersPayantDto setToBeExclude(Boolean toBeExclude) {
        this.toBeExclude = toBeExclude;
        return this;
    }

    public TiersPayantDto setPlafondConso(Long plafondConso) {
        this.plafondConso = plafondConso;
        return this;
    }

    public TiersPayantDto setPlafondClient(Long plafondClient) {
        this.plafondClient = plafondClient;
        return this;
    }

    public TiersPayantDto setStatut(TiersPayantStatut statut) {
        this.statut = statut;
        return this;
    }

    public TiersPayantDto setCategorie(TiersPayantCategorie categorie) {
        this.categorie = categorie;
        return this;
    }

    public TiersPayantDto setRemiseForfaitaire(Long remiseForfaitaire) {
        this.remiseForfaitaire = remiseForfaitaire;
        return this;
    }

    public TiersPayantDto setNbreBordereaux(Integer nbreBordereaux) {
        this.nbreBordereaux = nbreBordereaux;
        return this;
    }

    public TiersPayantDto setCreated(Instant created) {
        this.created = created;
        return this;
    }

    public TiersPayantDto setUpdated(Instant updated) {
        this.updated = updated;
        return this;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public Integer getNbreBons() {
        return nbreBons;
    }

    public Long getMontantMaxParFcture() {
        return montantMaxParFcture;
    }

    public Integer getNbreFacture() {
        return nbreFacture;
    }

    public String getCodeOrganisme() {
        return codeOrganisme;
    }

    public String getCodeRegroupement() {
        return codeRegroupement;
    }

    public Long getConsoMensuelle() {
        return consoMensuelle;
    }

    public Boolean getPlafondAbsolu() {
        return plafondAbsolu;
    }

    public String getAdresse() {
        return adresse;
    }

    public String getTelephone() {
        return telephone;
    }

    public String getTelephoneFixe() {
        return telephoneFixe;
    }

    public String getEmail() {
        return email;
    }

    public Boolean getToBeExclude() {
        return toBeExclude;
    }

    public Long getPlafondConso() {
        return plafondConso;
    }

    public Long getPlafondClient() {
        return plafondClient;
    }

    public TiersPayantStatut getStatut() {
        return statut;
    }

    public TiersPayantCategorie getCategorie() {
        return categorie;
    }

    public Long getRemiseForfaitaire() {
        return remiseForfaitaire;
    }

    public Integer getNbreBordereaux() {
        return nbreBordereaux;
    }

    public Instant getCreated() {
        return created;
    }

    public Instant getUpdated() {
        return updated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TiersPayantDto entity = (TiersPayantDto) o;
        return Objects.equals(this.id, entity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" +
            "id = " + id + ", " +
            "name = " + name + ", " +
            "fullName = " + fullName + ", " +
            "nbreBons = " + nbreBons + ", " +
            "montantMaxParFcture = " + montantMaxParFcture + ", " +
            "nbreFacture = " + nbreFacture + ", " +
            "codeOrganisme = " + codeOrganisme + ", " +
            "codeRegroupement = " + codeRegroupement + ", " +
            "consoMensuelle = " + consoMensuelle + ", " +
            "plafondAbsolu = " + plafondAbsolu + ", " +
            "adresse = " + adresse + ", " +
            "telephone = " + telephone + ", " +
            "telephoneFixe = " + telephoneFixe + ", " +
            "email = " + email + ", " +
            "toBeExclude = " + toBeExclude + ", " +
            "plafondConso = " + plafondConso + ", " +
            "plafondClient = " + plafondClient + ", " +
            "statut = " + statut + ", " +
            "categorie = " + categorie + ", " +
            "remiseForfaitaire = " + remiseForfaitaire + ", " +
            "nbreBordereaux = " + nbreBordereaux + ", " +
            "created = " + created + ", " +
            "updated = " + updated + ")";
    }
}
