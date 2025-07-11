package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;
import com.kobe.warehouse.domain.enumeration.TypeAssure;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.errors.InvalidPhoneNumberException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AssuredCustomerService {
    default AssuredCustomer fromDto(AssuredCustomerDTO dto) {
        AssuredCustomer assuredCustomer = new AssuredCustomer();
        assuredCustomer.setDatNaiss(dto.getDatNaiss());
        assuredCustomer.setSexe(dto.getSexe());
        assuredCustomer.setFirstName(dto.getFirstName().trim());
        assuredCustomer.setLastName(dto.getLastName().trim());
        assuredCustomer.setEmail(dto.getEmail());
        assuredCustomer.setPhone(dto.getPhone());
        assuredCustomer.setCreatedAt(LocalDateTime.now());
        assuredCustomer.setUpdatedAt(assuredCustomer.getUpdatedAt());
        assuredCustomer.setStatus(Status.ENABLE);
        assuredCustomer.setTypeAssure(TypeAssure.PRINCIPAL);
        assuredCustomer.setNumAyantDroit(Objects.nonNull(dto.getNumAyantDroit()) ? dto.getNumAyantDroit().trim() : null);
        assuredCustomer.setCode(RandomStringUtils.randomNumeric(6));
        ClientTiersPayant o = getClientTiersPayantFromDto(dto);
        o.setAssuredCustomer(assuredCustomer);
        assuredCustomer.getClientTiersPayants().add(o);
        return assuredCustomer;
    }

    default void clientTiersPayantFromDto(List<ClientTiersPayantDTO> dtos, AssuredCustomer assuredCustomer) {
        if (dtos != null) {
            dtos.forEach(c -> {
                ClientTiersPayant o = this.getClientTiersPayantFromDto(c);
                o.setAssuredCustomer(assuredCustomer);
                assuredCustomer.getClientTiersPayants().add(o);
            });
        }
    }

    default ClientTiersPayant getClientTiersPayantFromDto(AssuredCustomerDTO dto) {
        ClientTiersPayant o = new ClientTiersPayant();
        o.setCreated(LocalDateTime.now());
        o.setUpdated(o.getCreated());
        o.setTiersPayant(new TiersPayant().setId(dto.getTiersPayantId()));
        o.setNum(Objects.nonNull(dto.getNum()) ? dto.getNum().trim() : null);
        o.setPriorite(PrioriteTiersPayant.R0);
        o.setTaux(dto.getTaux());
        o.setStatut(TiersPayantStatut.ACTIF);
        return o;
    }

    default ClientTiersPayant getClientTiersPayantFromDto(AssuredCustomerDTO dto, ClientTiersPayant o) {
        o.setUpdated(LocalDateTime.now());
        o.setTiersPayant(new TiersPayant().setId(dto.getTiersPayantId()));
        o.setNum(Objects.nonNull(dto.getNum()) ? dto.getNum().trim() : null);
        o.setTaux(dto.getTaux());
        return o;
    }

    default ClientTiersPayant getClientTiersPayantFromDto(ClientTiersPayantDTO dto) {
        ClientTiersPayant o = new ClientTiersPayant();
        o.setId(dto.getId());
        o.setCreated(LocalDateTime.now());
        o.setUpdated(o.getCreated());
        o.setTiersPayant(new TiersPayant().setId(dto.getTiersPayantId()));
        o.setNum(Objects.nonNull(dto.getNum()) ? dto.getNum().trim() : null);
        o.setPriorite(dto.getPriorite());
        o.setTaux(dto.getTaux());
        o.setStatut(TiersPayantStatut.ACTIF);
        return o;
    }

    default void ayantDroitsFromDto(List<AssuredCustomerDTO> dtos, AssuredCustomer assuredCustomer) {
        dtos.forEach(tp -> {
            AssuredCustomer ayantDroit = buildAyantDroitfromDto(tp);
            ayantDroit.setTypeAssure(TypeAssure.AYANT_DROIT);
            ayantDroit.setAssurePrincipal(assuredCustomer);
            ayantDroit.setNumAyantDroit(tp.getNumAyantDroit());
            assuredCustomer.getAyantDroits().add(ayantDroit);
        });
    }

    default AssuredCustomer fromDto(AssuredCustomerDTO dto, AssuredCustomer assuredCustomer) {
        assuredCustomer.setDatNaiss(dto.getDatNaiss());
        assuredCustomer.setSexe(dto.getSexe());
        assuredCustomer.setFirstName(dto.getFirstName().trim());
        assuredCustomer.setLastName(dto.getLastName().trim());
        assuredCustomer.setEmail(dto.getEmail());
        assuredCustomer.setPhone(dto.getPhone());
        assuredCustomer.setUpdatedAt(LocalDateTime.now());
        assuredCustomer.setNumAyantDroit(dto.getNumAyantDroit());
        return assuredCustomer;
    }

    AssuredCustomer createFromDto(AssuredCustomerDTO dto) throws InvalidPhoneNumberException;

    AssuredCustomer updateFromDto(AssuredCustomerDTO dto) throws InvalidPhoneNumberException;

    void delete(Long id);

    void desable(Long id);

    AssuredCustomer createAyantDroitFromDto(AssuredCustomerDTO dto);

    AssuredCustomer updateAyantDroitFromDto(AssuredCustomerDTO dto);

    default AssuredCustomer fromExternalDto(AssuredCustomerDTO dto) {
        AssuredCustomer assuredCustomer = new AssuredCustomer();
        assuredCustomer.setDatNaiss(dto.getDatNaiss());
        assuredCustomer.setSexe(StringUtils.isNotEmpty(dto.getSexe()) ? dto.getSexe() : null);
        assuredCustomer.setFirstName(dto.getFirstName());
        assuredCustomer.setLastName(dto.getLastName());
        assuredCustomer.setEmail(StringUtils.isNotEmpty(dto.getEmail()) ? dto.getEmail() : null);
        assuredCustomer.setPhone(dto.getPhone());
        assuredCustomer.setCreatedAt(LocalDateTime.now());
        assuredCustomer.setUpdatedAt(assuredCustomer.getUpdatedAt());
        assuredCustomer.setStatus(Status.ENABLE);
        assuredCustomer.setTypeAssure(TypeAssure.PRINCIPAL);
        return assuredCustomer;
    }

    default void clientTiersPayantExternalFromDto(
        final List<ClientTiersPayantDTO> dtos,
        final TiersPayant tiersPayant,
        final AssuredCustomer assuredCustomer
    ) {
        dtos.forEach(c -> {
            ClientTiersPayant o = new ClientTiersPayant();
            o.setCreated(LocalDateTime.now());
            o.setTiersPayant(tiersPayant);
            o.setNum(c.getNum());
            o.setPriorite(c.getPriorite());
            o.setTaux(c.getTaux());
            o.setStatut(TiersPayantStatut.ACTIF);
            o.setUpdated(o.getCreated());
            o.setAssuredCustomer(assuredCustomer);
            assuredCustomer.getClientTiersPayants().add(o);
        });
    }

    default ClientTiersPayant updateClientTiersPayantFromDto(ClientTiersPayantDTO c, ClientTiersPayant o, AssuredCustomer assuredCustomer) {
        o.setUpdated(LocalDateTime.now());
        o.setNum(c.getNum());
        o.setPriorite(c.getPriorite());
        o.setTaux(c.getTaux());
        o.setAssuredCustomer(assuredCustomer);
        o.setTiersPayant(new TiersPayant().setId(c.getTiersPayantId()));
        assuredCustomer.getClientTiersPayants().add(o);
        return o;
    }

    default AssuredCustomer buildAyantDroitfromDto(AssuredCustomerDTO dto) {
        AssuredCustomer assuredCustomer = new AssuredCustomer();
        assuredCustomer.setDatNaiss(dto.getDatNaiss());
        assuredCustomer.setSexe(dto.getSexe());
        assuredCustomer.setFirstName(dto.getFirstName().trim());
        assuredCustomer.setLastName(dto.getLastName().trim());
        assuredCustomer.setEmail(dto.getEmail());
        assuredCustomer.setPhone(dto.getPhone());
        assuredCustomer.setCreatedAt(LocalDateTime.now());
        assuredCustomer.setUpdatedAt(assuredCustomer.getUpdatedAt());
        assuredCustomer.setStatus(Status.ENABLE);
        assuredCustomer.setTypeAssure(TypeAssure.AYANT_DROIT);
        assuredCustomer.setNumAyantDroit(dto.getNumAyantDroit());
        assuredCustomer.setCode(RandomStringUtils.randomNumeric(6));
        assuredCustomer.setAssurePrincipal(buildAssureFromId(dto.getAssureId()));
        return assuredCustomer;
    }

    default AssuredCustomer buildAssureFromId(Long id) {
        AssuredCustomer assuredCustomer = new AssuredCustomer();
        assuredCustomer.setId(id);
        return assuredCustomer;
    }

    ClientTiersPayant upadateClientTiersPayant(ClientTiersPayantDTO c, ClientTiersPayant o, AssuredCustomer assuredCustomer);

    AssuredCustomerDTO mappEntityToDto(AssuredCustomer assuredCustomer);

    AssuredCustomerDTO mappAyantDroitEntityToDto(AssuredCustomer assuredCustomer);

    Page<AssuredCustomerDTO> fetch(String query, TiersPayantCategorie typeTiersPayant, Pageable pageable);

    void deleteCustomerById(Long id) throws GenericError;

    AssuredCustomer addTiersPayant(ClientTiersPayantDTO dto) throws GenericError;

    AssuredCustomer updateTiersPayant(ClientTiersPayantDTO dto) throws GenericError;

    void deleteTiersPayant(Long id) throws GenericError;
}
