package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.repository.AssuredCustomerRepository;
import com.kobe.warehouse.repository.ClientTiersPayantRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.AssuredCustomerService;
import com.kobe.warehouse.service.CustomerDataService;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.web.rest.errors.GenericError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AssuredCustomerServiceImpl implements AssuredCustomerService {
    private final Logger log = LoggerFactory.getLogger(AssuredCustomerServiceImpl.class);
    private final AssuredCustomerRepository assuredCustomerRepository;
    private final ClientTiersPayantRepository clientTiersPayantRepository;
    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;
    private final CustomerDataService customerDataService;

    public AssuredCustomerServiceImpl(AssuredCustomerRepository assuredCustomerRepository, ClientTiersPayantRepository clientTiersPayantRepository, ThirdPartySaleLineRepository thirdPartySaleLineRepository, CustomerDataService customerDataService) {
        this.assuredCustomerRepository = assuredCustomerRepository;
        this.clientTiersPayantRepository = clientTiersPayantRepository;
        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
        this.customerDataService = customerDataService;
    }

    @Override
    public AssuredCustomer createFromDto(AssuredCustomerDTO dto) {
        AssuredCustomer assuredCustomer = fromDto(dto);
        clientTiersPayantFromDto(dto.getTiersPayants(), assuredCustomer);
        ayantDroitsFromDto(dto.getAyantDroits(), assuredCustomer);
        return assuredCustomerRepository.save(assuredCustomer);

    }

    @Override
    public AssuredCustomer updateFromDto(AssuredCustomerDTO dto) {
        AssuredCustomer assuredCustomer = fromDto(dto, assuredCustomerRepository.getReferenceById(dto.getId()));
        List<ClientTiersPayant> clientTiersPayants = clientTiersPayantRepository.findAllByAssuredCustomerId(assuredCustomer.getId());
        ClientTiersPayant t0 = clientTiersPayants.stream().filter(e -> e.getPriorite() == PrioriteTiersPayant.T0).findFirst().get();
        assuredCustomer.getClientTiersPayants().clear();
        dto.getTiersPayants().forEach(clientTiersPayantDto -> {
            Optional<ClientTiersPayant> tiersPayantOptional = clientTiersPayants.stream().filter(c -> clientTiersPayantDto.getId() != null && c.getId() == clientTiersPayantDto.getId()).findFirst();
            if (tiersPayantOptional.isPresent()) {
                upadateClientTiersPayant(clientTiersPayantDto, tiersPayantOptional.get(), assuredCustomer);
            } else {
                ClientTiersPayant clientTiersPayant = getClientTiersPayantFromDto(clientTiersPayantDto);
                clientTiersPayant.setAssuredCustomer(assuredCustomer);
                assuredCustomer.getClientTiersPayants().add(clientTiersPayant);
            }
        });
        assuredCustomer.getClientTiersPayants().add(getClientTiersPayantFromDto(dto, t0));
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
        canModifyTiersPayant(o);
        log.info("cccc  ===============>>>{0}", c);
        o = updateClientTiersPayantFromDto(c, o, assuredCustomer);
        log.info("===============>>>{0}", o);
        return o;
    }

    @Override
    @Transactional(readOnly = true)
    public AssuredCustomerDTO mappEntityToDto(AssuredCustomer assuredCustomer) {
        List<ClientTiersPayantDTO> clientTiersPayantDTOS = clientTiersPayantRepository.findAllByAssuredCustomerId(assuredCustomer.getId()).stream().map(ClientTiersPayantDTO::new).collect(Collectors.toList());
        List<AssuredCustomerDTO> ayantDroits = assuredCustomerRepository.findAllByAssurePrincipalId(assuredCustomer.getId()).stream().map(ayantDroit -> new AssuredCustomerDTO(ayantDroit, Collections.emptyList(), Collections.emptyList())).collect(Collectors.toList());
        AssuredCustomerDTO assuredCustomerDTO = new AssuredCustomerDTO(assuredCustomer, clientTiersPayantDTOS, ayantDroits);
        return assuredCustomerDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public AssuredCustomerDTO mappAyantDroitEntityToDto(AssuredCustomer assuredCustomer) {
        AssuredCustomerDTO assuredCustomerDTO = new AssuredCustomerDTO(assuredCustomer, Collections.emptyList(), Collections.emptyList());
        return assuredCustomerDTO;
    }

    @Override
    public void deleteCustomerById(Long id) throws GenericError {
        try {
            AssuredCustomer assuredCustomer = assuredCustomerRepository.getReferenceById(id);
            List<AssuredCustomer> ayantDroits = assuredCustomerRepository.findAllByAssurePrincipalId(id);
            ayantDroits.forEach(ayantDroit -> assuredCustomerRepository.deleteById(ayantDroit.getId()));
            assuredCustomerRepository.delete(assuredCustomer);
        } catch (Exception e) {
            log.debug("{}", e);
            throw new GenericError("deleteCustomer", "Impossible de supprimer ce client, Il existe des ventes qui lui sont ratachées ", "deleteCustomer");
        }

    }

    @Override
    public List<AssuredCustomerDTO> fetch(String query, String typeTiersPayant) {

        return customerDataService.loadAllAsuredCustomers(query, typeTiersPayant);
    }

    private boolean canModifyTiersPayant(ClientTiersPayant clientTiersPayant) throws GenericError {
        long countSales = thirdPartySaleLineRepository.countByClientTiersPayantId(clientTiersPayant.getId());
        if (countSales > 0)
            throw new GenericError("updateTiersPayantFromClientForm", "Il existe des ventes avec tiers-payant. Veuillez basculer les ventes ", "salesRelatedToCustomer");
        return true;
    }
}
