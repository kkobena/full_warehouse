package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;

import java.time.Instant;
import java.util.List;

public interface AssuredCustomerService {
    default AssuredCustomer fromDto(AssuredCustomerDTO dto) {
        AssuredCustomer assuredCustomer = new AssuredCustomer();
        assuredCustomer.setDatNaiss(dto.getDatNaiss());
        assuredCustomer.setSexe(dto.getSexe());
        assuredCustomer.setFirstName(dto.getFirstName());
        assuredCustomer.setLastName(dto.getLastName());
        assuredCustomer.setEmail(dto.getEmail());
        assuredCustomer.setPhone(dto.getPhone());
        assuredCustomer.setCreatedAt(Instant.now());
        assuredCustomer.setUpdatedAt(assuredCustomer.getUpdatedAt());
        assuredCustomer.setStatus(Status.ENABLE);
        return assuredCustomer;
    }

    default void clientTiersPayantFromDto(List<ClientTiersPayantDTO> dtos, AssuredCustomer assuredCustomer) {
        dtos.forEach(c -> {
            ClientTiersPayant o = new ClientTiersPayant();
            o.setCreated(Instant.now());
            o.setTiersPayant(new TiersPayant().setId(c.getTiersPayantId()));
            o.setNum(c.getNum());
            o.setPlafondConso(c.getPlafondConso());
            o.setPlafondJournalier(c.getPlafondJournalier());
            o.setPriorite(c.getPriorite());
            o.setTaux(c.getTaux());
            o.setStatut(TiersPayantStatut.ACTIF);
            o.setUpdated(o.getCreated());
            o.setAssuredCustomer(assuredCustomer);
            assuredCustomer.getClientTiersPayants().add(o);

        });
    }

    default void ayantDroitsFromDto(List<AssuredCustomerDTO> dtos, AssuredCustomer assuredCustomer) {
        dtos.forEach(tp -> {
            AssuredCustomer ayantDroit = fromDto(tp);
            ayantDroit.setAssurePrincipal(assuredCustomer);
            ayantDroit.setNumAyantDroit(tp.getNumAyantDroit());
            assuredCustomer.getAyantDroits().add(ayantDroit);

        });
    }

    default AssuredCustomer fromDto(AssuredCustomerDTO dto, AssuredCustomer assuredCustomer) {
        assuredCustomer.setDatNaiss(dto.getDatNaiss());
        assuredCustomer.setSexe(dto.getSexe());
        assuredCustomer.setFirstName(dto.getFirstName());
        assuredCustomer.setLastName(dto.getLastName());
        assuredCustomer.setEmail(dto.getEmail());
        assuredCustomer.setPhone(dto.getPhone());
        assuredCustomer.setUpdatedAt(Instant.now());
        return assuredCustomer;
    }

    void createFromDto(AssuredCustomerDTO dto);

    void updateFromDto(AssuredCustomerDTO dto);

    void delete(Long id);

    void desable(Long id);

    void createAyantDroitFromDto(AssuredCustomerDTO dto);

    void updateAyantDroitFromDto(AssuredCustomerDTO dto);

}
