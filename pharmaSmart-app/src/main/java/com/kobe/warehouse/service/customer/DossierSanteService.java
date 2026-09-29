package com.kobe.warehouse.service.customer;

import com.kobe.warehouse.domain.AlerteSanteDerogation;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CustomerAllergie;
import com.kobe.warehouse.domain.CustomerDossierSante;
import com.kobe.warehouse.domain.Dci;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.StatutDci;
import com.kobe.warehouse.repository.AlerteSanteDerogationRepository;
import com.kobe.warehouse.repository.CustomerAllergieRepository;
import com.kobe.warehouse.repository.CustomerDossierSanteRepository;
import com.kobe.warehouse.repository.CustomerRepository;
import com.kobe.warehouse.service.errors.GenericError;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Dossier de sécurité du patient et alertes à la vente (docs/PLAN-FICHE-CLIENT.md, lot 2).
 *
 * <p>Une allergie à une molécule que le produit contient bloque sa délivrance : passer outre exige
 * le droit {@value #DROIT_DEROGATION}, ou la clé d'un collègue qui le détient, et laisse une trace.
 * Grossesse et allaitement ne sont que rappelés : sans données de contre-indication par produit,
 * les bloquer arrêterait toutes les ventes de la patiente.
 */
@Service
@Transactional
public class DossierSanteService {

    public static final String DROIT_DEROGATION = "pr-forcer-alerte-sante";
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CustomerRepository customerRepository;
    private final CustomerDossierSanteRepository dossierRepository;
    private final CustomerAllergieRepository allergieRepository;
    private final AlerteSanteDerogationRepository derogationRepository;
    private final DerogationAuthorizer derogationAuthorizer;
    private final EntityManager em;

    public DossierSanteService(
        CustomerRepository customerRepository,
        CustomerDossierSanteRepository dossierRepository,
        CustomerAllergieRepository allergieRepository,
        AlerteSanteDerogationRepository derogationRepository,
        DerogationAuthorizer derogationAuthorizer,
        EntityManager em
    ) {
        this.customerRepository = customerRepository;
        this.dossierRepository = dossierRepository;
        this.allergieRepository = allergieRepository;
        this.derogationRepository = derogationRepository;
        this.derogationAuthorizer = derogationAuthorizer;
        this.em = em;
    }

    @Transactional(readOnly = true)
    public DossierSanteDTO dossier(Integer customerId) {
        List<DossierSanteDTO.Allergie> allergies = allergieRepository
            .findAllByCustomerIdOrderByCreatedAtAsc(customerId)
            .stream()
            .map(DossierSanteService::toDto)
            .toList();
        return dossierRepository
            .findById(customerId)
            .map(d ->
                new DossierSanteDTO(
                    allergies,
                    List.copyOf(d.getPathologies()),
                    d.isGrossesse(),
                    d.getDateTerme(),
                    d.isAllaitement(),
                    d.getPoidsKg(),
                    d.getDatePesee(),
                    d.getNote(),
                    d.getUpdatedAt(),
                    d.getUpdatedBy() == null ? null : d.getUpdatedBy().getFirstName() + " " + d.getUpdatedBy().getLastName()
                )
            )
            .orElseGet(() ->
                new DossierSanteDTO(allergies, List.of(), false, null, false, null, null, null, null, null)
            );
    }

    public DossierSanteDTO enregistrer(Integer customerId, DossierSanteDTO dto) {
        if (!customerRepository.existsById(customerId)) {
            throw new GenericError("Client introuvable", "customerNotFound");
        }
        CustomerDossierSante dossier = dossierRepository.findById(customerId).orElseGet(() -> new CustomerDossierSante().setCustomerId(customerId));
        dossier
            .setPathologies(nettoyer(dto.pathologies()))
            .setGrossesse(dto.grossesse())
            .setDateTerme(dto.grossesse() ? dto.dateTerme() : null)
            .setAllaitement(dto.allaitement())
            .setPoidsKg(dto.poidsKg())
            .setDatePesee(dto.poidsKg() == null ? null : Objects.requireNonNullElse(dto.datePesee(), LocalDate.now()))
            .setNote(StringUtils.hasText(dto.note()) ? dto.note().trim() : null)
            .setUpdatedAt(LocalDateTime.now())
            .setUpdatedBy(derogationAuthorizer.utilisateurCourant());
        dossierRepository.save(dossier);
        synchroniserAllergies(customerId, dto.allergies() == null ? List.of() : dto.allergies());
        return dossier(customerId);
    }

    /** Alertes levées par l'ajout du produit à une vente de ce client : les bloquantes d'abord. */
    @Transactional(readOnly = true)
    public List<AlerteSanteDTO> alertes(Integer customerId, Integer produitId) {
        List<AlerteSanteDTO> alertes = new ArrayList<>();
        for (Object[] ligne : allergieRepository.findAllergiesMisesEnCause(customerId, produitId, StatutDci.COMPOSEE)) {
            CustomerAllergie allergie = (CustomerAllergie) ligne[0];
            String molecule = (String) ligne[1];
            String message = "Allergie connue à " + allergie.getDci().getLibelle() + " : ce produit contient " + molecule + ".";
            if (StringUtils.hasText(allergie.getReaction())) {
                message += " Réaction signalée : " + allergie.getReaction() + ".";
            }
            alertes.add(new AlerteSanteDTO(AlerteSanteDTO.BLOQUANTE, "ALLERGIE", message));
        }
        dossierRepository
            .findById(customerId)
            .ifPresent(dossier -> {
                if (grossesseEnCours(dossier)) {
                    String terme = dossier.getDateTerme() == null ? "" : " (terme prévu le " + dossier.getDateTerme().format(JOUR) + ")";
                    alertes.add(new AlerteSanteDTO(AlerteSanteDTO.INFO, "GROSSESSE", "Patiente enceinte" + terme + " : vérifier la compatibilité du produit."));
                }
                if (dossier.isAllaitement()) {
                    alertes.add(new AlerteSanteDTO(AlerteSanteDTO.INFO, "ALLAITEMENT", "Patiente allaitante : vérifier la compatibilité du produit."));
                }
            });
        return alertes;
    }

    /**
     * Délivrance malgré une alerte bloquante. L'utilisateur qui détient le droit passe seul ; les
     * autres présentent la clé de sécurité d'un collègue qui le détient.
     */
    public void deroger(Integer customerId, DerogationAlerteSanteDTO demande) {
        List<AlerteSanteDTO> bloquantes = alertes(customerId, demande.produitId()).stream().filter(AlerteSanteDTO::bloquante).toList();
        if (bloquantes.isEmpty()) {
            throw new GenericError("Aucune alerte à lever pour ce produit.", "aucuneAlerteSante");
        }
        AppUser utilisateur = derogationAuthorizer.utilisateurCourant();
        AppUser autorisePar = derogationAuthorizer.autoriser(
            DROIT_DEROGATION,
            demande.actionAuthorityKey(),
            "la délivrance malgré une allergie",
            "alerteSante"
        );
        derogationRepository.save(
            new AlerteSanteDerogation()
                .setCustomerId(customerId)
                .setProduit(em.getReference(Produit.class, demande.produitId()))
                .setAlertes(bloquantes.stream().map(AlerteSanteDTO::message).collect(Collectors.joining("\n")))
                .setMotif(demande.motif().trim())
                .setUser(utilisateur)
                .setAutorisePar(autorisePar)
        );
    }

    private void synchroniserAllergies(Integer customerId, List<DossierSanteDTO.Allergie> saisies) {
        Map<Integer, CustomerAllergie> existantes = allergieRepository
            .findAllByCustomerIdOrderByCreatedAtAsc(customerId)
            .stream()
            .collect(Collectors.toMap(CustomerAllergie::getId, Function.identity()));
        // Les suppressions d'abord : remplacer une allergie par la même molécule heurterait sinon
        // l'unicité (client, molécule) avant que l'ancienne ne parte.
        Set<Integer> conservees = saisies.stream().map(DossierSanteDTO.Allergie::id).filter(existantes::containsKey).collect(Collectors.toSet());
        existantes.values().stream().filter(a -> !conservees.contains(a.getId())).forEach(allergieRepository::delete);
        allergieRepository.flush();
        Set<Integer> molecules = new HashSet<>();
        for (DossierSanteDTO.Allergie saisie : saisies) {
            String libelle = StringUtils.hasText(saisie.libelle()) ? saisie.libelle().trim() : null;
            if (saisie.dciId() == null && libelle == null) {
                continue;
            }
            // Une même molécule saisie deux fois n'est gardée qu'une fois (contrainte d'unicité).
            if (saisie.dciId() != null && !molecules.add(saisie.dciId())) {
                continue;
            }
            CustomerAllergie allergie = saisie.id() != null && existantes.containsKey(saisie.id())
                ? existantes.get(saisie.id())
                : new CustomerAllergie().setCustomerId(customerId);
            allergie
                .setDci(saisie.dciId() == null ? null : em.getReference(Dci.class, saisie.dciId()))
                .setLibelle(saisie.dciId() == null ? libelle : null)
                .setReaction(StringUtils.hasText(saisie.reaction()) ? saisie.reaction().trim() : null);
            allergieRepository.save(allergie);
        }
    }

    private static boolean grossesseEnCours(CustomerDossierSante dossier) {
        return dossier.isGrossesse() && (dossier.getDateTerme() == null || !dossier.getDateTerme().isBefore(LocalDate.now()));
    }

    private static List<String> nettoyer(List<String> libelles) {
        if (libelles == null) {
            return new ArrayList<>();
        }
        return libelles.stream().filter(StringUtils::hasText).map(String::trim).distinct().collect(Collectors.toCollection(ArrayList::new));
    }

    private static DossierSanteDTO.Allergie toDto(CustomerAllergie a) {
        Dci dci = a.getDci();
        return new DossierSanteDTO.Allergie(a.getId(), dci == null ? null : dci.getId(), dci == null ? null : dci.getLibelle(), a.getLibelle(), a.getReaction());
    }
}
