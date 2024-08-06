package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.repository.AssuredCustomerRepository;
import com.kobe.warehouse.repository.ClientTiersPayantRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.AssuredCustomerService;
import com.kobe.warehouse.service.CustomerDataService;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.web.rest.errors.GenericError;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional
public class AssuredCustomerServiceImpl implements AssuredCustomerService {

    private final Logger log = LoggerFactory.getLogger(AssuredCustomerServiceImpl.class);
    private final AssuredCustomerRepository assuredCustomerRepository;
    private final ClientTiersPayantRepository clientTiersPayantRepository;
    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;
    private final CustomerDataService customerDataService;

    public AssuredCustomerServiceImpl(
        AssuredCustomerRepository assuredCustomerRepository,
        ClientTiersPayantRepository clientTiersPayantRepository,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        CustomerDataService customerDataService
    ) {
        this.assuredCustomerRepository = assuredCustomerRepository;
        this.clientTiersPayantRepository = clientTiersPayantRepository;
        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
        this.customerDataService = customerDataService;
    }

    @Override
    public AssuredCustomer createFromDto(AssuredCustomerDTO dto) {
        AssuredCustomer assuredCustomer = fromDto(dto);
        ayantDroitsFromDto(dto.getAyantDroits(), assuredCustomer);
        clientTiersPayantFromDto(dto.getTiersPayants(), assuredCustomer);
        return assuredCustomerRepository.save(assuredCustomer);
    }

    @Override
    public AssuredCustomer updateFromDto(AssuredCustomerDTO dto) {
        AssuredCustomer assuredCustomer = fromDto(dto, assuredCustomerRepository.getReferenceById(dto.getId()));
        List<ClientTiersPayant> clientTiersPayants = clientTiersPayantRepository.findAllByAssuredCustomerId(assuredCustomer.getId());
        clientTiersPayants
            .stream()
            .filter(e -> e.getPriorite() == PrioriteTiersPayant.T0)
            .findFirst()
            .ifPresent(t0 -> this.clientTiersPayantRepository.save(getClientTiersPayantFromDto(dto, t0)));
        List<ClientTiersPayant> complementaires = clientTiersPayants
            .stream()
            .filter(e -> e.getPriorite() != PrioriteTiersPayant.T0)
            .toList();
        if (dto.getTiersPayants() != null) {
            dto
                .getTiersPayants()
                .forEach(clientTiersPayantDto -> {
                    Optional<ClientTiersPayant> tiersPayantOptional = complementaires
                        .stream()
                        .filter(c -> clientTiersPayantDto.getId() != null && Objects.equals(c.getId(), clientTiersPayantDto.getId()))
                        .findFirst();
                    if (tiersPayantOptional.isPresent()) {
                        upadateClientTiersPayant(clientTiersPayantDto, tiersPayantOptional.get(), assuredCustomer);
                    } else {
                        ClientTiersPayant clientTiersPayant = getClientTiersPayantFromDto(clientTiersPayantDto);
                        clientTiersPayant.setAssuredCustomer(assuredCustomer);
                        assuredCustomer.getClientTiersPayants().add(clientTiersPayant);
                        this.clientTiersPayantRepository.save(clientTiersPayant);
                    }
                });
        }
        if (CollectionUtils.isEmpty(dto.getTiersPayants()) && !complementaires.isEmpty()) {
            clientTiersPayantRepository.deleteAll(complementaires);
        }
        if (!CollectionUtils.isEmpty(dto.getTiersPayants()) && !complementaires.isEmpty()) {
            complementaires.forEach(clientTiersPayant -> {
                if (dto.getTiersPayants().stream().noneMatch(e -> clientTiersPayant.getId().equals(e.getId()))) {
                    canModifyTiersPayant(clientTiersPayant.getId());
                    clientTiersPayantRepository.delete(clientTiersPayant);
                }
            });
        }

        return assuredCustomerRepository.save(assuredCustomer);
    }

    @Override
    public void delete(Long id) {
        assuredCustomerRepository.deleteById(id);
    }

    @Override
    public void desable(Long id) {
        AssuredCustomer assuredCustomer = assuredCustomerRepository.getReferenceById(id);
        assuredCustomer.setStatus(Status.DISABLE);
        assuredCustomerRepository.save(assuredCustomer);
    }

    @Override
    public AssuredCustomer createAyantDroitFromDto(AssuredCustomerDTO dto) {
        AssuredCustomer ayantDroit = buildAyantDroitfromDto(dto);
        return assuredCustomerRepository.save(ayantDroit);
    }

