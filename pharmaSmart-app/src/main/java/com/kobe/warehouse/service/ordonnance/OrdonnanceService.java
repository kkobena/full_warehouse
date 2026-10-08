package com.kobe.warehouse.service.ordonnance;

import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.CustomerTraitementChronique;
import com.kobe.warehouse.domain.ProduitDci;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import com.kobe.warehouse.domain.enumeration.DecisionRapprochement;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.StatutOrdonnance;
import com.kobe.warehouse.domain.ordonnance.Ordonnance;
import com.kobe.warehouse.domain.ordonnance.OrdonnanceDelivrance;
import com.kobe.warehouse.domain.ordonnance.OrdonnanceLigne;
import com.kobe.warehouse.domain.ordonnance.OrdonnanceVente;
import com.kobe.warehouse.domain.ordonnance.Prescripteur;
import com.kobe.warehouse.domain.ordonnance.VentePrescripteur;
import com.kobe.warehouse.repository.CustomerRepository;
import com.kobe.warehouse.repository.CustomerTraitementChroniqueRepository;
import com.kobe.warehouse.repository.OrdonnanceDelivranceRepository;
import com.kobe.warehouse.repository.OrdonnanceRepository;
import com.kobe.warehouse.repository.OrdonnanceVenteRepository;
import com.kobe.warehouse.repository.PrescripteurRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.ProduitDciRepository;
import com.kobe.warehouse.repository.ProduitRefSpecialiteRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.VentePrescripteurRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.ordonnance.AppariementDTO;
import com.kobe.warehouse.service.dto.ordonnance.LigneSuivieDTO;
import com.kobe.warehouse.service.dto.ordonnance.OrdonnanceCreationDTO;
import com.kobe.warehouse.service.dto.ordonnance.OrdonnanceDTO;
import com.kobe.warehouse.service.dto.ordonnance.OrdonnanceLigneCreationDTO;
import com.kobe.warehouse.service.dto.ordonnance.OrdonnanceLigneDTO;
import com.kobe.warehouse.service.dto.ordonnance.ProduitGroupeDTO;
import com.kobe.warehouse.service.dto.ordonnance.QuantiteDelivreeDTO;
import com.kobe.warehouse.service.errors.GenericError;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Suivi d'ordonnance  : saisie, reste à délivrer, renouvellements, rattachement aux
 * ventes. Une délivrance ne compte que si sa vente est CLÔTURÉE et NON ANNULÉE (jointure à la
 * lecture) : une vente abandonnée, en attente ou annulée ne consomme pas l'ordonnance, et son
 * annulation rétablit le reste à délivrer sans rien avoir à défaire.
 */
@Service
@Transactional
public class OrdonnanceService {

    private final OrdonnanceRepository ordonnanceRepository;
    private final OrdonnanceDelivranceRepository delivranceRepository;
    private final OrdonnanceVenteRepository venteRepository;
    private final PrescripteurRepository prescripteurRepository;
    private final CustomerRepository customerRepository;
    private final ProduitRepository produitRepository;
    private final SalesRepository salesRepository;
    private final SalesLineRepository salesLineRepository;
    private final ProduitRefSpecialiteRepository produitRefSpecialiteRepository;
    private final CustomerTraitementChroniqueRepository traitementChroniqueRepository;
    private final ProduitDciRepository produitDciRepository;
    private final VentePrescripteurRepository ventePrescripteurRepository;
    private final UserService userService;

