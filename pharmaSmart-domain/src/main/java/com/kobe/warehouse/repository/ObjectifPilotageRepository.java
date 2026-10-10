package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ObjectifPilotage;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ObjectifPilotageRepository extends JpaRepository<ObjectifPilotage, Long> {
    @Query("SELECT o FROM ObjectifPilotage o LEFT JOIN FETCH o.modifiePar WHERE o.annee = :annee ORDER BY o.indicateur, o.mois")
    List<ObjectifPilotage> listerParAnnee(int annee);

    List<ObjectifPilotage> findAllByAnneeAndIndicateur(int annee, IndicateurPilotage indicateur);

    List<ObjectifPilotage> findAllByAnneeBetween(int du, int au);
}
