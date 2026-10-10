package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.PilotageVenteJour;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.service.dto.pilotage.LignesJourDTO;
import com.kobe.warehouse.service.dto.pilotage.MontantJourDTO;
import com.kobe.warehouse.service.dto.pilotage.VentesJourDTO;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Mesures journalières du pilotage, lues sur les agrégats (migration V2.1.34) ; les ventilations : {@link PilotageAnalyseRepository}. */
public interface PilotageMesuresRepository extends Repository<PilotageVenteJour, Long> {
    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.VentesJourDTO(
            v.jour,
            coalesce(sum(CASE WHEN v.annulee = false THEN v.nbVentes ELSE 0L END), 0L),
            coalesce(sum(CASE WHEN v.annulee = true THEN v.nbVentes ELSE 0L END), 0L),
            coalesce(sum(CASE WHEN v.annulee = false THEN v.montantTtc ELSE 0L END), 0L),
            coalesce(sum(CASE WHEN v.annulee = false THEN v.montantHt ELSE 0L END), 0L),
            coalesce(sum(CASE WHEN v.annulee = false THEN v.remise ELSE 0L END), 0L),
            coalesce(sum(CASE WHEN v.annulee = false THEN v.partTiersPayant ELSE 0L END), 0L))
        FROM PilotageVenteJour v
        WHERE v.jour BETWEEN :du AND :au
          AND v.categorieChiffreAffaire IN :categories
        GROUP BY v.jour
        ORDER BY v.jour
        """
    )
    List<VentesJourDTO> listerVentesParJour(LocalDate du, LocalDate au, Set<CategorieChiffreAffaire> categories);

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.LignesJourDTO(
            l.jour, coalesce(sum(l.montantTtc), 0L), coalesce(sum(l.montantHt), 0L), coalesce(sum(l.coutHt), 0L), coalesce(sum(l.quantiteServie), 0L))
        FROM PilotageVenteLigneJour l
        WHERE l.jour BETWEEN :du AND :au
          AND l.categorieChiffreAffaire IN :categories
        GROUP BY l.jour
        ORDER BY l.jour
        """
    )
    List<LignesJourDTO> listerLignesParJour(LocalDate du, LocalDate au, Set<CategorieChiffreAffaire> categories);

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.MontantJourDTO(a.jour, coalesce(sum(a.montantTtc), 0L))
        FROM PilotageAchatJour a
        WHERE a.jour BETWEEN :du AND :au
        GROUP BY a.jour
        ORDER BY a.jour
        """
    )
    List<MontantJourDTO> listerAchatsParJour(LocalDate du, LocalDate au);

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.MontantJourDTO(e.jour, coalesce(sum(e.montant), 0L))
        FROM PilotageEncaissementJour e
        WHERE e.jour BETWEEN :du AND :au
          AND e.typeTransaction IN :types
        GROUP BY e.jour
        ORDER BY e.jour
        """
    )
    List<MontantJourDTO> listerEncaissementsParJour(LocalDate du, LocalDate au, Collection<String> types);
}
