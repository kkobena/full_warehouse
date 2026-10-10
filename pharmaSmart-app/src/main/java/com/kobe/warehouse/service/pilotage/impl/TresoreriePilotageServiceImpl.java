package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.ModeClotureAvoir;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import com.kobe.warehouse.repository.PilotageTresorerieRepository;
import com.kobe.warehouse.service.dto.pilotage.CaissierEncaissementDTO;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.ComptageDTO;
import com.kobe.warehouse.service.dto.pilotage.DelaiObserveDTO;
import com.kobe.warehouse.service.dto.pilotage.DifferesAgeDTO;
import com.kobe.warehouse.service.dto.pilotage.DifferesTresorerieDTO;
import com.kobe.warehouse.service.dto.pilotage.EncaissementModeJourDTO;
import com.kobe.warehouse.service.dto.pilotage.EncaissementsTresorerieDTO;
import com.kobe.warehouse.service.dto.pilotage.FactureOrganismeDTO;
import com.kobe.warehouse.service.dto.pilotage.ModeEncaissementDTO;
import com.kobe.warehouse.service.dto.pilotage.OrganismeTresorerieDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.SerieModeDTO;
import com.kobe.warehouse.service.dto.pilotage.TiersPayantTresorerieDTO;
import com.kobe.warehouse.service.dto.pilotage.TrancheMontantDTO;
import com.kobe.warehouse.service.pilotage.DictionnaireIndicateursService;
import com.kobe.warehouse.service.pilotage.PeriodeComparaisonService;
import com.kobe.warehouse.service.pilotage.TresoreriePilotageService;
import com.kobe.warehouse.service.pilotage.calcul.CreancesTiersPayant;
import com.kobe.warehouse.service.pilotage.calcul.DecoupagePeriode;
import com.kobe.warehouse.service.pilotage.calcul.LibellesPilotage;
import com.kobe.warehouse.service.pilotage.calcul.ModeEncaissement;
import com.kobe.warehouse.service.pilotage.calcul.OrganismeCreances;
import com.kobe.warehouse.service.pilotage.calcul.Variations;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trésorerie. Encaissements : règlements de ventes, de différés et de factures tiers payant (agrégat des encaissements).
 * Tiers payant : mêmes définitions que le vieillissement des créances ; délai retenu pour chaque organisme, dans cet ordre
 * (décision du 2026-10-09) : délai observé s'il a assez d'historique, sinon délai contractuel de son groupe, sinon délai par
 * défaut de l'officine.
 */
@Service
@Transactional(readOnly = true)
public class TresoreriePilotageServiceImpl implements TresoreriePilotageService {

    private static final int CLIENTS_MAX = 20;
    private static final double CENT = 100.0;
    private static final List<InvoiceStatut> NON_SOLDEES = List.of(InvoiceStatut.NOT_PAID, InvoiceStatut.PARTIALLY_PAID);
    private static final List<ModeClotureAvoir> REMBOURSEMENTS = List.of(ModeClotureAvoir.REMBOURSEMENT_ESPECES, ModeClotureAvoir.REMBOURSEMENT_CB);

    private final PeriodeComparaisonService periodeComparaisonService;
    private final DictionnaireIndicateursService dictionnaireIndicateursService;
    private final PilotageTresorerieRepository pilotageTresorerieRepository;
    private final AppConfigurationService appConfigurationService;

    public TresoreriePilotageServiceImpl(
        PeriodeComparaisonService periodeComparaisonService,
        DictionnaireIndicateursService dictionnaireIndicateursService,
        PilotageTresorerieRepository pilotageTresorerieRepository,
        AppConfigurationService appConfigurationService
    ) {
        this.periodeComparaisonService = periodeComparaisonService;
        this.dictionnaireIndicateursService = dictionnaireIndicateursService;
        this.pilotageTresorerieRepository = pilotageTresorerieRepository;
        this.appConfigurationService = appConfigurationService;
    }

