package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.RemiseClient;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AssuredCustomerDTO extends CustomerDTO {
    private RemiseClient remise;
    private String sexe;
    private LocalDate datNaiss;
    private String num;
    private Long assureId;
    private String numAyantDroit;
    private List<AssuredCustomerDTO> ayantDroits = new ArrayList<>();
    private List<ClientTiersPayantDTO> tiersPayants = new ArrayList<>();

    public String getNum() {
        this.tiersPayants.stream().filter(t->t.getPriorite()== PrioriteTiersPayant.T0).findFirst().ifPresent(tp->num=tp.getNum());
        return num;
    }

    public String getNumAyantDroit() {
        return numAyantDroit;
    }

    public AssuredCustomerDTO setNumAyantDroit(String numAyantDroit) {
        this.numAyantDroit = numAyantDroit;
        return this;
    }

    public AssuredCustomerDTO setNum(String num) {
        this.num = num;
        return this;
    }

    public List<AssuredCustomerDTO> getAyantDroits() {
        return ayantDroits;
    }

    public AssuredCustomerDTO setAyantDroits(List<AssuredCustomerDTO> ayantDroits) {
        this.ayantDroits = ayantDroits;
        return this;
    }

    public List<ClientTiersPayantDTO> getTiersPayants() {
        return tiersPayants;
    }

    public AssuredCustomerDTO setTiersPayants(List<ClientTiersPayantDTO> tiersPayants) {
        this.tiersPayants = tiersPayants;
        return this;
    }

    public Long getAssureId() {
        return assureId;
    }

    public AssuredCustomerDTO setAssureId(Long assureId) {
        this.assureId = assureId;
        return this;
    }

    public RemiseClient getRemise() {
        return remise;
    }

    public AssuredCustomerDTO setRemise(RemiseClient remise) {
        this.remise = remise;
        return this;
    }

    public String getSexe() {
        return sexe;
    }

    public AssuredCustomerDTO setSexe(String sexe) {
        this.sexe = sexe;
        return this;
    }

    public LocalDate getDatNaiss() {
        return datNaiss;
    }

    public AssuredCustomerDTO setDatNaiss(LocalDate datNaiss) {
        this.datNaiss = datNaiss;
        return this;
    }

    public AssuredCustomerDTO(AssuredCustomer customer) {
        super(customer);
        this.remise = customer.getRemise();
        this.sexe = customer.getSexe();
    }
}