    public OrdonnanceService(
        OrdonnanceRepository ordonnanceRepository,
        OrdonnanceDelivranceRepository delivranceRepository,
        OrdonnanceVenteRepository venteRepository,
        PrescripteurRepository prescripteurRepository,
        CustomerRepository customerRepository,
        ProduitRepository produitRepository,
        SalesRepository salesRepository,
        SalesLineRepository salesLineRepository,
        ProduitRefSpecialiteRepository produitRefSpecialiteRepository,
        CustomerTraitementChroniqueRepository traitementChroniqueRepository,
        ProduitDciRepository produitDciRepository,
        VentePrescripteurRepository ventePrescripteurRepository,
        UserService userService
    ) {
        this.ordonnanceRepository = ordonnanceRepository;
        this.delivranceRepository = delivranceRepository;
        this.venteRepository = venteRepository;
        this.prescripteurRepository = prescripteurRepository;
        this.customerRepository = customerRepository;
        this.produitRepository = produitRepository;
        this.salesRepository = salesRepository;
        this.salesLineRepository = salesLineRepository;
        this.produitRefSpecialiteRepository = produitRefSpecialiteRepository;
        this.traitementChroniqueRepository = traitementChroniqueRepository;
        this.produitDciRepository = produitDciRepository;
        this.ventePrescripteurRepository = ventePrescripteurRepository;
        this.userService = userService;
    }

    public OrdonnanceDTO creer(OrdonnanceCreationDTO dto) {
        Customer client = customerRepository.findById(dto.customerId()).orElseThrow(() -> new GenericError("Client introuvable", "customerNotFound"));
        Prescripteur prescripteur = dto.prescripteurId() == null
            ? null
            : prescripteurRepository.findById(dto.prescripteurId()).orElseThrow(() -> new GenericError("Prescripteur introuvable", "prescripteurIntrouvable"));
        if (dto.dateFinValidite() != null && dto.dateFinValidite().isBefore(dto.datePrescription())) {
            throw new GenericError("La fin de validité précède la date de prescription.", "ordonnanceValiditeIncoherente");
        }
        Ordonnance ordonnance = new Ordonnance()
            .setCustomer(client)
            .setPrescripteur(prescripteur)
            .setDatePrescription(dto.datePrescription())
            .setRenouvellements(dto.renouvellements())
            .setDateFinValidite(dto.dateFinValidite())
            .setNote(StringUtils.hasText(dto.note()) ? dto.note().trim() : null)
            .setCreatedBy(userService.getUser());
        int rang = 1;
        for (OrdonnanceLigneCreationDTO l : dto.lignes()) {
            if (l.produitId() == null && !StringUtils.hasText(l.texteLu())) {
                throw new GenericError("Chaque ligne désigne un produit ou un texte.", "ordonnanceLigneSansDesignation");
            }
            ordonnance.ajouterLigne(
                new OrdonnanceLigne()
                    .setRang(rang++)
                    .setTexteLu(StringUtils.hasText(l.texteLu()) ? l.texteLu().trim() : null)
                    .setProduit(l.produitId() == null ? null : produitRepository.findById(l.produitId()).orElseThrow(() -> new GenericError("Produit introuvable", "produitIntrouvable")))
                    .setPosologie(StringUtils.hasText(l.posologie()) ? l.posologie().trim() : null)
                    .setDureeJours(l.dureeJours())
                    .setQuantitePrescrite(l.quantitePrescrite())
            );
        }
        Ordonnance enregistree = ordonnanceRepository.save(ordonnance);
        actualiserTraitementsChroniques(enregistree);
        return toDto(enregistree);
    }

    /**
     * Une ordonnance plus récente renouvelle le traitement chronique qu'elle couvre (plan §13.6) : un traitement
     * actif du client, désigné par l'un des produits prescrits ou par une DCI de ces produits, prend sa date
     * et sa fin de validité. Jamais l'inverse : une ordonnance plus ancienne n'écrase rien.
     */
    private void actualiserTraitementsChroniques(Ordonnance ordonnance) {
        Set<Integer> produitIds = new HashSet<>();
        ordonnance.getLignes().stream().filter(l -> l.getProduit() != null).forEach(l -> produitIds.add(l.getProduit().getId()));
        if (produitIds.isEmpty()) {
            return;
        }
        Set<Integer> dciIds = new HashSet<>();
        for (ProduitDci pd : produitDciRepository.findAvecDci(produitIds)) {
            dciIds.add(pd.getDci().getId());
        }
        for (CustomerTraitementChronique t : traitementChroniqueRepository.findAllByCustomerIdOrderByActifDescCreatedAtAsc(ordonnance.getCustomer().getId())) {
            boolean couvert = (t.getProduit() != null && produitIds.contains(t.getProduit().getId())) || (t.getDci() != null && dciIds.contains(t.getDci().getId()));
            boolean plusRecente = t.getDateOrdonnance() == null || ordonnance.getDatePrescription().isAfter(t.getDateOrdonnance());
            if (t.isActif() && couvert && plusRecente) {
                traitementChroniqueRepository.save(t.setDateOrdonnance(ordonnance.getDatePrescription()).setDateFinOrdonnance(ordonnance.getDateFinValidite()));
            }
        }
    }

