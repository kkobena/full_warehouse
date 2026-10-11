package com.kobe.warehouse.service.pilotage.impl;

import static com.kobe.warehouse.domain.enumeration.IndicateurPilotage.*;

import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.TypeComparaison;
import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.ContributionDTO;
import com.kobe.warehouse.service.dto.pilotage.EcartsPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.IndicateurPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.PointSerieDTO;
import com.kobe.warehouse.service.dto.pilotage.ProjectionObjectifDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.SerieIndicateurDTO;
import com.kobe.warehouse.service.dto.pilotage.SeriesPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.FormatGraphe;
import com.kobe.warehouse.service.dto.pilotage.rapport.LigneRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.LigneRapportDTO.CelluleRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.MontantRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.TableauDeBordRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.TableauRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.TuileRapportDTO;
import com.kobe.warehouse.service.pilotage.AlertesPilotageService;
import com.kobe.warehouse.service.pilotage.EcartsPilotageService;
import com.kobe.warehouse.service.pilotage.RapportCourbesService;
import com.kobe.warehouse.service.pilotage.RapportTableauDeBordService;
import com.kobe.warehouse.service.pilotage.SeriesPilotageService;
import com.kobe.warehouse.service.pilotage.calcul.FormatRapport;
import com.kobe.warehouse.service.pilotage.calcul.HorizonCourbe;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RapportTableauDeBordServiceImpl implements RapportTableauDeBordService {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int CONTRIBUTIONS = 5;

    /** Mêmes tuiles que l'écran : un indicateur principal, puis celui qui l'éclaire. */
    private static final IndicateurPilotage[][] TUILES = {
        { CA_TTC, CA_NET },
        { NB_VENTES, FREQUENTATION },
        { PANIER_MOYEN, ARTICLES_PAR_VENTE },
        { MARGE_BRUTE, TAUX_MARGE },
        { REMISES, TAUX_REMISE },
        { ACHATS_TTC, RATIO_VENTES_ACHATS },
        { PART_TIERS_PAYANT },
        { ENCAISSEMENTS },
    };
    private static final List<IndicateurPilotage> COLONNES = List.of(
        CA_TTC,
        MARGE_BRUTE,
        TAUX_MARGE,
        REMISES,
        ACHATS_TTC,
        NB_VENTES,
        PANIER_MOYEN,
        PART_TIERS_PAYANT
    );

    private final SeriesPilotageService seriesPilotageService;
    private final EcartsPilotageService ecartsPilotageService;
    private final AlertesPilotageService alertesPilotageService;
    private final RapportCourbesService rapportCourbesService;

    public RapportTableauDeBordServiceImpl(
        SeriesPilotageService seriesPilotageService,
        EcartsPilotageService ecartsPilotageService,
        AlertesPilotageService alertesPilotageService,
        RapportCourbesService rapportCourbesService
    ) {
        this.seriesPilotageService = seriesPilotageService;
        this.ecartsPilotageService = ecartsPilotageService;
        this.alertesPilotageService = alertesPilotageService;
        this.rapportCourbesService = rapportCourbesService;
    }

    @Override
    public TableauDeBordRapportDTO construireTableauDeBord(RequetePilotageDTO requete) {
        SeriesPilotageDTO series = seriesPilotageService.calculerSeries(requete.lireAvecIndicateurs(lireIndicateurs()));
        Map<IndicateurPilotage, SerieIndicateurDTO> parIndicateur = new EnumMap<>(IndicateurPilotage.class);
        for (SerieIndicateurDTO serie : series.series()) {
            parIndicateur.put(IndicateurPilotage.valueOf(serie.indicateur().code()), serie);
        }
        boolean contreObjectif = requete.comparaison() == TypeComparaison.OBJECTIF;
        EcartsPilotageDTO ecarts = ecartsPilotageService.expliquerEcarts(requete, CONTRIBUTIONS);
        List<SerieIndicateurDTO> colonnes = COLONNES.stream().map(parIndicateur::get).filter(Objects::nonNull).toList();
        return new TableauDeBordRapportDTO(
            libellerContexte(series.comparaison(), contreObjectif),
            alertesPilotageService.listerAlertes(LocalDate.now()),
            construireTuiles(parIndicateur, contreObjectif),
            "Chiffre d'affaires TTC — " + HorizonCourbe.libellerHorizon(requete.granularite()),
            rapportCourbesService.dessinerHistorique(requete, CA_TTC, contreObjectif, FormatGraphe.NORMAL),
            ecarts.effetFrequentation() == null ? null : FormatRapport.formaterEcart("Écart de CA", ecarts.ca() - ecarts.caReference()),
            lireEffets(ecarts),
            lireContributions(ecarts.hausses()),
            lireContributions(ecarts.baisses()),
            new TableauRapportDTO(
                "Période",
                colonnes.stream().map(serie -> serie.indicateur().libelle()).toList(),
                construireLignes(colonnes, parIndicateur.get(CA_TTC))
            )
        );
    }

    private static List<IndicateurPilotage> lireIndicateurs() {
        Set<IndicateurPilotage> indicateurs = new LinkedHashSet<>();
        for (IndicateurPilotage[] tuile : TUILES) {
            indicateurs.addAll(List.of(tuile));
        }
        indicateurs.addAll(COLONNES);
        return List.copyOf(indicateurs);
    }

    private static String libellerContexte(ComparaisonPeriodesDTO comparaison, boolean contreObjectif) {
        String periode = libellerPeriode(comparaison.periode()) + (comparaison.aDate() ? " (à date)" : "");
        if (contreObjectif) {
            return periode + " comparé aux objectifs (un mois entamé compte au prorata de ses jours).";
        }
        return comparaison.reference() == null ? periode : periode + " comparé à " + libellerPeriode(comparaison.reference());
    }

    private static String libellerPeriode(PeriodeDTO periode) {
        return "du " + periode.du().format(JOUR) + " au " + periode.au().format(JOUR);
    }

    private static List<TuileRapportDTO> construireTuiles(Map<IndicateurPilotage, SerieIndicateurDTO> parIndicateur, boolean contreObjectif) {
        List<TuileRapportDTO> tuiles = new ArrayList<>(TUILES.length);
        for (IndicateurPilotage[] tuile : TUILES) {
            SerieIndicateurDTO principal = parIndicateur.get(tuile[0]);
            if (principal != null) {
                SerieIndicateurDTO secondaire = tuile.length > 1 ? parIndicateur.get(tuile[1]) : null;
                tuiles.add(construireTuile(principal, secondaire, contreObjectif));
            }
        }
        return tuiles;
    }

    private static TuileRapportDTO construireTuile(SerieIndicateurDTO principal, SerieIndicateurDTO secondaire, boolean contreObjectif) {
        IndicateurPilotageDTO indicateur = principal.indicateur();
        UniteIndicateur unite = indicateur.unite();
        List<String> complements = new ArrayList<>(3);
        if (secondaire != null) {
            complements.add(secondaire.indicateur().libelle() + " : " + FormatRapport.formaterValeur(secondaire.valeur(), secondaire.indicateur().unite()));
        }
        if (principal.objectif() != null && !contreObjectif) {
            complements.add("Objectif : " + FormatRapport.formaterValeur(principal.objectif(), unite));
        }
        ProjectionObjectifDTO projection = principal.projection();
        if (projection != null && (CA_TTC.name().equals(indicateur.code()) || principal.objectif() != null)) {
            complements.add(libellerProjection(projection, unite));
        }
        return new TuileRapportDTO(
            indicateur.libelle(),
            FormatRapport.formaterValeur(principal.valeur(), unite),
            FormatRapport.formaterVariation(principal.ecart(), principal.ecartPct(), unite, indicateur.sensFavorable()),
            complements
        );
    }

    private static String libellerProjection(ProjectionObjectifDTO projection, UniteIndicateur unite) {
        String texte = "Fin de mois projetée : " + FormatRapport.formaterValeur(projection.projection(), unite);
        if (projection.atteinteProjetee() == null) {
            return texte;
        }
        return texte + " (" + FormatRapport.formaterValeur(projection.atteinteProjetee(), UniteIndicateur.POURCENTAGE) + " de l'objectif)";
    }

    private static List<MontantRapportDTO> lireEffets(EcartsPilotageDTO ecarts) {
        if (ecarts.effetFrequentation() == null) {
            return List.of();
        }
        return List.of(
            FormatRapport.formaterEcart("Fréquentation (nombre de ventes)", ecarts.effetFrequentation()),
            FormatRapport.formaterEcart("Articles par vente", Objects.requireNonNullElse(ecarts.effetArticles(), 0L)),
            FormatRapport.formaterEcart("Prix moyen d'un article", Objects.requireNonNullElse(ecarts.effetPrix(), 0L))
        );
    }

    private static List<MontantRapportDTO> lireContributions(List<ContributionDTO> contributions) {
        return contributions
            .stream()
            .map(contribution -> FormatRapport.formaterEcart(contribution.libelle() + " (" + contribution.libelleAxe() + ")", contribution.ecart()))
            .toList();
    }

    /** De la tranche la plus récente à la plus ancienne, comme à l'écran. */
    private static List<LigneRapportDTO> construireLignes(List<SerieIndicateurDTO> colonnes, SerieIndicateurDTO caTtc) {
        if (caTtc == null) {
            return List.of();
        }
        int tranches = caTtc.points().size();
        List<LigneRapportDTO> lignes = new ArrayList<>(tranches);
        for (int rang = tranches - 1; rang >= 0; rang--) {
            List<CelluleRapportDTO> cellules = new ArrayList<>(colonnes.size());
            for (SerieIndicateurDTO serie : colonnes) {
                cellules.add(construireCellule(serie, rang));
            }
            lignes.add(new LigneRapportDTO(caTtc.points().get(rang).libelle(), cellules));
        }
        return lignes;
    }

    private static CelluleRapportDTO construireCellule(SerieIndicateurDTO serie, int rang) {
        if (rang >= serie.points().size()) {
            return new CelluleRapportDTO("—", null);
        }
        PointSerieDTO point = serie.points().get(rang);
        IndicateurPilotageDTO indicateur = serie.indicateur();
        return new CelluleRapportDTO(
            FormatRapport.formaterValeur(point.valeur(), indicateur.unite()),
            FormatRapport.formaterVariation(point.ecart(), point.ecartPct(), indicateur.unite(), indicateur.sensFavorable())
        );
    }
}
