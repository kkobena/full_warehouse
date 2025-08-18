package com.kobe.warehouse.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
public class AssuredCustomer extends Customer {

    @ManyToOne
    private RemiseClient remise;

    @Column(name = "sexe")
    private String sexe;

    @Column(name = "dat_naiss")
    private LocalDate datNaiss;

    @ManyToOne
    @JoinColumn(name = "assure_principal_id", referencedColumnName = "id")
    private AssuredCustomer assurePrincipal;

    @Column(name = "num_ayant_droit", length = 100)
    private String numAyantDroit;

    @OneToMany(mappedBy = "assurePrincipal", orphanRemoval = true, cascade = { CascadeType.REMOVE, CascadeType.PERSIST })
    private Set<AssuredCustomer> ayantDroits = new HashSet<>();

    @OneToMany(mappedBy = "assuredCustomer", orphanRemoval = true, cascade = { CascadeType.REMOVE, CascadeType.PERSIST })
    private Set<ClientTiersPayant> clientTiersPayants = new HashSet<>();

    public AssuredCustomer getAssurePrincipal() {
        return assurePrincipal;
    }

    public AssuredCustomer setAssurePrincipal(AssuredCustomer assurePrincipal) {
        this.assurePrincipal = assurePrincipal;
        return this;
    }

    public Set<AssuredCustomer> getAyantDroits() {
        return ayantDroits;
    }

    public AssuredCustomer setAyantDroits(Set<AssuredCustomer> ayantDroits) {
        this.ayantDroits = ayantDroits;
        return this;
    }

    public Set<ClientTiersPayant> getClientTiersPayants() {
        return clientTiersPayants;
    }

    public AssuredCustomer setClientTiersPayants(Set<ClientTiersPayant> clientTiersPayants) {
        this.clientTiersPayants = clientTiersPayants;
        return this;
    }

    public String getNumAyantDroit() {
        return numAyantDroit;
    }

    public AssuredCustomer setNumAyantDroit(String numAyantDroit) {
        this.numAyantDroit = numAyantDroit;
        return this;
    }

    public RemiseClient getRemise() {
        return remise;
    }

    public void setRemise(RemiseClient remise) {
        this.remise = remise;
    }

    public String getSexe() {
        return sexe;
    }

    public void setSexe(String sexe) {
        this.sexe = sexe;
    }

    public LocalDate getDatNaiss() {
        return datNaiss;
    }

    public void setDatNaiss(LocalDate datNaiss) {
        this.datNaiss = datNaiss;
    }
}
