package com.kobe.warehouse.service.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.kobe.warehouse.domain.UninsuredCustomer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

public class UninsuredCustomerDTO extends CustomerDTO {

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @PastOrPresent
    private LocalDate datNaiss;

    private String sexe;

    public UninsuredCustomerDTO() {}

    public UninsuredCustomerDTO(UninsuredCustomer customer) {
        super(customer);
        this.datNaiss = customer.getDatNaiss();
        this.sexe = customer.getSexe();
    }

    public LocalDate getDatNaiss() {
        return datNaiss;
    }

    public UninsuredCustomerDTO setDatNaiss(LocalDate datNaiss) {
        this.datNaiss = datNaiss;
        return this;
    }

    public String getSexe() {
        return sexe;
    }

    public UninsuredCustomerDTO setSexe(String sexe) {
        this.sexe = sexe;
        return this;
    }

    /** Format vérifié à la création d'un client comptant ; une adresse vide reste permise (champ facultatif). */
    @Override
    @Email
    public String getEmail() {
        return super.getEmail();
    }
}
