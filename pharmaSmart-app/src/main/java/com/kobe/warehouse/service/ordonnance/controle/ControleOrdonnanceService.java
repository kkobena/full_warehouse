package com.kobe.warehouse.service.ordonnance.controle;

import com.kobe.warehouse.domain.AlerteSanteDerogation;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.CustomerDossierSante;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.pharmacovigilance.ClasseInteraction;
import com.kobe.warehouse.domain.pharmacovigilance.ContreIndication;
import com.kobe.warehouse.domain.pharmacovigilance.DciClasse;
import com.kobe.warehouse.domain.pharmacovigilance.Interaction;
import com.kobe.warehouse.domain.pharmacovigilance.NiveauInteraction;
import com.kobe.warehouse.domain.pharmacovigilance.ReferentielInteractionVersion;
import com.kobe.warehouse.repository.AlerteSanteDerogationRepository;
import com.kobe.warehouse.repository.ClasseInteractionRepository;
import com.kobe.warehouse.repository.ContreIndicationRepository;
import com.kobe.warehouse.repository.CustomerDossierSanteRepository;
import com.kobe.warehouse.repository.CustomerRepository;
import com.kobe.warehouse.repository.DciClasseRepository;
import com.kobe.warehouse.repository.InteractionRepository;
import com.kobe.warehouse.repository.ReferentielInteractionVersionRepository;
import com.kobe.warehouse.service.customer.DerogationAuthorizer;
import com.kobe.warehouse.service.customer.DossierSanteService;
import com.kobe.warehouse.service.dto.controle.AlerteControleDTO;
import com.kobe.warehouse.service.dto.controle.ControleResultatDTO;
import com.kobe.warehouse.service.dto.controle.EntreeControleDTO;
import com.kobe.warehouse.service.dto.controle.MoleculeDTO;
import com.kobe.warehouse.service.dto.controle.PriseEnCompteControleDTO;
import com.kobe.warehouse.service.dto.controle.ProfilPatientDTO;
import com.kobe.warehouse.service.dto.controle.TraitementEnCoursDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Contrôle d'un panier de ventes pour un client identifié : interactions, redondances et
 * contre-indications liées au profil. Il aide, il ne décide pas : aucune alerte ne bloque
 * seule, une contre-indication exige une prise en compte explicite, motivée et tracée.
 */
@Service
@Transactional
public class ControleOrdonnanceService {

    private final MoleculesProduitService moleculesProduitService;
    private final TraitementsEnCoursService traitementsEnCoursService;
    private final InteractionRepository interactionRepository;
    private final ContreIndicationRepository contreIndicationRepository;
    private final DciClasseRepository dciClasseRepository;
    private final ClasseInteractionRepository classeRepository;
    private final ReferentielInteractionVersionRepository versionRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDossierSanteRepository dossierRepository;
    private final AlerteSanteDerogationRepository derogationRepository;
    private final DerogationAuthorizer derogationAuthorizer;
    private final AppConfigurationService appConfigurationService;
    private final EntityManager em;

    public ControleOrdonnanceService(
        MoleculesProduitService moleculesProduitService,
        TraitementsEnCoursService traitementsEnCoursService,
        InteractionRepository interactionRepository,
        ContreIndicationRepository contreIndicationRepository,
        DciClasseRepository dciClasseRepository,
        ClasseInteractionRepository classeRepository,
        ReferentielInteractionVersionRepository versionRepository,
        CustomerRepository customerRepository,
        CustomerDossierSanteRepository dossierRepository,
        AlerteSanteDerogationRepository derogationRepository,
        DerogationAuthorizer derogationAuthorizer,
        AppConfigurationService appConfigurationService,
        EntityManager em
    ) {
        this.moleculesProduitService = moleculesProduitService;
        this.traitementsEnCoursService = traitementsEnCoursService;
        this.interactionRepository = interactionRepository;
        this.contreIndicationRepository = contreIndicationRepository;
        this.dciClasseRepository = dciClasseRepository;
        this.classeRepository = classeRepository;
        this.versionRepository = versionRepository;
        this.customerRepository = customerRepository;
        this.dossierRepository = dossierRepository;
        this.derogationRepository = derogationRepository;
        this.derogationAuthorizer = derogationAuthorizer;
        this.appConfigurationService = appConfigurationService;
        this.em = em;
    }

