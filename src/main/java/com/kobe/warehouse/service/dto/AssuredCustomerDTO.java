package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.RemiseClient;


import java.time.LocalDate;

public class AssuredCustomerDTO extends CustomerDTO{
    private RemiseClient remise;
    private String sexe;
    private LocalDate datNaiss;

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
}
