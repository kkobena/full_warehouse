package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.SuggestionReassort;
import com.kobe.warehouse.domain.enumeration.StatutReassort;
import com.kobe.warehouse.domain.enumeration.TypeReassort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@Repository
public interface SuggestionReassortRepository extends JpaRepository<SuggestionReassort, Integer> {

    Optional<SuggestionReassort> findOneByStatutAndMagasinIdAndTypeReassort(StatutReassort statut, Integer magasinId, TypeReassort typeReassort);

    /**
     * Find all open suggestions for the current user's magasin
     *
     * @param statut the status (OPEN)
     * @param magasinId the magasin ID
     * @return list of open suggestions
     */
    @Query("SELECT s FROM SuggestionReassort s LEFT JOIN FETCH s.ligneReassorts WHERE s.statut = :statut AND s.magasin.id = :magasinId ORDER BY s.createdAt DESC")
    List<SuggestionReassort> findAllByStatutAndMagasinId(@Param("statut") StatutReassort statut, @Param("magasinId") Integer magasinId);

    /**
     * Find all open suggestions for the current user's magasin filtered by type
     *
     * @param statut the status (OPEN)
     * @param magasinId the magasin ID
     * @param typeReassort the type of reassort
     * @return list of open suggestions
     */
    @Query("SELECT s FROM SuggestionReassort s LEFT JOIN FETCH s.ligneReassorts WHERE s.statut = :statut AND s.magasin.id = :magasinId AND s.typeReassort = :typeReassort ORDER BY s.createdAt DESC")
    List<SuggestionReassort> findAllByStatutAndMagasinIdAndTypeReassort(@Param("statut") StatutReassort statut, @Param("magasinId") Integer magasinId, @Param("typeReassort") TypeReassort typeReassort);
}