    @Override
    public EncaissementsTresorerieDTO analyserEncaissements(RequetePilotageDTO requete) {
        ComparaisonPeriodesDTO comparaison = periodeComparaisonService.comparer(requete, LocalDate.now());
        PeriodeDTO periode = comparaison.periode();
        List<PeriodeDTO> tranches = DecoupagePeriode.decouper(periode, requete.granularite());
        NavigableMap<LocalDate, Integer> rangs = new TreeMap<>();
        List<String> libelles = new ArrayList<>(tranches.size());
        for (int rang = 0; rang < tranches.size(); rang++) {
            rangs.put(tranches.get(rang).du(), rang);
            libelles.add(LibellesPilotage.libellerTranche(tranches.get(rang), requete.granularite()));
        }

        Map<String, ModeEncaissement> modes = new LinkedHashMap<>();
        long total = 0;
        for (EncaissementModeJourDTO jour : pilotageTresorerieRepository.sommerEncaissementsParJourEtMode(periode.du(), periode.au(), LecteurMesuresPilotage.TYPES_ENCAISSEMENT)) {
            modes.computeIfAbsent(jour.mode(), cle -> new ModeEncaissement(jour.libelle(), tranches.size())).ajouter(rangs.floorEntry(jour.jour()).getValue(), jour.montant());
            total += jour.montant();
        }
        Map<String, Long> reference = new HashMap<>();
        long totalReference = 0;
        if (comparaison.reference() != null) {
            PeriodeDTO lue = comparaison.reference();
            for (EncaissementModeJourDTO jour : pilotageTresorerieRepository.sommerEncaissementsParJourEtMode(lue.du(), lue.au(), LecteurMesuresPilotage.TYPES_ENCAISSEMENT)) {
                reference.merge(jour.mode(), jour.montant(), Long::sum);
                totalReference += jour.montant();
            }
        }

        boolean avecReference = comparaison.reference() != null;
        long totalPeriode = total;
        List<Map.Entry<String, ModeEncaissement>> ordonnes = modes.entrySet().stream().sorted(Comparator.comparingLong((Map.Entry<String, ModeEncaissement> mode) -> mode.getValue().total()).reversed()).toList();
        return new EncaissementsTresorerieDTO(
            comparaison,
            Variations.cellule(UniteIndicateur.MONTANT, (double) total, avecReference ? (double) totalReference : null),
            ordonnes
                .stream()
                .map(mode ->
                    new ModeEncaissementDTO(
                        mode.getKey(),
                        mode.getValue().libelle(),
                        Variations.cellule(UniteIndicateur.MONTANT, (double) mode.getValue().total(), avecReference ? (double) reference.getOrDefault(mode.getKey(), 0L) : null),
                        Variations.part((double) mode.getValue().total(), (double) totalPeriode)
                    )
                )
                .toList(),
            libelles,
            ordonnes.stream().map(mode -> new SerieModeDTO(mode.getValue().libelle(), Arrays.stream(mode.getValue().parTranche()).boxed().toList())).toList(),
            voitLEquipe() ? listerCaissiers(periode) : null
        );
    }

    @Override
    public TiersPayantTresorerieDTO analyserTiersPayant(RequetePilotageDTO requete, LocalDate aujourdhui) {
        ComparaisonPeriodesDTO comparaison = periodeComparaisonService.comparer(requete, aujourdhui);
        Map<String, FactureOrganismeDTO> facturation = new LinkedHashMap<>();
        long facture = 0;
        long regle = 0;
        for (FactureOrganismeDTO organisme : pilotageTresorerieRepository.sommerFacturationParOrganisme(comparaison.periode().du(), comparaison.periode().au())) {
            facturation.put(organisme.cle(), organisme);
            facture += organisme.facture();
            regle += organisme.regle();
        }
        Long factureReference = null;
        Long regleReference = null;
        if (comparaison.reference() != null) {
            factureReference = 0L;
            regleReference = 0L;
            for (FactureOrganismeDTO organisme : pilotageTresorerieRepository.sommerFacturationParOrganisme(comparaison.reference().du(), comparaison.reference().au())) {
                factureReference += organisme.facture();
                regleReference += organisme.regle();
            }
        }

        Map<String, DelaiObserveDTO> observes = new HashMap<>();
        for (DelaiObserveDTO delai : pilotageTresorerieRepository.listerDelaisObserves(InvoiceStatut.PAID)) {
            observes.put(delai.cle(), delai);
        }
        CreancesTiersPayant creances = CreancesTiersPayant.ranger(
            pilotageTresorerieRepository.listerFacturesNonSoldees(NON_SOLDEES),
            observes,
            appConfigurationService.getDelaiReglement(),
            aujourdhui
        );
        List<OrganismeTresorerieDTO> organismes = creances.lister(facturation);
        return new TiersPayantTresorerieDTO(
            comparaison,
            Variations.cellule(UniteIndicateur.MONTANT, (double) facture, factureReference == null ? null : (double) factureReference),
            Variations.cellule(UniteIndicateur.MONTANT, (double) regle, regleReference == null ? null : (double) regleReference),
            creances.encours(),
            creances.dso(),
            organismes,
            creances.vieillissement(),
            creances.echeancier(aujourdhui),
            concentration(organismes, 3, creances.encours()),
            concentration(organismes, 5, creances.encours()),
            OrganismeCreances.SEUIL_HISTORIQUE
        );
    }

