package com.kobe.warehouse.service.customer;

import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.CustomerConsentement;
import com.kobe.warehouse.domain.enumeration.CanalConsentement;
import com.kobe.warehouse.repository.CustomerConsentementRepository;
import com.kobe.warehouse.service.errors.GenericError;
import jakarta.persistence.EntityManager;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consentement du client aux messages, par canal (docs/PLAN-FICHE-CLIENT.md, lot 4). Chaque
 * changement est conservé : on sait qui l'a recueilli et quand.
 *
 * <p>Seul un refus explicite bloque un envoi. Les messages actuels (relance d'un différé, avoir
 * disponible) concernent une opération en cours du client ; les bloquer faute de réponse
 * couperait toutes les fiches existantes, jamais interrogées.
 */
@Service
@Transactional
public class ConsentementService {

    private final CustomerConsentementRepository repository;
    private final EntityManager em;
    private final DerogationAuthorizer derogationAuthorizer;

    public ConsentementService(CustomerConsentementRepository repository, EntityManager em, DerogationAuthorizer derogationAuthorizer) {
        this.repository = repository;
        this.em = em;
        this.derogationAuthorizer = derogationAuthorizer;
    }

    /** Un état par canal, dans l'ordre de l'énumération. */
    @Transactional(readOnly = true)
    public List<ConsentementDTO> etat(Integer customerId) {
        List<CustomerConsentement> historique = repository.findAllByCustomerIdOrderByCreatedAtDesc(customerId);
        return Arrays.stream(CanalConsentement.values())
            .map(canal ->
                historique.stream().filter(c -> c.getCanal() == canal).findFirst().map(ConsentementService::toDto).orElse(new ConsentementDTO(canal, null, null, null))
            )
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ConsentementDTO> historique(Integer customerId) {
        return repository.findAllByCustomerIdOrderByCreatedAtDesc(customerId).stream().map(ConsentementService::toDto).toList();
    }

    public ConsentementDTO enregistrer(Integer customerId, CanalConsentement canal, boolean accorde) {
        if (em.find(Customer.class, customerId) == null) {
            throw new GenericError("Client introuvable", "customerNotFound");
        }
        return toDto(
            repository.save(
                new CustomerConsentement().setCustomerId(customerId).setCanal(canal).setAccorde(accorde).setUser(derogationAuthorizer.utilisateurCourant())
            )
        );
    }

    /** Vrai si le client a retiré son accord pour ce canal. */
    @Transactional(readOnly = true)
    public boolean estRefuse(Integer customerId, CanalConsentement canal) {
        return repository
            .findAllByCustomerIdOrderByCreatedAtDesc(customerId)
            .stream()
            .filter(c -> c.getCanal() == canal)
            .findFirst()
            .map(c -> !c.isAccorde())
            .orElse(false);
    }

    private static ConsentementDTO toDto(CustomerConsentement c) {
        return new ConsentementDTO(c.getCanal(), c.isAccorde(), c.getCreatedAt(), c.getUser().getFirstName() + " " + c.getUser().getLastName());
    }
}