    @Transactional(readOnly = true)
    public ControleResultatDTO controler(Integer customerId, Collection<Integer> produitIds) {
        Customer client = customerRepository.findById(customerId).orElseThrow(() -> new GenericError("Client introuvable", "customerNotFound"));
        Map<Integer, String> panier = moleculesProduitService.libelles(produitIds);
        Map<Integer, List<MoleculeDTO>> moleculesPanier = moleculesProduitService.molecules(panier.keySet());
        List<TraitementEnCoursDTO> enCours = traitementsEnCoursService.enCours(customerId);

        Set<Integer> dcis = new LinkedHashSet<>();
        moleculesPanier.values().forEach(l -> l.forEach(m -> dcis.add(m.refDciId())));
        enCours.forEach(c -> dcis.add(c.molecule().refDciId()));

        Map<Integer, String> sources = versionRepository
            .findAllByPublieTrueOrderByPublieLeDesc()
            .stream()
            .collect(Collectors.toMap(ReferentielInteractionVersion::getId, v -> v.getSource() + " " + v.getVersion()));

        Map<Integer, Set<Integer>> classesParDci = new HashMap<>();
        Map<Integer, String> classes = new HashMap<>();
        List<Interaction> interactions = List.of();
        List<ContreIndication> contreIndications = List.of();
        if (!dcis.isEmpty() && !sources.isEmpty()) {
            for (DciClasse dc : dciClasseRepository.findByRefDciIds(dcis)) {
                classesParDci.computeIfAbsent(dc.getId().refDciId(), k -> new LinkedHashSet<>()).add(dc.getId().classeId());
            }
            // Seules les classes d'une version publiée comptent, y compris pour les redondances.
            classeRepository
                .findAllById(classesParDci.values().stream().flatMap(Set::stream).collect(Collectors.toSet()))
                .stream()
                .filter((ClasseInteraction c) -> sources.containsKey(c.getVersionId()))
                .forEach(c -> classes.put(c.getId(), c.getLibelle()));
            classesParDci.values().forEach(ids -> ids.retainAll(classes.keySet()));
            interactions = interactionRepository.findEntre(dcis);
            contreIndications = contreIndicationRepository.findPubliees(dcis);
        }

        var dossier = dossierRepository.findById(customerId);
        Integer age = client.getDatNaiss() == null ? null : Period.between(client.getDatNaiss(), LocalDate.now()).getYears();
        ProfilPatientDTO profil = new ProfilPatientDTO(
            age,
            client.getSexe(),
            dossier.map(CustomerDossierSante::isGrossesse).orElse(false),
            dossier.map(CustomerDossierSante::isAllaitement).orElse(false)
        );

        ControleResultatDTO brut = MoteurControle.evaluer(
            new EntreeControleDTO(panier, moleculesPanier, enCours, classesParDci, classes, interactions, contreIndications, sources, profil)
        );
        // Le niveau décide : chaque niveau est paramétré, bloquant (1) ou simple avertissement (0, par défaut).
        Map<NiveauInteraction, Boolean> bloquants = new EnumMap<>(NiveauInteraction.class);
        for (NiveauInteraction niveau : NiveauInteraction.values()) {
            bloquants.put(niveau, appConfigurationService.isControleOrdonnanceBloquant(niveau));
        }
        return new ControleResultatDTO(
            brut.alertes().stream().map(a -> a.bloquantSi(bloquants.get(a.niveau()))).toList(),
            brut.nonControles(),
            brut.referentielPublie(),
            brut.limite()
        );
    }

    /**
     * Prise en compte des alertes du panier. Une alerte d'un niveau paramétré bloquant exige un motif et
     * le droit {@value DossierSanteService#DROIT_DEROGATION} (ou la clé d'un collègue qui le détient) ;
     * les autres sont un simple acquittement, tracé de la même façon.
     */
    public void prendreEnCompte(Integer customerId, PriseEnCompteControleDTO demande) {
        List<AlerteControleDTO> alertes = controler(customerId, demande.produitIds()).alertes();
        if (alertes.isEmpty()) {
            throw new GenericError("Aucune alerte à prendre en compte pour ce panier.", "aucuneAlerteControle");
        }
        boolean bloquante = alertes.stream().anyMatch(AlerteControleDTO::exigeMotif);
        if (bloquante && !StringUtils.hasText(demande.motif())) {
            throw new GenericError("Un motif est exigé pour passer outre une alerte bloquante.", "motifExige");
        }
        AppUser utilisateur = derogationAuthorizer.utilisateurCourant();
        AppUser autorisePar = bloquante
            ? derogationAuthorizer.autoriser(
                DossierSanteService.DROIT_DEROGATION,
                demande.actionAuthorityKey(),
                "la délivrance malgré une alerte du contrôle d'ordonnance",
                "alerteControle"
            )
            : utilisateur;
        String motif = StringUtils.hasText(demande.motif()) ? demande.motif().trim() : "Acquittement";
        Set<Integer> tracesPourProduit = new LinkedHashSet<>();
        for (AlerteControleDTO alerte : alertes) {
            for (Integer produitId : alerte.produitIds()) {
                if (tracesPourProduit.add(produitId)) {
                    String texte = alertes
                        .stream()
                        .filter(a -> a.produitIds().contains(produitId))
                        .map(a -> "[" + a.niveau() + "] " + a.message() + (a.source() == null ? "" : " (" + a.source() + ")"))
                        .collect(Collectors.joining("\n"));
                    derogationRepository.save(
                        new AlerteSanteDerogation()
                            .setCustomerId(customerId)
                            .setProduit(em.getReference(Produit.class, produitId))
                            .setAlertes(texte)
                            .setMotif(motif)
                            .setUser(utilisateur)
                            .setAutorisePar(autorisePar)
                    );
                }
            }
        }
    }
}
