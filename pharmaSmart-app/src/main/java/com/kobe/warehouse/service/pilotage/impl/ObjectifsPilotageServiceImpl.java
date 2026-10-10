package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.ObjectifPilotage;
import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.repository.ObjectifPilotageRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.pilotage.GrilleObjectifsDTO;
import com.kobe.warehouse.service.dto.pilotage.IndicateurPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneObjectifsDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.ProjectionObjectifDTO;
import com.kobe.warehouse.service.dto.pilotage.SaisieObjectifsDTO;
import com.kobe.warehouse.service.dto.pilotage.SuiviIndicateurDTO;
import com.kobe.warehouse.service.dto.pilotage.SuiviMoisDTO;
import com.kobe.warehouse.service.dto.pilotage.SuiviObjectifsDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.pilotage.DictionnaireIndicateursService;
import com.kobe.warehouse.service.pilotage.ObjectifsPilotageService;
import com.kobe.warehouse.service.pilotage.calcul.CalculateurIndicateurs;
import com.kobe.warehouse.service.pilotage.calcul.DecoupagePeriode;
import com.kobe.warehouse.service.pilotage.calcul.MesuresPilotage;
import com.kobe.warehouse.service.pilotage.calcul.ObjectifsAnnee;
import com.kobe.warehouse.service.pilotage.calcul.SuiviObjectifs;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ObjectifsPilotageServiceImpl implements ObjectifsPilotageService {

    /** CA, marge (en valeur ou en taux) et taux de remise maximal. */
    static final List<IndicateurPilotage> INDICATEURS_OBJECTIFS = List.of(
        IndicateurPilotage.CA_TTC,
        IndicateurPilotage.MARGE_BRUTE,
        IndicateurPilotage.TAUX_MARGE,
        IndicateurPilotage.TAUX_REMISE
    );
    private static final int MOIS_PAR_AN = 12;
    private static final double CENT = 100.0;
    private static final double DIXIEME = 10.0;

    private final ObjectifPilotageRepository objectifPilotageRepository;
    private final LecteurMesuresPilotage lecteurMesuresPilotage;
    private final DictionnaireIndicateursService dictionnaireIndicateursService;
    private final UserService userService;

    public ObjectifsPilotageServiceImpl(
        ObjectifPilotageRepository objectifPilotageRepository,
        LecteurMesuresPilotage lecteurMesuresPilotage,
        DictionnaireIndicateursService dictionnaireIndicateursService,
        UserService userService
    ) {
        this.objectifPilotageRepository = objectifPilotageRepository;
        this.lecteurMesuresPilotage = lecteurMesuresPilotage;
        this.dictionnaireIndicateursService = dictionnaireIndicateursService;
        this.userService = userService;
    }

    @Override
    public GrilleObjectifsDTO lireGrille(int annee, LocalDate aujourdhui) {
        ObjectifsAnnee objectifs = ObjectifsAnnee.ranger(objectifPilotageRepository.listerParAnnee(annee));
        List<LigneObjectifsDTO> lignes = listerIndicateurs()
            .stream()
            .map(indicateur ->
                new LigneObjectifsDTO(
                    IndicateurPilotageDTO.fromIndicateur(indicateur),
                    objectifs.lireMois(indicateur),
                    objectifs.lireAuteur(indicateur),
                    objectifs.lireDate(indicateur)
                )
            )
            .toList();
        return new GrilleObjectifsDTO(annee, compterMoisClos(annee, aujourdhui), lignes);
    }

    @Override
    @Transactional
    public GrilleObjectifsDTO enregistrer(SaisieObjectifsDTO saisie, LocalDate aujourdhui) {
        verifierIndicateur(saisie.indicateur());
        Map<Integer, ObjectifPilotage> existants = new HashMap<>();
        for (ObjectifPilotage objectif : objectifPilotageRepository.findAllByAnneeAndIndicateur(saisie.annee(), saisie.indicateur())) {
            existants.put(objectif.getMois(), objectif);
        }
        verifierMoisClos(saisie, existants, compterMoisClos(saisie.annee(), aujourdhui));
        AppUser auteur = userService.getUser();
        LocalDateTime maintenant = LocalDateTime.now();
        for (int mois = 1; mois <= MOIS_PAR_AN; mois++) {
            Double valeur = saisie.mois().get(mois - 1);
            ObjectifPilotage existant = existants.get(mois);
            if (valeur == null) {
                if (existant != null) {
                    objectifPilotageRepository.delete(existant);
                }
            } else if (existant == null || existant.getValeur() != valeur) {
                ObjectifPilotage objectif = existant == null
                    ? new ObjectifPilotage().setIndicateur(saisie.indicateur()).setAnnee(saisie.annee()).setMois(mois)
                    : existant;
                objectifPilotageRepository.save(objectif.setValeur(valeur).setModifiePar(auteur).setModifieLe(maintenant));
            }
        }
        return lireGrille(saisie.annee(), aujourdhui);
    }

    /** Mois écoulés de l'année : tous pour une année passée, ceux d'avant le mois en cours pour l'année en cours, aucun ensuite. */
    private int compterMoisClos(int annee, LocalDate aujourdhui) {
        if (annee < aujourdhui.getYear()) {
            return MOIS_PAR_AN;
        }
        return annee == aujourdhui.getYear() ? aujourdhui.getMonthValue() - 1 : 0;
    }

    /** Le suivi d'un mois clos s'est fait contre son objectif : le changer après coup fausserait « tenu » ou « manqué ». */
    private void verifierMoisClos(SaisieObjectifsDTO saisie, Map<Integer, ObjectifPilotage> existants, int moisClos) {
        for (int mois = 1; mois <= moisClos; mois++) {
            Double valeur = saisie.mois().get(mois - 1);
            ObjectifPilotage existant = existants.get(mois);
            boolean change = existant == null ? valeur != null : valeur == null || existant.getValeur() != valeur;
            if (change) {
                throw new GenericError(
                    "Les objectifs d'un mois clos ne se modifient plus (" + Month.of(mois).getDisplayName(TextStyle.FULL, Locale.FRENCH) + " " + saisie.annee() + ")"
                );
            }
        }
    }

    @Override
    public List<Double> proposer(int annee, IndicateurPilotage indicateur, double hausse) {
        verifierIndicateur(indicateur);
        PeriodeDTO anneePrecedente = new PeriodeDTO(LocalDate.of(annee - 1, 1, 1), LocalDate.of(annee - 1, MOIS_PAR_AN, 31));
        List<Double> proposition = new ArrayList<>(MOIS_PAR_AN);
        for (MesuresPilotage mois : lecteurMesuresPilotage.lireParTranche(anneePrecedente, DecoupagePeriode.decouper(anneePrecedente, Granularite.MOIS))) {
            proposition.add(proposerMois(indicateur, CalculateurIndicateurs.calculer(indicateur, mois), hausse));
        }
        return proposition;
    }

    @Override
    public SuiviObjectifsDTO suivre(int annee, LocalDate aujourdhui) {
        ObjectifsAnnee objectifs = ObjectifsAnnee.ranger(objectifPilotageRepository.listerParAnnee(annee));
        LocalDate jusquAu = annee == aujourdhui.getYear() ? aujourdhui : LocalDate.of(annee, MOIS_PAR_AN, 31);
        List<MesuresPilotage> parMois = lireMois(annee, jusquAu, aujourdhui);
        boolean anneeEnCours = annee == aujourdhui.getYear();
        List<MesuresPilotage> moisN1 = anneeEnCours ? lecteurMesuresPilotage.lireMemeMoisN1(aujourdhui) : List.of();

        List<SuiviIndicateurDTO> indicateurs = new ArrayList<>();
        for (IndicateurPilotage indicateur : listerIndicateurs()) {
            ProjectionObjectifDTO projection = anneeEnCours
                ? SuiviObjectifs.projeter(
                      indicateur,
                      objectifs.lireObjectif(indicateur, aujourdhui.getMonthValue()),
                      parMois.getLast(),
                      moisN1.getFirst(),
                      moisN1.size() > 1 ? moisN1.get(1) : MesuresPilotage.AUCUNE,
                      aujourdhui
                  )
                : null;
            indicateurs.add(suivreIndicateur(indicateur, objectifs, parMois, anneeEnCours ? aujourdhui.getMonthValue() : 0, projection));
        }
        return new SuiviObjectifsDTO(annee, jusquAu, indicateurs);
    }

    /** Les 12 mois, le cumul des mois clos (montants seulement) ; {@code moisEnCours} vaut 0 hors de l'année en cours. */
    private static SuiviIndicateurDTO suivreIndicateur(
        IndicateurPilotage indicateur,
        ObjectifsAnnee objectifs,
        List<MesuresPilotage> parMois,
        int moisEnCours,
        ProjectionObjectifDTO projection
    ) {
        List<SuiviMoisDTO> mois = new ArrayList<>(MOIS_PAR_AN);
        double objectifCumule = 0;
        double realiseCumule = 0;
        int dernierMoisCumule = 0;
        for (int numero = 1; numero <= MOIS_PAR_AN; numero++) {
            Double objectif = objectifs.lireObjectif(indicateur, numero);
            Double realise = numero <= parMois.size() ? CalculateurIndicateurs.calculer(indicateur, parMois.get(numero - 1)) : null;
            boolean enCours = numero == moisEnCours;
            mois.add(SuiviObjectifs.suivre(indicateur, numero, objectif, realise, enCours));
            if (objectif != null && realise != null && !enCours) {
                objectifCumule += objectif;
                realiseCumule += realise;
                dernierMoisCumule = numero;
            }
        }
        SuiviMoisDTO cumul = indicateur.estAdditif() && dernierMoisCumule > 0
            ? SuiviObjectifs.suivre(indicateur, dernierMoisCumule, objectifCumule, realiseCumule, false)
            : null;
        return new SuiviIndicateurDTO(IndicateurPilotageDTO.fromIndicateur(indicateur), mois, cumul, projection);
    }

    /** Mois de l'année jusqu'à {@code jusquAu} ; rien pour une année à venir. */
    private List<MesuresPilotage> lireMois(int annee, LocalDate jusquAu, LocalDate aujourdhui) {
        if (annee > aujourdhui.getYear()) {
            return List.of();
        }
        PeriodeDTO periode = new PeriodeDTO(LocalDate.of(annee, 1, 1), jusquAu);
        return lecteurMesuresPilotage.lireParTranche(periode, DecoupagePeriode.decouper(periode, Granularite.MOIS));
    }

    private static Double proposerMois(IndicateurPilotage indicateur, Double valeurN1, double hausse) {
        if (valeurN1 == null) {
            return null;
        }
        return indicateur.estAdditif() ? (double) Math.round(valeurN1 * (1 + hausse / CENT)) : Math.round(valeurN1 * DIXIEME) / DIXIEME;
    }

    private List<IndicateurPilotage> listerIndicateurs() {
        return dictionnaireIndicateursService.filtrerAutorises(INDICATEURS_OBJECTIFS);
    }

    private void verifierIndicateur(IndicateurPilotage indicateur) {
        if (!listerIndicateurs().contains(indicateur)) {
            throw new GenericError("Cet indicateur ne porte pas d'objectif, ou vous n'avez pas le droit de le voir.", "pilotageObjectifIndicateur");
        }
    }
}
