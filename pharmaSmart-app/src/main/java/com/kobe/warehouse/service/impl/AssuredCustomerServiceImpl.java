package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.Util;
import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.domain.enumeration.TypeAssure;
import com.kobe.warehouse.repository.AssuredCustomerRepository;
import com.kobe.warehouse.repository.ClientTiersPayantRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.AssuredCustomerService;
import com.kobe.warehouse.service.CustomerDataService;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.ControleAssureDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.errors.InvalidPhoneNumberException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.kobe.warehouse.service.customer.HistoriqueClientService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class AssuredCustomerServiceImpl implements AssuredCustomerService {

    private final AssuredCustomerRepository assuredCustomerRepository;
    private final ClientTiersPayantRepository clientTiersPayantRepository;
    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;
    private final CustomerDataService customerDataService;
    private final HistoriqueClientService historiqueClientService;

    public AssuredCustomerServiceImpl(
        AssuredCustomerRepository assuredCustomerRepository,
        ClientTiersPayantRepository clientTiersPayantRepository,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        CustomerDataService customerDataService,
        HistoriqueClientService historiqueClientService
    ) {
        this.historiqueClientService = historiqueClientService;
        this.assuredCustomerRepository = assuredCustomerRepository;
        this.clientTiersPayantRepository = clientTiersPayantRepository;
        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
        this.customerDataService = customerDataService;
    }

    @Override
    public AssuredCustomer createFromDto(AssuredCustomerDTO dto) throws InvalidPhoneNumberException {
        if (StringUtils.hasText(dto.getPhone()) && !Util.isValidPhoneNumber(dto.getPhone())) {
            throw new InvalidPhoneNumberException();
        }
        verifierNumerosDeCarte(dto, null);
        verifierAbsenceDeDossierIdentique(dto, null);
        AssuredCustomer assuredCustomer = fromDto(dto);
        ayantDroitsFromDto(dto.getAyantDroits(), assuredCustomer);
        clientTiersPayantFromDto(dto.getTiersPayants(), assuredCustomer);
        return assuredCustomerRepository.save(assuredCustomer);
    }

    @Override
    public AssuredCustomer updateFromDto(AssuredCustomerDTO dto) throws InvalidPhoneNumberException {
        if (StringUtils.hasText(dto.getPhone()) && !Util.isValidPhoneNumber(dto.getPhone())) {
            throw new InvalidPhoneNumberException();
        }
        verifierNumerosDeCarte(dto, dto.getId());
        verifierAbsenceDeDossierIdentique(dto, dto.getId());
        AssuredCustomer assuredCustomer = fromDto(dto, assuredCustomerRepository.getReferenceById(dto.getId()));
        List<ClientTiersPayant> clientTiersPayants = clientTiersPayantRepository.findAllByAssuredCustomerId(assuredCustomer.getId());
        clientTiersPayants
            .stream()
            .filter(e -> e.getPriorite() == PrioriteTiersPayant.R0)
            .findFirst()
            .ifPresent(t0 -> this.clientTiersPayantRepository.save(getClientTiersPayantFromDto(dto, t0)));
        List<ClientTiersPayant> complementaires = clientTiersPayants
            .stream()
            .filter(e -> e.getPriorite() != PrioriteTiersPayant.R0)
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
    public void delete(Integer id) {
        assuredCustomerRepository.deleteById(id);
    }

    @Override
    public void desable(Integer id) {
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
    public void deleteCustomerById(Integer id) throws GenericError {
        historiqueClientService.verifierSuppression(id);
        try {
            AssuredCustomer assuredCustomer = assuredCustomerRepository.getReferenceById(id);
            List<AssuredCustomer> ayantDroits = assuredCustomerRepository.findAllByAssurePrincipalId(id);
            ayantDroits.forEach(ayantDroit -> assuredCustomerRepository.deleteById(ayantDroit.getId()));
            assuredCustomerRepository.delete(assuredCustomer);
            // Écrit maintenant, pour que la violation de clé étrangère tombe dans ce catch et non au commit.
            assuredCustomerRepository.flush();
        } catch (Exception e) {
            throw new GenericError("Impossible de supprimer ce client : des données lui sont encore rattachées. Désactivez-le.", "deleteCustomer");
        }
    }

    @Override
    public AssuredCustomer addTiersPayant(ClientTiersPayantDTO dto) throws GenericError {
        verifierNumeroDeCarte(dto.getTiersPayantId(), dto.getNum(), dto.getCustomerId());
        AssuredCustomer assuredCustomer = assuredCustomerRepository.getReferenceById(dto.getCustomerId());
        ClientTiersPayant clientTiersPayant = getClientTiersPayantFromDto(dto);
        clientTiersPayant.setAssuredCustomer(assuredCustomer);
        assuredCustomer.getClientTiersPayants().add(clientTiersPayantRepository.save(clientTiersPayant));
        return assuredCustomer;
    }

    @Override
    public AssuredCustomer updateTiersPayant(ClientTiersPayantDTO dto) throws GenericError {
        ClientTiersPayant clientTiersPayant = clientTiersPayantRepository.getReferenceById(dto.getId());
        verifierNumeroDeCarte(
            clientTiersPayant.getTiersPayant().getId(),
            dto.getNum(),
            clientTiersPayant.getAssuredCustomer().getId()
        );

        clientTiersPayant.setTaux(dto.getTaux());
        clientTiersPayant.setNum(dto.getNum());
        clientTiersPayant.setDateFinValidite(dto.getDateFinValidite());
        if (dto.getPriorite() != clientTiersPayant.getPriorite()) {
            AssuredCustomer customer = clientTiersPayant.getAssuredCustomer();
            Set<ClientTiersPayant> clientTiersPayants = customer
                .getClientTiersPayants()
                .stream()
                .filter(c -> !Objects.equals(dto.getId(), c.getId()))
                .collect(Collectors.toSet());
            ClientTiersPayant old = clientTiersPayants.stream().filter(c -> c.getPriorite() == dto.getPriorite()).findFirst().orElse(null);
            if (old != null) {
                old.setPriorite(clientTiersPayant.getPriorite());
                clientTiersPayantRepository.save(old);
            }
            clientTiersPayant.setPriorite(dto.getPriorite());
        }
        return clientTiersPayantRepository.save(clientTiersPayant).getAssuredCustomer();
    }

    @Override
    public void deleteTiersPayant(Integer id) throws GenericError {
        canModifyTiersPayant(id);
        clientTiersPayantRepository.deleteById(id);
    }

    @Override
    public Page<AssuredCustomerDTO> fetch(String query, TiersPayantCategorie typeTiersPayant, Pageable pageable) {
        return customerDataService.loadAllAsuredCustomers(query, typeTiersPayant, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ControleAssureDTO controlerAssure(Integer tiersPayantId, String num, String firstName, String lastName, Integer idClientExclu) {
        String titulaire = null;
        if (tiersPayantId != null && StringUtils.hasText(num)) {
            titulaire = clientTiersPayantRepository
                .findFirstByTiersPayantIdAndNum(tiersPayantId, num.trim())
                .filter(c -> !Objects.equals(idClientExclu, c.getAssuredCustomer().getId()))
                .map(c -> nomComplet(c.getAssuredCustomer()))
                .orElse(null);
        }
        List<String> homonymes = StringUtils.hasText(firstName) && StringUtils.hasText(lastName)
            ? assuredCustomerRepository
                .findAllByFirstNameIgnoreCaseAndLastNameIgnoreCase(firstName.trim(), lastName.trim())
                .stream()
                .filter(a -> a.getTypeAssure() != TypeAssure.AYANT_DROIT)
                .filter(a -> !Objects.equals(idClientExclu, a.getId()))
                .limit(5)
                .map(this::nomComplet)
                .toList()
            : List.of();
        String dossierIdentique = trouverDossierIdentique(firstName, lastName, num, idClientExclu);
        return new ControleAssureDTO(titulaire != null, titulaire, dossierIdentique != null, dossierIdentique, homonymes);
    }

    /**
     * Refuse un numéro de carte déjà attribué, pour le même organisme, à un autre dossier. La base
     * l'interdit déjà ; ce contrôle remplace l'erreur de contrainte, illisible pour un caissier, par
     * le nom du dossier en cause.
     */
    private void verifierNumerosDeCarte(AssuredCustomerDTO dto, Integer idClientExclu) {
        verifierNumeroDeCarte(dto.getTiersPayantId(), dto.getNum(), idClientExclu);
        if (dto.getTiersPayants() != null) {
            dto.getTiersPayants().forEach(c -> verifierNumeroDeCarte(c.getTiersPayantId(), c.getNum(), idClientExclu));
        }
    }

    /**
     * Un client portant les mêmes nom, prénom ET numéro de matricule qu'un dossier existant est bloqué,
     * quel que soit l'organisme : c'est la même personne ressaisie. Les ayants droit sont soumis à la
     * même règle avec leur numéro assuré. (Deux homonymes de matricules différents restent permis.)
     */
    private void verifierAbsenceDeDossierIdentique(AssuredCustomerDTO dto, Integer idClientExclu) {
        String existant = trouverDossierIdentique(dto.getFirstName(), dto.getLastName(), dto.getNum(), idClientExclu);
        if (existant != null) {
            throw new GenericError(
                "Un client avec ces nom, prénom et numéro de matricule existe déjà (%s)".formatted(existant),
                "clientExistant"
            );
        }
        if (dto.getAyantDroits() != null) {
            dto
                .getAyantDroits()
                .stream()
                .filter(a -> StringUtils.hasText(a.getFirstName()) && StringUtils.hasText(a.getLastName()) && StringUtils.hasText(a.getNumAyantDroit()))
                .forEach(a ->
                    assuredCustomerRepository
                        .findAllByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndNumAyantDroit(a.getFirstName().trim(), a.getLastName().trim(), a.getNumAyantDroit().trim())
                        .stream()
                        .filter(e -> e.getTypeAssure() == TypeAssure.AYANT_DROIT)
                        .filter(e -> e.getAssurePrincipal() == null || !Objects.equals(idClientExclu, e.getAssurePrincipal().getId()))
                        .findFirst()
                        .ifPresent(e -> {
                            throw new GenericError(
                                "Un ayant droit avec ces nom, prénom et numéro assuré existe déjà (%s)".formatted(nomComplet(e)),
                                "ayantDroitExistant"
                            );
                        })
                );
        }
    }

    private String trouverDossierIdentique(String firstName, String lastName, String num, Integer idClientExclu) {
        if (!StringUtils.hasText(firstName) || !StringUtils.hasText(lastName) || !StringUtils.hasText(num)) {
            return null;
        }
        return clientTiersPayantRepository
            .findDossiersIdentiques(firstName.trim(), lastName.trim(), num.trim(), idClientExclu == null ? -1 : idClientExclu)
            .stream()
            .findFirst()
            .map(c -> nomComplet(c.getAssuredCustomer()))
            .orElse(null);
    }

    private void verifierNumeroDeCarte(Integer tiersPayantId, String num, Integer idClientExclu) {
        if (tiersPayantId == null || !StringUtils.hasText(num)) {
            return;
        }
        clientTiersPayantRepository
            .findFirstByTiersPayantIdAndNum(tiersPayantId, num.trim())
            .filter(c -> !Objects.equals(idClientExclu, c.getAssuredCustomer().getId()))
            .ifPresent(c -> {
                throw new GenericError(
                    "Le numéro de carte « %s » est déjà utilisé par %s pour cet organisme".formatted(num.trim(), nomComplet(c.getAssuredCustomer())),
                    "numeroCarteExistant"
                );
            });
    }

    private String nomComplet(AssuredCustomer client) {
        return (client.getFirstName() + " " + client.getLastName()).trim();
    }

    private void canModifyTiersPayant(Integer id) throws GenericError {
        long countSales = thirdPartySaleLineRepository.countByClientTiersPayantId(id);
        if (countSales > 0) {
            throw new GenericError("Il existe des ventes avec tiers-payant. Veuillez basculer les ventes ");
        }
    }
}