    @Transactional(readOnly = true)
    public OrdonnanceDTO detail(Integer id) {
        return toDto(charger(id));
    }

    /** Ordonnances d'un client, les plus récentes d'abord ; {@code statut} nul = toutes. */
    @Transactional(readOnly = true)
    public List<OrdonnanceDTO> duClient(Integer customerId, StatutOrdonnance statut) {
        List<OrdonnanceDTO> toutes = ordonnanceRepository.findByCustomerIdOrderByDatePrescriptionDescIdDesc(customerId).stream().map(this::toDto).toList();
        return statut == null ? toutes : toutes.stream().filter(o -> o.statut() == statut).toList();
    }

    /** Clôture (ou réouverture) décidée par une personne. */
    public OrdonnanceDTO cloturer(Integer id, boolean cloturee) {
        return toDto(ordonnanceRepository.save(charger(id).setCloturee(cloturee)));
    }

    /** Rattache une vente à une ordonnance EN_COURS : l'ordonnance alimente le panier, elle ne le remplace pas. */
    public OrdonnanceDTO rattacherVente(Integer ordonnanceId, Long salesId, LocalDate salesDate) {
        Ordonnance ordonnance = charger(ordonnanceId);
        if (toDto(ordonnance).statut() != StatutOrdonnance.EN_COURS) {
            throw new GenericError("Cette ordonnance n'est plus en cours : elle ne se reprend pas.", "ordonnanceNonEnCours");
        }
        if (!salesRepository.existsById(new SaleId(salesId, salesDate))) {
            throw new GenericError("Vente introuvable", "venteIntrouvable");
        }
        OrdonnanceVente.Id cle = new OrdonnanceVente.Id(ordonnanceId, salesId, salesDate);
        if (!venteRepository.existsById(cle)) {
            venteRepository.save(new OrdonnanceVente(ordonnanceId, salesId, salesDate));
        }
        // Une vente rattachée à une ordonnance est une vente sur ordonnance : « conseil » ne s'applique plus (DEPOT reste).
        salesRepository.changerTypePrescription(salesId, salesDate, TypePrescription.CONSEIL, TypePrescription.PRESCRIPTION);
        return toDto(ordonnance);
    }

    /** Rattache une ligne de vente à une ligne d'ordonnance : le produit délivré peut différer du prescrit (générique). */
    public OrdonnanceDTO lierLigneDeVente(Integer ordonnanceId, Integer ligneId, Long salesLineId, LocalDate salesLineDate) {
        Ordonnance ordonnance = charger(ordonnanceId);
        if (ordonnance.getLignes().stream().noneMatch(l -> l.getId().equals(ligneId))) {
            throw new GenericError("Cette ligne n'appartient pas à l'ordonnance.", "ordonnanceLigneIntrouvable");
        }
        // La vente de la ligne doit d'abord être rattachée à l'ordonnance.
        if (!venteRepository.existsLigneDeVenteRattachee(ordonnanceId, salesLineId, salesLineDate)) {
            throw new GenericError("La vente de cette ligne n'est pas rattachée à l'ordonnance.", "venteNonRattachee");
        }
        if (!delivranceRepository.existsByLigneIdAndSalesLineIdAndSalesLineDate(ligneId, salesLineId, salesLineDate)) {
            delivranceRepository.save(new OrdonnanceDelivrance(ligneId, salesLineId, salesLineDate));
        }
        return toDto(ordonnance);
    }

