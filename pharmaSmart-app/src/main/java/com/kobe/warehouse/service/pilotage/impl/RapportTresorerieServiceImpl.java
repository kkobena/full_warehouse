package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.SensFavorable;
import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import com.kobe.warehouse.service.dto.pilotage.CaissierEncaissementDTO;
import com.kobe.warehouse.service.dto.pilotage.CelluleAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.ComptageDTO;
import com.kobe.warehouse.service.dto.pilotage.DifferesTresorerieDTO;
import com.kobe.warehouse.service.dto.pilotage.EncaissementsTresorerieDTO;
import com.kobe.warehouse.service.dto.pilotage.ModeEncaissementDTO;
import com.kobe.warehouse.service.dto.pilotage.OrganismeTresorerieDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.SerieModeDTO;
import com.kobe.warehouse.service.dto.pilotage.TiersPayantTresorerieDTO;
import com.kobe.warehouse.service.dto.pilotage.TrancheMontantDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.FormatGraphe;
import com.kobe.warehouse.service.dto.pilotage.rapport.LigneRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.LigneRapportDTO.CelluleRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.SerieGraphiqueDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.SerieGraphiqueDTO.StyleSerie;
import com.kobe.warehouse.service.dto.pilotage.rapport.TableauRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.TresorerieRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.TuileRapportDTO;
import com.kobe.warehouse.service.pilotage.GraphiquePilotageService;
import com.kobe.warehouse.service.pilotage.RapportCourbesService;
import com.kobe.warehouse.service.pilotage.RapportTresorerieService;
import com.kobe.warehouse.service.pilotage.TresoreriePilotageService;
import com.kobe.warehouse.service.pilotage.calcul.HorizonCourbe;
import com.kobe.warehouse.service.pilotage.calcul.TableauxRapport;
import com.kobe.warehouse.service.pilotage.calcul.TuilesRapport;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RapportTresorerieServiceImpl implements RapportTresorerieService {

    private static final int LIGNES_MAXIMUM = 30;
    private static final int MODES_EN_TUILE = 3;
    private static final List<String> COLONNES_MODES = List.of("Encaissé", "Part");
    private static final List<String> COLONNES_CAISSIERS = List.of("Encaissé", "Transactions", "Sessions de caisse");
    private static final List<String> COLONNES_ORGANISMES = List.of(
        "Facturé",
        "Réglé",
        "Recouvrement",
        "Encours",
        "Part",
        "DSO (j)",
        "En retard",
        "Délai retenu"
    );
    private static final List<String> COLONNES_CLIENTS = List.of("Ventes", "Reste dû");

    private final TresoreriePilotageService tresoreriePilotageService;
    private final RapportCourbesService rapportCourbesService;
    private final GraphiquePilotageService graphiquePilotageService;

    public RapportTresorerieServiceImpl(
        TresoreriePilotageService tresoreriePilotageService,
        RapportCourbesService rapportCourbesService,
        GraphiquePilotageService graphiquePilotageService
    ) {
        this.tresoreriePilotageService = tresoreriePilotageService;
        this.rapportCourbesService = rapportCourbesService;
        this.graphiquePilotageService = graphiquePilotageService;
    }

    @Override
    public TresorerieRapportDTO construireTresorerie(RequetePilotageDTO requete) {
        LocalDate aujourdhui = LocalDate.now();
        EncaissementsTresorerieDTO encaissements = tresoreriePilotageService.analyserEncaissements(requete);
        TiersPayantTresorerieDTO tiersPayant = tresoreriePilotageService.analyserTiersPayant(requete, aujourdhui);
        DifferesTresorerieDTO differes = tresoreriePilotageService.analyserDifferes(requete, aujourdhui);
        String horizon = HorizonCourbe.libellerHorizon(requete.granularite());
        return new TresorerieRapportDTO(
            lireTuilesEncaissements(encaissements),
            "Encaissé par mode — " + horizon,
            dessinerEncaissements(requete),
            construireModes(encaissements.modes()),
            encaissements.caissiers() == null ? null : TableauxRapport.limiter(construireCaissiers(encaissements.caissiers()), LIGNES_MAXIMUM),
            lireTuilesTiersPayant(tiersPayant),
            "Part du tiers payant dans le CA — " + horizon,
            rapportCourbesService.dessinerHistorique(requete, IndicateurPilotage.PART_TIERS_PAYANT, false, FormatGraphe.MOITIE),
            "Échéance de chaque facture : délai observé de l'organisme (au moins " +
            tiersPayant.seuilHistorique() +
            " factures réglées), sinon celui de son groupe, sinon celui de l'officine.",
            dessinerTranches(tiersPayant.encaissementsAttendus(), "Attendu"),
            dessinerTranches(tiersPayant.vieillissement(), "Encours"),
            TableauxRapport.limiter(construireOrganismes(tiersPayant.organismes()), LIGNES_MAXIMUM),
            lireTuilesDifferes(differes),
            dessinerTranches(differes.vieillissement(), "Reste dû"),
            TableauxRapport.limiter(construireClients(differes.clients()), LIGNES_MAXIMUM)
        );
    }

    /** Le total, puis les trois premiers modes. */
    private static List<TuileRapportDTO> lireTuilesEncaissements(EncaissementsTresorerieDTO encaissements) {
        List<TuileRapportDTO> tuiles = new ArrayList<>(MODES_EN_TUILE + 1);
        tuiles.add(TuilesRapport.lireTuile("Encaissé", encaissements.total(), UniteIndicateur.MONTANT, SensFavorable.HAUSSE));
        for (ModeEncaissementDTO mode : encaissements.modes().subList(0, Math.min(MODES_EN_TUILE, encaissements.modes().size()))) {
            tuiles.add(TuilesRapport.lireTuile("dont " + mode.libelle(), mode.montant(), UniteIndicateur.MONTANT, SensFavorable.NEUTRE));
        }
        return tuiles;
    }

    /** Encaissé par tranche des 13 dernières, empilé par mode de paiement. */
    private String dessinerEncaissements(RequetePilotageDTO requete) {
        RequetePilotageDTO historique = HorizonCourbe.elargirAuxTreizeTranches(requete, IndicateurPilotage.ENCAISSEMENTS);
        EncaissementsTresorerieDTO encaissements = tresoreriePilotageService.analyserEncaissements(historique);
        if (encaissements.tranches().isEmpty() || encaissements.series().isEmpty()) {
            return null;
        }
        List<SerieGraphiqueDTO> series = new ArrayList<>(encaissements.series().size());
        for (SerieModeDTO mode : encaissements.series()) {
            List<Double> valeurs = mode.valeurs().stream().map(valeur -> valeur == null ? null : valeur.doubleValue()).toList();
            series.add(new SerieGraphiqueDTO(mode.libelle(), valeurs, StyleSerie.PALETTE));
        }
        return graphiquePilotageService.dessinerBarresEmpilees(encaissements.tranches(), series, UniteIndicateur.MONTANT, FormatGraphe.MOITIE);
    }

    /** Une série de barres sur des tranches étiquetées : ancienneté, mois d'échéance. */
    private String dessinerTranches(List<TrancheMontantDTO> tranches, String libelle) {
        if (tranches.isEmpty()) {
            return null;
        }
        List<String> libelles = new ArrayList<>(tranches.size());
        List<Double> montants = new ArrayList<>(tranches.size());
        for (TrancheMontantDTO tranche : tranches) {
            libelles.add(tranche.libelle());
            montants.add((double) tranche.montant());
        }
        SerieGraphiqueDTO serie = new SerieGraphiqueDTO(libelle, montants, StyleSerie.PALETTE);
        return graphiquePilotageService.dessinerBarres(libelles, List.of(serie), UniteIndicateur.MONTANT, FormatGraphe.MOITIE);
    }

    private static TableauRapportDTO construireModes(List<ModeEncaissementDTO> modes) {
        List<LigneRapportDTO> lignes = new ArrayList<>(modes.size());
        for (ModeEncaissementDTO mode : modes) {
            List<CelluleRapportDTO> cellules = List.of(
                TableauxRapport.lireCellule(mode.montant(), UniteIndicateur.MONTANT, SensFavorable.NEUTRE),
                TableauxRapport.lireValeur(mode.part(), UniteIndicateur.POURCENTAGE)
            );
            lignes.add(new LigneRapportDTO(mode.libelle(), cellules));
        }
        return new TableauRapportDTO("Mode", COLONNES_MODES, lignes);
    }

    private static TableauRapportDTO construireCaissiers(List<CaissierEncaissementDTO> caissiers) {
        List<LigneRapportDTO> lignes = new ArrayList<>(caissiers.size());
        for (CaissierEncaissementDTO caissier : caissiers) {
            List<CelluleRapportDTO> cellules = List.of(
                TableauxRapport.lireValeur(caissier.montant(), UniteIndicateur.MONTANT),
                TableauxRapport.lireValeur(caissier.transactions(), UniteIndicateur.NOMBRE),
                TableauxRapport.lireValeur(caissier.sessions(), UniteIndicateur.NOMBRE)
            );
            lignes.add(new LigneRapportDTO(caissier.libelle(), cellules));
        }
        return new TableauRapportDTO("Caissier", COLONNES_CAISSIERS, lignes);
    }

    private static List<TuileRapportDTO> lireTuilesTiersPayant(TiersPayantTresorerieDTO tiersPayant) {
        return List.of(
            TuilesRapport.lireTuile("Facturé", tiersPayant.facture(), UniteIndicateur.MONTANT, SensFavorable.NEUTRE),
            TuilesRapport.lireTuile("Réglé", tiersPayant.regle(), UniteIndicateur.MONTANT, SensFavorable.HAUSSE),
            TuilesRapport.lireTuile("Encours", lireSeule(tiersPayant.encours()), UniteIndicateur.MONTANT, SensFavorable.BAISSE),
            TuilesRapport.lireTuile("DSO (j)", lireSeule(tiersPayant.dso()), UniteIndicateur.JOURS, SensFavorable.BAISSE),
            TuilesRapport.lireTuile(
                "Concentration (3 premiers)",
                lireSeule(tiersPayant.concentrationTrois()),
                UniteIndicateur.POURCENTAGE,
                SensFavorable.BAISSE
            )
        );
    }

    /** Le délai retenu ne s'affiche que s'il a une origine (observé, groupe ou défaut de l'officine). */
    private static TableauRapportDTO construireOrganismes(List<OrganismeTresorerieDTO> organismes) {
        List<LigneRapportDTO> lignes = new ArrayList<>(organismes.size());
        for (OrganismeTresorerieDTO organisme : organismes) {
            List<CelluleRapportDTO> cellules = List.of(
                TableauxRapport.lireValeur(organisme.facture(), UniteIndicateur.MONTANT),
                TableauxRapport.lireValeur(organisme.regle(), UniteIndicateur.MONTANT),
                TableauxRapport.lireValeur(organisme.tauxRecouvrement(), UniteIndicateur.POURCENTAGE),
                TableauxRapport.lireValeur(organisme.encours(), UniteIndicateur.MONTANT),
                TableauxRapport.lireValeur(organisme.partEncours(), UniteIndicateur.POURCENTAGE),
                TableauxRapport.lireValeur(organisme.dso(), UniteIndicateur.JOURS),
                TableauxRapport.lireValeur(organisme.enRetard(), UniteIndicateur.MONTANT),
                new CelluleRapportDTO(organisme.origineDelai() == null ? "—" : organisme.delaiRetenu() + " j", null)
            );
            lignes.add(new LigneRapportDTO(organisme.libelle(), cellules));
        }
        return new TableauRapportDTO("Organisme", COLONNES_ORGANISMES, lignes);
    }

    private static List<TuileRapportDTO> lireTuilesDifferes(DifferesTresorerieDTO differes) {
        return List.of(
            TuilesRapport.lireTuile("Différés à recouvrer", lireSeule(differes.encours()), UniteIndicateur.MONTANT, SensFavorable.BAISSE),
            TuilesRapport.lireTuile("Avoirs émis", differes.avoirsEmis(), UniteIndicateur.MONTANT, SensFavorable.BAISSE),
            TuilesRapport.lireTuile("Avoirs remboursés", differes.avoirsRembourses(), UniteIndicateur.MONTANT, SensFavorable.NEUTRE)
        );
    }

    private static TableauRapportDTO construireClients(List<ComptageDTO> clients) {
        List<LigneRapportDTO> lignes = new ArrayList<>(clients.size());
        for (ComptageDTO client : clients) {
            List<CelluleRapportDTO> cellules = List.of(
                TableauxRapport.lireValeur(client.nombre(), UniteIndicateur.NOMBRE),
                TableauxRapport.lireValeur(client.montant(), UniteIndicateur.MONTANT)
            );
            lignes.add(new LigneRapportDTO(client.libelle(), cellules));
        }
        return new TableauRapportDTO("Client", COLONNES_CLIENTS, lignes);
    }

    private static CelluleAnalyseDTO lireSeule(Number valeur) {
        return new CelluleAnalyseDTO(valeur == null ? null : valeur.doubleValue(), null, null, null);
    }
}
