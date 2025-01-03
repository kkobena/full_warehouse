package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.TiersPayant;
import java.io.Serializable;

public class OrganismeDTO implements Serializable {

    private Long id;
    private String name;
    private String fullName;
    private String codeOrganisme;
    private String adresse;
    private String telephone;
    private String telephoneFixe;

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getCodeOrganisme() {
        return codeOrganisme;
    }

    public void setCodeOrganisme(String codeOrganisme) {
        this.codeOrganisme = codeOrganisme;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getTelephoneFixe() {
        return telephoneFixe;
    }

    public void setTelephoneFixe(String telephoneFixe) {
        this.telephoneFixe = telephoneFixe;
    }

    public OrganismeDTO(TiersPayant tiersPayant) {
        this.adresse = tiersPayant.getAdresse();
        this.codeOrganisme = tiersPayant.getCodeOrganisme();
        this.fullName = tiersPayant.getFullName();
        this.id = tiersPayant.getId();
        this.name = tiersPayant.getFullName();
        this.telephone = tiersPayant.getTelephone();
        this.telephoneFixe = tiersPayant.getTelephoneFixe();
    }

    public OrganismeDTO(GroupeTiersPayant tiersPayant) {
        this.adresse = tiersPayant.getAdresse();
        this.id = tiersPayant.getId();
        this.name = tiersPayant.getName();
        this.telephone = tiersPayant.getTelephone();
        this.telephoneFixe = tiersPayant.getTelephoneFixe();
    }
}