    /**
     * Rattache la vente puis lie chacune de ses lignes à une ligne prescrite : d'abord le MÊME produit,
     * à défaut un produit du MÊME GROUPE GÉNÉRIQUE du référentiel (spécialité retenue de confiance) —
     * le générique délivré à la place du prescrit. Une ligne n'est liée qu'à une ligne prescrite qui a
     * encore un reste à délivrer ; sans correspondance sûre, elle reste non liée. Relançable.
     */
    public AppariementDTO apparierVente(Integer ordonnanceId, Long salesId, LocalDate salesDate) {
        OrdonnanceDTO avant = rattacherVente(ordonnanceId, salesId, salesDate);
        List<SalesLine> lignesDeVente = salesLineRepository.findBySalesIdAndSalesSaleDateOrderByProduitLibelle(salesId, salesDate);

        Set<Integer> produits = new HashSet<>();
        avant.lignes().stream().map(OrdonnanceLigneDTO::produitId).filter(java.util.Objects::nonNull).forEach(produits::add);
        lignesDeVente.forEach(l -> produits.add(l.getProduit().getId()));
        Map<Integer, Integer> groupes = new HashMap<>();
        if (!produits.isEmpty()) {
            for (ProduitGroupeDTO g : produitRefSpecialiteRepository.findGroupes(produits, List.of(DecisionRapprochement.AUTO, DecisionRapprochement.VALIDE))) {
                groupes.put(g.produitId(), g.groupeId());
            }
        }

        // Reste à délivrer par ligne prescrite, décompté au fil des lignes de vente pour ne pas sur-allouer.
        Map<Integer, Integer> reste = new HashMap<>();
        avant.lignes().forEach(l -> reste.put(l.id(), l.resteADelivrer()));
        List<Integer> ligneIds = avant.lignes().stream().map(OrdonnanceLigneDTO::id).toList();

        int liees = 0;
        int generiques = 0;
        for (SalesLine ligneVente : lignesDeVente) {
            Integer produitVendu = ligneVente.getProduit().getId();
            Long ligneVenteId = Objects.requireNonNull(ligneVente.getId()).getId();
            if (
                ligneVente.getQuantitySold() == null ||
                ligneVente.getQuantitySold() <= 0 ||
                delivranceRepository.existsByLigneIdInAndSalesLineIdAndSalesLineDate(ligneIds, ligneVenteId, ligneVente.getSaleDate())
            ) {
                continue;
            }
            Integer groupeVendu = groupes.get(produitVendu);
            OrdonnanceLigneDTO cible = avant
                .lignes()
                .stream()
                .filter(l -> reste.get(l.id()) > 0)
                .filter(l -> produitVendu.equals(l.produitId()))
                .findFirst()
                .orElse(null);
            boolean generique = false;
            if (cible == null && groupeVendu != null) {
                cible = avant
                    .lignes()
                    .stream()
                    .filter(l -> reste.get(l.id()) > 0 && l.produitId() != null)
                    .filter(l -> groupeVendu.equals(groupes.get(l.produitId())))
                    .findFirst()
                    .orElse(null);
                generique = cible != null;
            }
            if (cible == null) {
                continue;
            }
            delivranceRepository.save(new OrdonnanceDelivrance(cible.id(), ligneVenteId, ligneVente.getSaleDate()));
            reste.merge(cible.id(), -ligneVente.getQuantitySold(), Integer::sum);
            liees++;
            if (generique) {
                generiques++;
            }
        }
        return new AppariementDTO(detail(ordonnanceId), liees, generiques);
    }

