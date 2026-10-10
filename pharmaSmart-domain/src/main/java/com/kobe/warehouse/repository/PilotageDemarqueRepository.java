package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Ajustement;
import com.kobe.warehouse.domain.enumeration.AjustType;
import com.kobe.warehouse.domain.enumeration.AjustementStatut;
import com.kobe.warehouse.service.dto.pilotage.MontantDemarqueDTO;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Démarque du pilotage : ajustements de sortie clôturés, valorisés au prix d'achat courant du produit (même définition que le
 * rapport de démarque).
 */
public interface PilotageDemarqueRepository extends Repository<Ajustement, Integer> {
    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.MontantDemarqueDTO(
            coalesce(cast(m.id AS String), ''), coalesce(m.libelle, 'Sans motif'),
            coalesce(sum(abs(aj.qtyMvt)), 0L), coalesce(sum(abs(aj.qtyMvt) * coalesce(p.costAmount, 0)), 0L))
        FROM Ajustement aj
        JOIN aj.ajust a
        JOIN aj.stockProduit sp
        JOIN sp.produit p
        LEFT JOIN aj.motifAjustement m
        WHERE a.statut = :statut
          AND aj.type = :type
          AND a.dateMtv >= :debut AND a.dateMtv < :fin
        GROUP BY m.id, m.libelle
        """
    )
    List<MontantDemarqueDTO> sommerParMotif(LocalDateTime debut, LocalDateTime fin, AjustementStatut statut, AjustType type);

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.MontantDemarqueDTO(
            cast(p.id AS String), p.libelle,
            coalesce(sum(abs(aj.qtyMvt)), 0L), coalesce(sum(abs(aj.qtyMvt) * coalesce(p.costAmount, 0)), 0L))
        FROM Ajustement aj
        JOIN aj.ajust a
        JOIN aj.stockProduit sp
        JOIN sp.produit p
        WHERE a.statut = :statut
          AND aj.type = :type
          AND a.dateMtv >= :debut AND a.dateMtv < :fin
        GROUP BY p.id, p.libelle
        ORDER BY coalesce(sum(abs(aj.qtyMvt) * coalesce(p.costAmount, 0)), 0L) DESC
        """
    )
    List<MontantDemarqueDTO> listerProduitsLesPlusTouches(LocalDateTime debut, LocalDateTime fin, AjustementStatut statut, AjustType type, Pageable page);
}