    @Override
    public DifferesTresorerieDTO analyserDifferes(RequetePilotageDTO requete, LocalDate aujourdhui) {
        ComparaisonPeriodesDTO comparaison = periodeComparaisonService.comparer(requete, aujourdhui);
        var officine = CategorieChiffreAffaire.officine();
        DifferesAgeDTO ages = pilotageTresorerieRepository.sommerDifferesParAge(
            aujourdhui.minusDays(30),
            aujourdhui.minusDays(60),
            aujourdhui.minusDays(90),
            SalesStatut.CLOSED,
            officine
        );
        List<TrancheMontantDTO> vieillissement = List.of(
            new TrancheMontantDTO("0 à 30 jours", ages.moins30()),
            new TrancheMontantDTO("31 à 60 jours", ages.de31a60()),
            new TrancheMontantDTO("61 à 90 jours", ages.de61a90()),
            new TrancheMontantDTO("Plus de 90 jours", ages.plus90())
        );
        boolean avecReference = comparaison.reference() != null;
        return new DifferesTresorerieDTO(
            ages.moins30() + ages.de31a60() + ages.de61a90() + ages.plus90(),
            vieillissement,
            pilotageTresorerieRepository.sommerDifferesParClient(SalesStatut.CLOSED, officine, PageRequest.of(0, CLIENTS_MAX)),
            Variations.cellule(
                UniteIndicateur.MONTANT,
                (double) avoirs(comparaison.periode(), false),
                avecReference ? (double) avoirs(comparaison.reference(), false) : null
            ),
            Variations.cellule(
                UniteIndicateur.MONTANT,
                (double) avoirs(comparaison.periode(), true),
                avecReference ? (double) avoirs(comparaison.reference(), true) : null
            )
        );
    }

    private long avoirs(PeriodeDTO periode, boolean rembourses) {
        LocalDateTime debut = periode.du().atStartOfDay();
        LocalDateTime fin = periode.au().plusDays(1).atStartOfDay();
        ComptageDTO avoirs = rembourses
            ? pilotageTresorerieRepository.sommerAvoirsRembourses(debut, fin, REMBOURSEMENTS)
            : pilotageTresorerieRepository.sommerAvoirsEmis(debut, fin);
        return avoirs.montant();
    }

    private List<CaissierEncaissementDTO> listerCaissiers(PeriodeDTO periode) {
        Map<String, Long> sessions = new HashMap<>();
        for (ComptageDTO caissier : pilotageTresorerieRepository.compterSessionsParCaissier(periode.du().atStartOfDay(), periode.au().plusDays(1).atStartOfDay())) {
            sessions.put(caissier.cle(), caissier.nombre());
        }
        return pilotageTresorerieRepository
            .sommerEncaissementsParCaissier(periode.du(), periode.au(), LecteurMesuresPilotage.TYPES_ENCAISSEMENT)
            .stream()
            .sorted(Comparator.comparingLong(ComptageDTO::montant).reversed())
            .map(caissier -> new CaissierEncaissementDTO(caissier.cle(), caissier.libelle(), caissier.montant(), caissier.nombre(), sessions.getOrDefault(caissier.cle(), 0L)))
            .toList();
    }

    private boolean voitLEquipe() {
        return dictionnaireIndicateursService.lireDroitsAccordes().contains(DroitPilotage.CLIENTS_EQUIPE);
    }

    /** Part de l'encours portée par les {@code nombre} premiers organismes (déjà classés par encours décroissant). */
    private static Double concentration(List<OrganismeTresorerieDTO> organismes, int nombre, long encours) {
        long premiers = 0;
        for (int rang = 0; rang < Math.min(nombre, organismes.size()); rang++) {
            premiers += organismes.get(rang).encours();
        }
        return encours == 0 ? null : premiers * CENT / encours;
    }

}
