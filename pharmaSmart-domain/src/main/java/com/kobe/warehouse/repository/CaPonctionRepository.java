package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.CaPonction;
import com.kobe.warehouse.service.declaration_ca.dto.StatutPonction;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CaPonctionRepository extends JpaRepository<CaPonction, Integer> {
    /**
     * Une ponction, à condition qu'elle relève du magasin demandé.
     *
     * <p>L'identifiant seul ne suffit pas : sur une installation multi-magasins, il laisserait
     * consulter — et annuler — la ponction d'une autre officine à qui connaît un numéro.
     */
    Optional<CaPonction> findByIdAndMagasinId(Integer id, Integer magasinId);

    /** Historique d'une officine, de la période la plus récente à la plus ancienne. */
    List<CaPonction> findByMagasinIdOrderByDateDebutDesc(Integer magasinId);

    /**
     * Ponctions dont la période recouvre celle demandée.
     *
     * <p>Deux intervalles fermés se chevauchent si chacun commence avant que l'autre ne finisse —
     * c'est l'équivalent JPQL de l'opérateur {@code &&} sur {@code daterange}. Ce contrôle sert à
     * <strong>prévenir</strong> l'utilisateur avant la saisie ; la garantie, elle, reste portée par
     * la contrainte d'exclusion GiST, qu'aucun appel forgé ne contourne.
     */
    @Query(
        """
        select p from CaPonction p
         where p.magasin.id = :magasinId
           and p.statut in :statuts
           and p.dateDebut <= :dateFin
           and p.dateFin >= :dateDebut
        """
    )
    List<CaPonction> findChevauchantes(
        @Param("magasinId") Integer magasinId,
        @Param("dateDebut") LocalDate dateDebut,
        @Param("dateFin") LocalDate dateFin,
        @Param("statuts") Collection<StatutPonction> statuts
    );

    /**
     * Reporte sur l'en-tête ce que le détail totalise réellement.
     *
     * <p>Rend l'invariant <strong>V4</strong> vrai par construction : le montant affiché dans
     * l'historique est, littéralement, la somme des lignes du justificatif.
     *
     * <p>En SQL natif, faute d'équivalent JPQL : la mise à jour lit une agrégation d'une autre table
     * dans la même instruction ({@code update … from}). Charger la ponction pour lui affecter des
     * totaux calculés en Java rouvrirait la fenêtre que cette instruction referme.
     *
     * <p>{@code clearAutomatically} n'est pas un ornement : l'en-tête vient d'être inséré, il est
     * donc dans le contexte de persistance avec ses montants d'avant l'alignement. Sans vidage, la
     * relecture qui suit immédiatement rendrait ces valeurs périmées — et l'écran afficherait une
     * ponction dont les totaux ne correspondent pas à son propre détail.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        nativeQuery = true,
        value = """
        update ca_ponction p
           set montant_ponctionne = coalesce(d.total, 0),
               nombre_ventes = coalesce(d.ventes, 0),
               ca_declare = p.ca_apres_exclusions - coalesce(d.total, 0)
          from (select coalesce(sum(montant_ponctionne), 0) as total, count(*) as ventes
                  from ca_ponction_detail where ponction_id = :id) d
         where p.id = :id
        """
    )
    void alignerEnTeteSurLeDetail(@Param("id") Integer ponctionId);
}