    @Override
    public AssuredCustomer updateAyantDroitFromDto(AssuredCustomerDTO dto) {
        AssuredCustomer assuredCustomer = fromDto(dto, assuredCustomerRepository.getReferenceById(dto.getId()));
        return assuredCustomerRepository.save(assuredCustomer);
    }

    @Override
    public ClientTiersPayant upadateClientTiersPayant(ClientTiersPayantDTO c, ClientTiersPayant o, AssuredCustomer assuredCustomer) {
        if (!c.getTiersPayantId().equals(o.getTiersPayant().getId())) {
            canModifyTiersPayant(o.getId());
        }
        return this.clientTiersPayantRepository.save(updateClientTiersPayantFromDto(c, o, assuredCustomer));
    }

    @Override
    @Transactional(readOnly = true)
    public AssuredCustomerDTO mappEntityToDto(AssuredCustomer assuredCustomer) {
        List<ClientTiersPayantDTO> clientTiersPayantDTOS = clientTiersPayantRepository
            .findAllByAssuredCustomerId(assuredCustomer.getId())
            .stream()
            .map(ClientTiersPayantDTO::new)
            .toList();
        List<AssuredCustomerDTO> ayantDroits = assuredCustomerRepository
            .findAllByAssurePrincipalId(assuredCustomer.getId())
            .stream()
            .map(ayantDroit -> new AssuredCustomerDTO(ayantDroit, Collections.emptyList(), Collections.emptyList()))
            .toList();
        return new AssuredCustomerDTO(assuredCustomer, clientTiersPayantDTOS, ayantDroits);
    }

    @Override
    @Transactional(readOnly = true)
    public AssuredCustomerDTO mappAyantDroitEntityToDto(AssuredCustomer assuredCustomer) {
        return new AssuredCustomerDTO(assuredCustomer, Collections.emptyList(), Collections.emptyList());
    }

    @Override
    public void deleteCustomerById(Long id) throws GenericError {
        try {
            AssuredCustomer assuredCustomer = assuredCustomerRepository.getReferenceById(id);
            List<AssuredCustomer> ayantDroits = assuredCustomerRepository.findAllByAssurePrincipalId(id);
            ayantDroits.forEach(ayantDroit -> assuredCustomerRepository.deleteById(ayantDroit.getId()));
            assuredCustomerRepository.delete(assuredCustomer);
        } catch (Exception e) {
            throw new GenericError("Impossible de supprimer ce client, Il existe des ventes qui lui sont ratachées ", "deleteCustomer");
        }
    }

    @Override
    public AssuredCustomer addTiersPayant(ClientTiersPayantDTO dto) throws GenericError {
        AssuredCustomer assuredCustomer = assuredCustomerRepository.getReferenceById(dto.getCustomerId());
        ClientTiersPayant clientTiersPayant = getClientTiersPayantFromDto(dto);
        clientTiersPayant.setAssuredCustomer(assuredCustomer);
        assuredCustomer.getClientTiersPayants().add(clientTiersPayant);
        assuredCustomer = assuredCustomerRepository.save(assuredCustomer);
        return assuredCustomer;
    }

    @Override
    public AssuredCustomer updateTiersPayant(ClientTiersPayantDTO dto) throws GenericError {
        ClientTiersPayant clientTiersPayant = clientTiersPayantRepository.getReferenceById(dto.getId());
        clientTiersPayant.setTaux(dto.getTaux());
        clientTiersPayant.setPlafondAbsolu(dto.getPlafondAbsolu());
        clientTiersPayant.setPlafondConso(dto.getPlafondConso());
        clientTiersPayant.setPlafondJournalier(dto.getPlafondJournalier());
        clientTiersPayant.setNum(dto.getNum());
        return clientTiersPayantRepository.save(clientTiersPayant).getAssuredCustomer();
    }

    @Override
    public void deleteTiersPayant(Long id) throws GenericError {
        canModifyTiersPayant(id);
        clientTiersPayantRepository.deleteById(id);
    }

    @Override
    public Page<AssuredCustomerDTO> fetch(String query, TiersPayantCategorie typeTiersPayant, Pageable pageable) {
        return customerDataService.loadAllAsuredCustomers(query, typeTiersPayant, pageable);
    }

    private void canModifyTiersPayant(Long id) throws GenericError {
        long countSales = thirdPartySaleLineRepository.countByClientTiersPayantId(id);
        if (countSales > 0) {
            throw new GenericError("Il existe des ventes avec tiers-payant. Veuillez basculer les ventes ");
        }
    }
}