    /**
     * Déclare le prescripteur d'une vente qui n'a pas (encore) d'ordonnance : il lève l'exigence des
     * produits sur ordonnance à la clôture. Redéclarer le remplace.
     */
    public void definirPrescripteurDeVente(Long salesId, LocalDate salesDate, Integer prescripteurId) {
        if (!salesRepository.existsById(new SaleId(salesId, salesDate))) {
            throw new GenericError("Vente introuvable", "venteIntrouvable");
        }
        if (prescripteurRepository.findById(prescripteurId).filter(Prescripteur::isActif).isEmpty()) {
            throw new GenericError("Prescripteur introuvable ou désactivé", "prescripteurIntrouvable");
        }
        ventePrescripteurRepository.save(new VentePrescripteur(salesId, salesDate, prescripteurId, userService.getUser().getId()));
    }

    /** Détache une vente (panier abandonné, mauvaise ordonnance) et les délivrances qu'elle portait. */
    public OrdonnanceDTO detacherVente(Integer ordonnanceId, Long salesId, LocalDate salesDate) {
        charger(ordonnanceId);
        delivranceRepository.supprimerDeLaVente(ordonnanceId, salesId, salesDate);
        OrdonnanceVente.Id cle = new OrdonnanceVente.Id(ordonnanceId, salesId, salesDate);
        if (venteRepository.existsById(cle)) {
            venteRepository.deleteById(cle);
        }
        return toDto(charger(ordonnanceId));
    }

    private Ordonnance charger(Integer id) {
        return ordonnanceRepository.findById(id).orElseThrow(() -> new GenericError("Ordonnance introuvable", "ordonnanceIntrouvable"));
    }

    private OrdonnanceDTO toDto(Ordonnance o) {
        List<Integer> ligneIds = o.getLignes().stream().map(OrdonnanceLigne::getId).toList();
        Map<Integer, Integer> delivre = new HashMap<>();
        if (!ligneIds.isEmpty()) {
            for (QuantiteDelivreeDTO q : delivranceRepository.findQuantitesDelivrees(ligneIds, SalesStatut.CLOSED)) {
                delivre.put(q.ligneId(), q.quantite().intValue());
            }
            // Un retour client rend sa quantité à l'ordonnance (la ligne de vente, elle, reste inchangée).
            for (QuantiteDelivreeDTO q : delivranceRepository.findQuantitesRetournees(ligneIds)) {
                delivre.merge(q.ligneId(), -q.quantite().intValue(), Integer::sum);
            }
            delivre.replaceAll((ligne, quantite) -> Math.max(0, quantite));
        }
        int renouvellements = o.getRenouvellements();
        List<OrdonnanceLigneDTO> lignes = o
            .getLignes()
            .stream()
            .map(l -> {
                int quantiteDelivree = delivre.getOrDefault(l.getId(), 0);
                return new OrdonnanceLigneDTO(
                    l.getId(),
                    l.getRang(),
                    l.getTexteLu(),
                    l.getProduit() == null ? null : l.getProduit().getId(),
                    l.getProduit() == null ? null : l.getProduit().getLibelle(),
                    l.getPosologie(),
                    l.getDureeJours(),
                    l.getQuantitePrescrite(),
                    quantiteDelivree,
                    SuiviOrdonnance.resteADelivrer(l.getQuantitePrescrite(), renouvellements, quantiteDelivree)
                );
            })
            .toList();
        List<LigneSuivieDTO> suivies = lignes.stream().map(l -> new LigneSuivieDTO(l.quantitePrescrite(), l.quantiteDelivree())).toList();
        Prescripteur p = o.getPrescripteur();
        return new OrdonnanceDTO(
            o.getId(),
            o.getCustomer().getId(),
            p == null ? null : p.getId(),
            p == null ? null : (p.getNom() + (p.getPrenom() == null ? "" : " " + p.getPrenom())),
            o.getDatePrescription(),
            o.getSource().name(),
            renouvellements,
            SuiviOrdonnance.renouvellementsRestants(renouvellements, suivies),
            o.getDateFinValidite(),
            SuiviOrdonnance.statut(o.isCloturee(), o.getDateFinValidite(), renouvellements, suivies, LocalDate.now()),
            o.isCloturee(),
            o.getNote(),
            lignes,
            venteRepository.findVentesLiees(o.getId())
        );
    }
}
