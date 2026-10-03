package com.kobe.warehouse.service;

import com.kobe.warehouse.Util;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeAssure;
import com.kobe.warehouse.repository.UninsuredCustomerRepository;
import com.kobe.warehouse.service.customer.HistoriqueClientService;
import com.kobe.warehouse.service.dto.ControleClientDTO;
import com.kobe.warehouse.service.dto.UninsuredCustomerDTO;
import com.kobe.warehouse.service.errors.CustomerAlreadyExistException;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.errors.InvalidPhoneNumberException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UninsuredCustomerService {

    /** Au-delà, deux noms complets sont jugés voisins (une faute de frappe : « Kouacy » / « Kouassi »). */
    private static final double SEUIL_NOM_PROCHE = 0.6;

    private final UninsuredCustomerRepository uninsuredCustomerRepository;
    private final HistoriqueClientService historiqueClientService;

    public UninsuredCustomerService(UninsuredCustomerRepository uninsuredCustomerRepository, HistoriqueClientService historiqueClientService) {
        this.uninsuredCustomerRepository = uninsuredCustomerRepository;
        this.historiqueClientService = historiqueClientService;
    }

    public UninsuredCustomerDTO create(UninsuredCustomerDTO dto) throws CustomerAlreadyExistException {
        Optional<UninsuredCustomer> uninsuredCustomerOptional = findOne(dto);
        if (uninsuredCustomerOptional.isPresent()) {
            throw clientDejaExistant(uninsuredCustomerOptional.get());
        }
        if (org.springframework.util.StringUtils.hasText(dto.getPhone()) && !Util.isValidPhoneNumber(dto.getPhone())) {
            throw new InvalidPhoneNumberException();
        }
        var uninsuredCustomer = new UninsuredCustomer();
        uninsuredCustomer.setCreatedAt(LocalDateTime.now());
        uninsuredCustomer.setUpdatedAt(uninsuredCustomer.getUpdatedAt());
        uninsuredCustomer.setFirstName(dto.getFirstName());
        uninsuredCustomer.setLastName(dto.getLastName());
        uninsuredCustomer.setPhone(dto.getPhone());
        uninsuredCustomer.setEmail(dto.getEmail());
        uninsuredCustomer.setDatNaiss(dto.getDatNaiss());
        uninsuredCustomer.setSexe(StringUtils.isNotEmpty(dto.getSexe()) ? dto.getSexe() : null);
        uninsuredCustomer.setTypeAssure(TypeAssure.PRINCIPAL);
        uninsuredCustomer.setCode(RandomStringUtils.randomNumeric(6));
        var cust = uninsuredCustomerRepository.save(uninsuredCustomer);
        return uninsuredCustomerFromEntity(cust);
    }

    public UninsuredCustomerDTO update(UninsuredCustomerDTO dto) throws CustomerAlreadyExistException {
        Optional<UninsuredCustomer> uninsuredCustomerOptional = findOne(dto);
        if (uninsuredCustomerOptional.isPresent() && !Objects.equals(uninsuredCustomerOptional.get().getId(), dto.getId())) {
            throw clientDejaExistant(uninsuredCustomerOptional.get());
        }
        if (org.springframework.util.StringUtils.hasText(dto.getPhone()) && !Util.isValidPhoneNumber(dto.getPhone())) {
            throw new InvalidPhoneNumberException();
        }
        var uninsuredCustomer = uninsuredCustomerRepository.getReferenceById(dto.getId());
        uninsuredCustomer.setUpdatedAt(uninsuredCustomer.getUpdatedAt());
        uninsuredCustomer.setFirstName(dto.getFirstName());
        uninsuredCustomer.setLastName(dto.getLastName());
        uninsuredCustomer.setPhone(dto.getPhone());
        uninsuredCustomer.setEmail(dto.getEmail());
        uninsuredCustomer.setDatNaiss(dto.getDatNaiss());
        uninsuredCustomer.setSexe(StringUtils.isNotEmpty(dto.getSexe()) ? dto.getSexe() : null);
        var cust = uninsuredCustomerRepository.save(uninsuredCustomer);
        return uninsuredCustomerFromEntity(cust);
    }

    private UninsuredCustomerDTO uninsuredCustomerFromEntity(UninsuredCustomer uninsuredCustomer) {
        var customerDTO = new UninsuredCustomerDTO();
        customerDTO.setEmail(uninsuredCustomer.getEmail());
        customerDTO.setDatNaiss(uninsuredCustomer.getDatNaiss());
        customerDTO.setSexe(uninsuredCustomer.getSexe());
        customerDTO.setPhone(uninsuredCustomer.getPhone());
        customerDTO.setFirstName(uninsuredCustomer.getFirstName());
        customerDTO.setLastName(uninsuredCustomer.getLastName());
        customerDTO.setCode(uninsuredCustomer.getCode());
        customerDTO.setId(uninsuredCustomer.getId());
        customerDTO.setFullName(uninsuredCustomer.getFirstName() + " " + uninsuredCustomer.getLastName());
        return customerDTO;
    }

    public List<UninsuredCustomerDTO> fetch(String query) {
        Specification<UninsuredCustomer> specification = uninsuredCustomerRepository.specialisation(Status.ENABLE);
        if (StringUtils.isNotEmpty(query)) {
            query = query.toUpperCase() + "%";
            specification = specification.and(uninsuredCustomerRepository.specialisationQueryString(query));
        }
        return uninsuredCustomerRepository
            .findAll(specification, Sort.by(Sort.Direction.ASC, "firstName", "lastName"))
            .stream()
            .map(UninsuredCustomerDTO::new)
            .collect(Collectors.toList());
    }

    private CustomerAlreadyExistException clientDejaExistant(UninsuredCustomer existant) {
        return new CustomerAlreadyExistException(
            "Un client « %s %s » existe déjà avec ce téléphone".formatted(existant.getFirstName(), existant.getLastName())
        );
    }

    /**
     * Contrôle anticipé de la saisie : le client identique (nom, prénom, téléphone) qui bloquerait la
     * création, et les clients voisins à signaler — même téléphone, même nom, nom proche.
     */
    @Transactional(readOnly = true)
    public ControleClientDTO controler(String phone, String firstName, String lastName, Integer excludeId) {
        boolean avecIdentite = StringUtils.isNotBlank(firstName) && StringUtils.isNotBlank(lastName);
        boolean avecTelephone = StringUtils.isNotBlank(phone);
        final UninsuredCustomer existant;
        if (avecIdentite && avecTelephone) {
            UninsuredCustomerDTO identite = new UninsuredCustomerDTO();
            identite.setFirstName(firstName.trim());
            identite.setLastName(lastName.trim());
            identite.setPhone(phone.trim());
            existant = findOne(identite).filter(c -> !Objects.equals(excludeId, c.getId())).orElse(null);
        } else {
            existant = null;
        }
        // Voisins, sans doublon, dans l'ordre : même téléphone, même nom, nom proche.
        Map<Integer, ControleClientDTO.ClientProcheDTO> proches = new LinkedHashMap<>();
        if (avecTelephone) {
            uninsuredCustomerRepository.findAllByPhone(phone.trim()).forEach(c -> ajouterProche(proches, c, "même téléphone", excludeId, existant));
        }
        if (avecIdentite) {
            uninsuredCustomerRepository
                .findAllByFirstNameIgnoreCaseAndLastNameIgnoreCase(firstName.trim(), lastName.trim())
                .forEach(c -> ajouterProche(proches, c, "même nom", excludeId, existant));
            String nom = (firstName.trim() + " " + lastName.trim()).toUpperCase();
            if (nom.length() >= 5) {
                uninsuredCustomerRepository
                    .findAllByNomProche(nom, SEUIL_NOM_PROCHE, PageRequest.of(0, 5))
                    .forEach(c -> ajouterProche(proches, c, "nom proche", excludeId, existant));
            }
        }
        List<ControleClientDTO.ClientProcheDTO> liste = new ArrayList<>(proches.values());
        return new ControleClientDTO(
            existant == null ? null : uninsuredCustomerFromEntity(existant),
            liste.size() > 5 ? liste.subList(0, 5) : liste
        );
    }

    private void ajouterProche(
        Map<Integer, ControleClientDTO.ClientProcheDTO> proches,
        UninsuredCustomer client,
        String motif,
        Integer excludeId,
        UninsuredCustomer existant
    ) {
        boolean ecarte =
            Objects.equals(excludeId, client.getId()) ||
            (existant != null && Objects.equals(existant.getId(), client.getId())) ||
            client.getStatus() != Status.ENABLE;
        if (!ecarte) {
            proches.putIfAbsent(client.getId(), new ControleClientDTO.ClientProcheDTO(uninsuredCustomerFromEntity(client), motif));
        }
    }

    public Optional<UninsuredCustomer> findOne(UninsuredCustomerDTO dto) {
        Specification<UninsuredCustomer> specification = uninsuredCustomerRepository.specialisationCheckExist(
            dto.getFirstName(),
            dto.getLastName(),
            dto.getPhone()
        );
        return uninsuredCustomerRepository.findOne(specification);
    }

    public void deleteCustomerById(Integer id) throws GenericError {
        historiqueClientService.verifierSuppression(id);
        try {
            uninsuredCustomerRepository.deleteById(id);
            // Écrit maintenant, pour que la violation de clé étrangère tombe dans ce catch et non au commit.
            uninsuredCustomerRepository.flush();
        } catch (Exception e) {
            throw new GenericError("Impossible de supprimer ce client : des données lui sont encore rattachées. Désactivez-le.", "deleteCustomer");
        }
    }
}
