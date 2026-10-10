package com.kobe.warehouse.service.pilotage.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.ModeAnnees;
import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import com.kobe.warehouse.service.dto.pilotage.AnalysePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.AnneeCompareeDTO;
import com.kobe.warehouse.service.dto.pilotage.AxeAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.CelluleAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonAnneesDTO;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.ElementAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.IndicateurPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.PointSerieDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteAnneesDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.SerieIndicateurDTO;
import com.kobe.warehouse.service.dto.pilotage.SeriesPilotageDTO;
import com.kobe.warehouse.service.pilotage.AnalysePilotageService;
import com.kobe.warehouse.service.pilotage.ComparaisonAnneesService;
import com.kobe.warehouse.service.pilotage.SeriesPilotageService;
import com.kobe.warehouse.service.report.excel.CsvExportService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Pilotage — exports CSV côté serveur")
class ExportPilotageServiceImplTest {

    private static final RequetePilotageDTO REQUETE = new RequetePilotageDTO(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 10), null, null, null, null, null, null, null);
    private static final ComparaisonPeriodesDTO COMPARAISON = new ComparaisonPeriodesDTO(new PeriodeDTO(REQUETE.du(), REQUETE.au()), null, true);
    private static final IndicateurPilotageDTO CA = IndicateurPilotageDTO.fromIndicateur(IndicateurPilotage.CA_TTC);

    private final SeriesPilotageService seriesPilotageService = mock(SeriesPilotageService.class);
    private final AnalysePilotageService analysePilotageService = mock(AnalysePilotageService.class);
    private final ComparaisonAnneesService comparaisonAnneesService = mock(ComparaisonAnneesService.class);
    private final ExportPilotageServiceImpl service = new ExportPilotageServiceImpl(seriesPilotageService, analysePilotageService, comparaisonAnneesService, new CsvExportService());

    @Test
    @DisplayName("détail par période : libellé de la tranche, la plus récente d'abord, valeurs complètes à la virgule")
    void exporterSeries() {
        List<PointSerieDTO> points = List.of(
            new PointSerieDTO(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), "Sept. 2026", 1_250_000.456, 1_000_000.0, null, null, null),
            new PointSerieDTO(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10), "Oct. 2026 (au 10)", 400_000.0, null, null, null, null)
        );
        when(seriesPilotageService.calculerSeries(REQUETE)).thenReturn(new SeriesPilotageDTO(COMPARAISON, 34, 0, List.of(new SerieIndicateurDTO(CA, null, null, null, null, points, null, null))));

        List<String> lignes = lire(service.exporterSeries(REQUETE));

        assertThat(lignes.getFirst()).isEqualTo("Pilotage — détail par période du 01/09/2026 au 10/10/2026");
        assertThat(lignes.subList(2, lignes.size())).containsExactly(
            "Période;Chiffre d'affaires TTC;Chiffre d'affaires TTC (référence)",
            "Oct. 2026 (au 10);400000;",
            "Sept. 2026;1250000,46;1000000"
        );
    }

    @Test
    @DisplayName("analyse : éléments, autres puis total ; part et contribution")
    void exporterAnalyse() {
        CelluleAnalyseDTO cellule = new CelluleAnalyseDTO(300.0, 200.0, 100.0, 50.0);
        AnalysePilotageDTO analyse = new AnalysePilotageDTO(
            COMPARAISON,
            SourceAnalyse.LIGNES,
            List.of(CA),
            List.of(),
            AxeAnalyseDTO.fromAxe(AxeAnalyse.FAMILLE),
            null,
            List.of(new CelluleAnalyseDTO(400.0, 250.0, 150.0, 60.0)),
            List.of(new ElementAnalyseDTO("1", "Médicaments", List.of(cellule), 75.0, 66.667)),
            new ElementAnalyseDTO("autres", "Autres", List.of(new CelluleAnalyseDTO(100.0, 50.0, 50.0, 100.0)), 25.0, 33.333),
            2,
            null
        );
        RequeteAnalyseDTO reglage = new RequeteAnalyseDTO(null, null, null, null, null);
        when(analysePilotageService.analyser(any(), any())).thenReturn(analyse);

        List<String> lignes = lire(service.exporterAnalyse(REQUETE, reglage));

        assertThat(lignes.subList(2, lignes.size())).containsExactly(
            AxeAnalyse.FAMILLE.getLibelle() + ";Chiffre d'affaires TTC;Chiffre d'affaires TTC (référence);Part (%);Contribution à l'écart (%)",
            "Médicaments;300;200;75;66,67",
            "Autres;100;50;25;33,33",
            "Total;400;250;100;"
        );
    }

    @Test
    @DisplayName("années : un mois par ligne, le mois en cours dit « au », l'année en cours dite « en cours »")
    void exporterAnnees() {
        List<CelluleAnalyseDTO> mois = Collections.nCopies(12, new CelluleAnalyseDTO(10.0, null, null, null));
        ComparaisonAnneesDTO comparaison = new ComparaisonAnneesDTO(
            CA,
            ModeAnnees.MENSUEL,
            false,
            LocalDate.of(2026, 10, 10),
            List.of(new AnneeCompareeDTO(2025, true, mois, List.of(), new CelluleAnalyseDTO(120.0, null, null, null)), new AnneeCompareeDTO(2026, false, mois, List.of(), new CelluleAnalyseDTO(100.0, null, null, null))),
            List.of(),
            null,
            null,
            null,
            null,
            List.of(),
            null,
            null
        );
        when(comparaisonAnneesService.comparerAnnees(any(), any())).thenReturn(comparaison);

        List<String> lignes = lire(service.exporterAnnees(new RequeteAnneesDTO(null, null, null, null, null, null)));

        assertThat(lignes.get(2)).isEqualTo("Mois;2025;2026 (en cours)");
        assertThat(lignes.get(3)).isEqualTo("Janv.;10;10");
        assertThat(lignes.get(12)).isEqualTo("Oct. (au 10);10;10");
        assertThat(lignes.getLast()).isEqualTo("Année;120;100");
    }

    /** Lignes du fichier, BOM retiré. */
    private static List<String> lire(byte[] csv) {
        return new String(csv, StandardCharsets.UTF_8).replace("﻿", "").lines().toList();
    }
}
