package com.kobe.warehouse.service.customer;

import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.CustomerTraitementChronique;
import com.kobe.warehouse.domain.Dci;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.StatutLegal;
import com.kobe.warehouse.repository.CustomerTraitementChroniqueRepository;
import com.kobe.warehouse.service.customer.TraitementChroniqueDTO.Ordonnance;
import com.kobe.warehouse.service.customer.TraitementChroniqueDTO.Suivi;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import jakarta.persistence.EntityManager;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Traitements chroniques et rappel de renouvellement (docs/PLAN-FICHE-CLIENT.md, lot 4).
 *
 * <p>Le suivi se calcule à la lecture : l'échéance est la dernière délivrance au patient réel
 * (l'ayant droit s'il y en a un) d'un produit du traitement, plus la durée couverte. Sans produit
 * imposé, tout produit contenant la molécule compte, déconditionnés compris.
 *
 * <p>Renouvellement exceptionnel (paramètre {@code APP_RENOUVELLEMENT_EXCEPTIONNEL}, repris de
 * l'article L. 5125-23-1 du code de la santé publique français) : ordonnance expirée d'au moins
 * trois mois, hors stupéfiants et assimilés, première délivrance dans le mois qui suit
 * l'expiration, et au plus {@code APP_RENOUVELLEMENT_EXCEPTIONNEL_MOIS} mois après elle.
 */
@Service
@Transactional
public class TraitementChroniqueService {

    private static final int DUREE_MINIMALE_ORDONNANCE_MOIS = 3;
    private static final Set<StatutLegal> STUPEFIANTS = EnumSet.of(StatutLegal.STUPEFIANTS, StatutLegal.PSO);
    private static final Set<Suivi> A_RELANCER = EnumSet.of(Suivi.A_RENOUVELER, Suivi.EN_RETARD, Suivi.RUPTURE);

    private final CustomerTraitementChroniqueRepository repository;
    private final EntityManager em;
    private final AppConfigurationService configuration;
    private final DerogationAuthorizer derogationAuthorizer;

    public TraitementChroniqueService(
        CustomerTraitementChroniqueRepository repository,
        EntityManager em,
        AppConfigurationService configuration,
        DerogationAuthorizer derogationAuthorizer
    ) {
        this.repository = repository;
        this.em = em;
        this.configuration = configuration;
        this.derogationAuthorizer = derogationAuthorizer;
    }

    @Transactional(readOnly = true)
    public List<TraitementChroniqueDTO> traitements(Integer customerId) {
        LocalDate aujourdhui = LocalDate.now();
        return repository.findAllByCustomerIdOrderByActifDescCreatedAtAsc(customerId).stream().map(t -> evaluer(t, aujourdhui)).toList();
    }

    /** Patients actifs dont un traitement arrive à échéance, est en retard ou interrompu ; les plus urgents d'abord. */
    @Transactional(readOnly = true)
    public List<TraitementARenouvelerDTO> aRenouveler() {
        LocalDate aujourdhui = LocalDate.now();
        return repository
            .findAllByActifTrue()
            .stream()
            .map(t -> {
                Customer c = em.find(Customer.class, t.getCustomerId());
                TraitementChroniqueDTO dto = evaluer(t, aujourdhui);
                return c == null || c.getStatus() != Status.ENABLE || !A_RELANCER.contains(dto.suivi())
                    ? null
                    : new TraitementARenouvelerDTO(c.getId(), c.getCode(), c.getFirstName() + " " + c.getLastName(), c.getPhone(), dto);
            })
            .filter(java.util.Objects::nonNull)
            .sorted(Comparator.comparing(r -> r.traitement().joursRestants(), Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
    }

    public TraitementChroniqueDTO declarer(Integer customerId, TraitementChroniqueSaisieDTO saisie) {
        if (em.find(Customer.class, customerId) == null) {
            throw new GenericError("Client introuvable", "customerNotFound");
        }
        CustomerTraitementChronique traitement = new CustomerTraitementChronique().setCustomerId(customerId);
        appliquer(traitement, saisie);
        return evaluer(repository.save(traitement), LocalDate.now());
    }

    public TraitementChroniqueDTO modifier(Integer customerId, Integer id, TraitementChroniqueSaisieDTO saisie) {
        CustomerTraitementChronique traitement = repository
            .findById(id)
            .filter(t -> t.getCustomerId().equals(customerId))
            .orElseThrow(() -> new GenericError("Traitement introuvable", "traitementIntrouvable"));
        appliquer(traitement, saisie);
        return evaluer(repository.save(traitement), LocalDate.now());
    }

    private void appliquer(CustomerTraitementChronique t, TraitementChroniqueSaisieDTO saisie) {
        Dci dci = saisie.dciId() == null ? null : em.find(Dci.class, saisie.dciId());
        Produit produit = saisie.produitId() == null ? null : em.find(Produit.class, saisie.produitId());
        if (dci == null && produit == null) {
            throw new GenericError("Indiquez la molécule du traitement, ou le produit imposé.", "traitementSansCible");
        }
        if (dci != null && produit != null && !contient(produit.getId(), dci.getId())) {
            throw new GenericError("Le produit imposé ne contient pas la molécule du traitement.", "traitementProduitSansMolecule");
        }
        if (saisie.dureeJours() == null || saisie.dureeJours() <= 0) {
            throw new GenericError("La durée couverte par une délivrance est obligatoire.", "traitementSansDuree");
        }
        if (saisie.dateOrdonnance() != null && saisie.dateFinOrdonnance() != null && saisie.dateFinOrdonnance().isBefore(saisie.dateOrdonnance())) {
            throw new GenericError("La fin de l'ordonnance précède sa date.", "traitementOrdonnanceIncoherente");
        }
        t
            .setDci(dci)
            .setProduit(produit)
            .setDosage(texte(saisie.dosage()))
            .setPosologie(texte(saisie.posologie()))
            .setDureeJours(saisie.dureeJours())
            .setDateOrdonnance(saisie.dateOrdonnance())
            .setDateFinOrdonnance(saisie.dateFinOrdonnance())
            .setNote(texte(saisie.note()))
            .setActif(saisie.actif() == null || saisie.actif())
            .setUpdatedAt(LocalDateTime.now())
            .setUpdatedBy(derogationAuthorizer.utilisateurCourant());
    }

    TraitementChroniqueDTO evaluer(CustomerTraitementChronique t, LocalDate aujourdhui) {
        Delivrance delivrance = derniereDelivrance(t);
        LocalDate derniere = delivrance == null ? null : delivrance.date();
        LocalDate prochaine = derniere == null ? null : derniere.plusDays(t.getDureeJours());
        Long joursRestants = prochaine == null ? null : ChronoUnit.DAYS.between(aujourdhui, prochaine);
        Suivi suivi = suivi(t, joursRestants);

        Ordonnance ordonnance = t.getDateFinOrdonnance() == null
            ? Ordonnance.NON_RENSEIGNEE
            : t.getDateFinOrdonnance().isBefore(aujourdhui) ? Ordonnance.EXPIREE : Ordonnance.VALIDE;
        LocalDate exceptionnelJusquau = null;
        String motif = null;
        if (t.isActif() && ordonnance == Ordonnance.EXPIREE && configuration.isRenouvellementExceptionnelEnabled()) {
            motif = motifRefusExceptionnel(t, derniere, aujourdhui);
            if (motif == null) {
                exceptionnelJusquau = t.getDateFinOrdonnance().plusMonths(configuration.getRenouvellementExceptionnelMois());
            }
        }

        Dci dci = t.getDci();
        Produit produit = t.getProduit();
        return new TraitementChroniqueDTO(
            t.getId(),
            dci == null ? null : dci.getId(),
            dci == null ? null : dci.getLibelle(),
            produit == null ? null : produit.getId(),
            produit == null ? null : produit.getLibelle(),
            t.getDosage(),
            t.getPosologie(),
            t.getDureeJours(),
            t.getDateOrdonnance(),
            t.getDateFinOrdonnance(),
            t.getNote(),
            t.isActif(),
            derniere,
            delivrance == null ? null : delivrance.produitId(),
            delivrance == null ? null : delivrance.libelle(),
            prochaine,
            joursRestants,
            suivi,
            ordonnance,
            exceptionnelJusquau,
            motif
        );
    }

    private Suivi suivi(CustomerTraitementChronique t, Long joursRestants) {
        if (!t.isActif()) {
            return Suivi.ARRETE;
        }
        if (joursRestants == null) {
            return Suivi.SANS_DELIVRANCE;
        }
        if (joursRestants > configuration.getRappelRenouvellementJours()) {
            return Suivi.A_JOUR;
        }
        if (joursRestants >= 0) {
            return Suivi.A_RENOUVELER;
        }
        return -joursRestants > t.getDureeJours() ? Suivi.RUPTURE : Suivi.EN_RETARD;
    }

    /** Null si le renouvellement exceptionnel est possible, sinon la condition qui manque. */
    private String motifRefusExceptionnel(CustomerTraitementChronique t, LocalDate derniere, LocalDate aujourdhui) {
        LocalDate fin = t.getDateFinOrdonnance();
        if (stupefiant(t)) {
            return "Stupéfiant ou assimilé : renouvellement exceptionnel exclu.";
        }
        if (t.getDateOrdonnance() == null) {
            return "Date de l'ordonnance non renseignée : sa durée ne peut être vérifiée.";
        }
        if (t.getDateOrdonnance().plusMonths(DUREE_MINIMALE_ORDONNANCE_MOIS).isAfter(fin)) {
            return "Ordonnance de moins de " + DUREE_MINIMALE_ORDONNANCE_MOIS + " mois.";
        }
        if (aujourdhui.isAfter(fin.plusMonths(configuration.getRenouvellementExceptionnelMois()))) {
            return "Délai de " + configuration.getRenouvellementExceptionnelMois() + " mois après l'expiration dépassé.";
        }
        boolean delivreDepuisExpiration = derniere != null && derniere.isAfter(fin);
        if (!delivreDepuisExpiration && aujourdhui.isAfter(fin.plusMonths(1))) {
            return "Aucune délivrance dans le mois qui a suivi l'expiration.";
        }
        return null;
    }

    private boolean stupefiant(CustomerTraitementChronique t) {
        if (t.getProduit() != null) {
            return STUPEFIANTS.contains(t.getProduit().getStatutLegal());
        }
        Long n = em
            .createQuery(
                "SELECT COUNT(pd) FROM ProduitDci pd WHERE pd.dci.id = :dci AND pd.produit.statutLegal IN :statuts",
                Long.class
            )
            .setParameter("dci", t.getDci().getId())
            .setParameter("statuts", STUPEFIANTS)
            .getSingleResult();
        return n > 0;
    }

    private record Delivrance(LocalDate date, Integer produitId, String libelle) {}

    private Delivrance derniereDelivrance(CustomerTraitementChronique t) {
        String produits = t.getProduit() != null ? ":cible" : "(SELECT pd.produit_id FROM produit_dci pd WHERE pd.dci_id = :cible)";
        @SuppressWarnings("unchecked")
        List<Object[]> lignes = em
            .createNativeQuery(
                "SELECT s.sale_date, p.id, p.libelle FROM sales_line l" +
                " JOIN sales s ON s.id = l.sales_id AND s.sale_date = l.sales_sale_date" +
                " JOIN produit p ON p.id = l.produit_id" +
                " WHERE s.statut = 'CLOSED' AND NOT s.canceled" +
                " AND ((s.customer_id = :patient AND s.ayant_droit_id IS NULL) OR s.ayant_droit_id = :patient)" +
                " AND (p.id IN (" + produits + ") OR p.parent_id IN (" + produits + "))" +
                " ORDER BY s.sale_date DESC, s.updated_at DESC LIMIT 1"
            )
            .setParameter("patient", t.getCustomerId())
            .setParameter("cible", t.getProduit() != null ? t.getProduit().getId() : t.getDci().getId())
            .getResultList();
        if (lignes.isEmpty()) {
            return null;
        }
        Object[] r = lignes.getFirst();
        return new Delivrance(r[0] instanceof Date d ? d.toLocalDate() : (LocalDate) r[0], ((Number) r[1]).intValue(), (String) r[2]);
    }

    private boolean contient(Integer produitId, Integer dciId) {
        return em
            .createQuery("SELECT COUNT(pd) FROM ProduitDci pd WHERE pd.produit.id = :produit AND pd.dci.id = :dci", Long.class)
            .setParameter("produit", produitId)
            .setParameter("dci", dciId)
            .getSingleResult() > 0;
    }

    private static String texte(String valeur) {
        return StringUtils.hasText(valeur) ? valeur.trim() : null;
    }
}
