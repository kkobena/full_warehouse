package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kobe.warehouse.domain.enumeration.ModeAnnees;
import java.time.LocalDate;
import java.util.List;

/**
 * « Comparer les années ». Saisonnalité et croissance annuelle moyenne ne portent que sur les années closes et sur un
 * indicateur additif ; sinon, vides.
 *
 * @param saisonnalite poids de chaque mois dans l'année (%), moyenne des années closes
 * @param parFamille familles × années, sur l'indicateur ; {@code null} s'il ne se ventile pas par famille
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ComparaisonAnneesDTO(
    IndicateurPilotageDTO indicateur,
    ModeAnnees mode,
    boolean parJourOuvre,
    LocalDate jusquAu,
    List<AnneeCompareeDTO> annees,
    List<Double> saisonnalite,
    Double croissanceAnnuelleMoyenne,
    Integer croissanceDepuis,
    Integer croissanceJusqua,
    MoisRecordDTO moisMaximum,
    List<SyntheseAnneeDTO> synthese,
    CroiseAnalyseDTO parFamille,
    CroiseAnalyseDTO parNatureVente
) {}
