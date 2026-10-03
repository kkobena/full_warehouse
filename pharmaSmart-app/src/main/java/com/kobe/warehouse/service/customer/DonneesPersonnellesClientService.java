package com.kobe.warehouse.service.customer;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.RelanceDiffere;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.errors.GenericError;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Droits du client sur ses données (docs/PLAN-FICHE-CLIENT.md, lot 5 ; loi ivoirienne
 * n° 2013-450) : export de tout ce que l'officine détient, et effacement.
 *
 * <p>L'effacement est une <b>anonymisation</b> : les délivrances restent tracées (ventes, tiers
 * payant, dérogations), mais plus rien ne permet d'y retrouver la personne. Il est refusé tant que
 * le client doit encore de l'argent : la créance justifie de conserver son identité.
 */
@Service
@Transactional
public class DonneesPersonnellesClientService {

    static final String PRENOM_ANONYME = "ANONYME";
    static final String NUMERO_ANONYME = "ANONYMISE";
    private static final LocalDate ORIGINE = LocalDate.of(1970, 1, 1);

    private final EntityManager em;
    private final DossierSanteService dossierSanteService;
    private final CustomerDocumentService documentService;
    private final SalesRepository salesRepository;
    private final LogsService logsService;

    public DonneesPersonnellesClientService(
        EntityManager em,
        DossierSanteService dossierSanteService,
        CustomerDocumentService documentService,
        SalesRepository salesRepository,
        LogsService logsService
    ) {
        this.em = em;
        this.dossierSanteService = dossierSanteService;
        this.documentService = documentService;
        this.salesRepository = salesRepository;
        this.logsService = logsService;
    }

    @Transactional(readOnly = true)
    public DonneesClientDTO exporter(Integer customerId) {
        Customer customer = customer(customerId);
        LocalDate aujourdhui = LocalDate.now();
        List<DonneesClientDTO.Identite> ayantsDroit = em
            .createQuery("SELECT c FROM AssuredCustomer c WHERE c.assurePrincipal.id = :id ORDER BY c.lastName, c.firstName", AssuredCustomer.class)
            .setParameter("id", customerId)
            .getResultList()
            .stream()
            .map(DonneesPersonnellesClientService::identite)
            .toList();
        List<DonneesClientDTO.TiersPayant> tiersPayants = em
            .createQuery(
                "SELECT c FROM ClientTiersPayant c JOIN FETCH c.tiersPayant WHERE c.assuredCustomer.id = :id ORDER BY c.priorite",
                ClientTiersPayant.class
            )
            .setParameter("id", customerId)
            .getResultList()
            .stream()
            .map(c ->
                new DonneesClientDTO.TiersPayant(
                    c.getTiersPayant().getFullName(),
                    c.getNum(),
                    c.getTaux(),
                    c.getPriorite().name(),
                    c.getDateFinValidite()
                )
            )
            .toList();
        List<RelanceDiffereDTO> relances = em
            .createQuery("SELECT r FROM RelanceDiffere r JOIN FETCH r.user WHERE r.customerId = :id ORDER BY r.createdAt", RelanceDiffere.class)
            .setParameter("id", customerId)
            .getResultList()
            .stream()
            .map(r ->
                new RelanceDiffereDTO(
                    r.getCreatedAt(),
                    r.getTelephone(),
                    r.getMontant(),
                    r.getMessage(),
                    r.getUser().getFirstName() + " " + r.getUser().getLastName()
                )
            )
            .toList();
        return new DonneesClientDTO(
            LocalDateTime.now(),
            identite(customer),
            ayantsDroit,
            tiersPayants,
            dossierSanteService.dossier(customerId),
            documentService.attestation(customerId, ORIGINE, aujourdhui).depenses(),
            documentService.releve(customerId, ORIGINE, aujourdhui).lignes(),
            relances
        );
    }

    public void anonymiser(Integer customerId) {
        Customer customer = customer(customerId);
        if (PRENOM_ANONYME.equals(customer.getFirstName()) && customer.getCode().equals(customer.getLastName())) {
            throw new GenericError("Ce client est déjà anonymisé", "clientDejaAnonymise");
        }
        BigDecimal encours = salesRepository.getDiffereSoldeByCustomerId(customerId);
        if (encours != null && encours.signum() > 0) {
            throw new GenericError(
                "Le client doit encore " + encours.toBigInteger() + " : ses données ne peuvent être effacées qu'une fois ses différés réglés.",
                "anonymisationAvecEncours"
            );
        }
        customer.setFirstName(PRENOM_ANONYME);
        customer.setLastName(customer.getCode());
        customer.setPhone(null);
        customer.setEmail(null);
        customer.setStatus(Status.DISABLE);
        customer.setUpdatedAt(LocalDateTime.now());
        if (customer instanceof AssuredCustomer assure) {
            assure.setDatNaiss(null);
            assure.setSexe(null);
            assure.setNumAyantDroit(null);
            em
                .createQuery("UPDATE ClientTiersPayant c SET c.num = :num WHERE c.assuredCustomer = :client")
                .setParameter("num", NUMERO_ANONYME)
                .setParameter("client", assure)
                .executeUpdate();
        }
        em.createQuery("DELETE FROM CustomerAllergie a WHERE a.customerId = :id").setParameter("id", customerId).executeUpdate();
        em.createQuery("DELETE FROM CustomerDossierSante d WHERE d.customerId = :id").setParameter("id", customerId).executeUpdate();
        // Le message de relance reprend le prénom : il part avec le numéro.
        em
            .createQuery("UPDATE RelanceDiffere r SET r.telephone = '', r.message = '' WHERE r.customerId = :id")
            .setParameter("id", customerId)
            .executeUpdate();
        logsService.create(TransactionType.ANONYMISATION_CLIENT, "Anonymisation du client " + customer.getCode(), customerId.toString());
    }

    private Customer customer(Integer customerId) {
        Customer customer = customerId == null ? null : em.find(Customer.class, customerId);
        if (customer == null) {
            throw new GenericError("Client introuvable", "customerNotFound");
        }
        return customer;
    }

    private static DonneesClientDTO.Identite identite(Customer c) {
        AssuredCustomer a = c instanceof AssuredCustomer assure ? assure : null;
        return new DonneesClientDTO.Identite(
            c.getId(),
            c.getCode(),
            c.getFirstName(),
            c.getLastName(),
            c.getPhone(),
            c.getEmail(),
            c.getDatNaiss(),
            c.getSexe(),
            a == null ? null : a.getNumAyantDroit(),
            c.getStatus().name(),
            c.getCreatedAt()
        );
    }
}
