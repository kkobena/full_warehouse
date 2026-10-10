package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.PilotageVenteJour;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.service.dto.pilotage.ChiffreAffairesReferenceDTO;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Lectures de l'agrégat des en-têtes de vente : même définition du CA que {@link PilotageVenteRepository}, sur l'agrégat. */
public interface PilotageVenteJourRepository extends Repository<PilotageVenteJour, Long> {
    default ChiffreAffairesReferenceDTO calculerChiffreAffairesOfficine(LocalDate du, LocalDate au) {
        return calculerChiffreAffaires(du, au, CategorieChiffreAffaire.officine());
    }

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.ChiffreAffairesReferenceDTO(
            coalesce(sum(v.nbVentes), 0L), coalesce(sum(v.montantTtc), 0L), coalesce(sum(v.montantHt), 0L), coalesce(sum(v.remise), 0L))
        FROM PilotageVenteJour v
        WHERE v.jour BETWEEN :du AND :au
          AND v.annulee = false
          AND v.categorieChiffreAffaire IN :categories
        """
    )
    ChiffreAffairesReferenceDTO calculerChiffreAffaires(LocalDate du, LocalDate au, Set<CategorieChiffreAffaire> categories);

    /** Jour ouvré = jour où l'officine a vendu : gardes, fermetures et fériés se comptent d'eux-mêmes. */
    default long compterJoursOuvres(LocalDate du, LocalDate au) {
        return compterJoursAvecVentes(du, au, CategorieChiffreAffaire.officine());
    }

    @Query(
        """
        SELECT count(DISTINCT v.jour)
        FROM PilotageVenteJour v
        WHERE v.jour BETWEEN :du AND :au
          AND v.annulee = false
          AND v.categorieChiffreAffaire IN :categories
        """
    )
    long compterJoursAvecVentes(LocalDate du, LocalDate au, Set<CategorieChiffreAffaire> categories);
}
