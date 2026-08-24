package com.kobe.warehouse.repository;

import static com.kobe.warehouse.constant.EntityConstant.SANS_EMPLACEMENT_CODE;

import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.Rayon_;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.Storage_;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RayonRepository extends JpaRepository<Rayon, Integer>,
    JpaSpecificationExecutor<Rayon> {

    List<Rayon> findAllByStorageIdOrderByLibelle(Integer storageId);


    Optional<Rayon> findFirstByLibelleAndStorageId(String libelle, Integer storageId);

    Optional<Rayon> findFirstByCodeAndStorageId(String code, Integer storageId);


    void deleteAllByStorage(Storage storage);

    default Specification<Rayon> specialisationStorage(Integer storageId) {
        return (root, _, cb) -> cb.equal(root.get(Rayon_.storage).get(Storage_.id), storageId);
    }

    /**
     * Recherche sur le libellé ou le code. {@code queryValue} est attendu en majuscules et déjà
     * pourvu de ses jokers, comme pour les autres référentiels.
     */
    default Specification<Rayon> specialisationQueryString(String queryValue) {
        return (root, _, cb) ->
            cb.or(
                cb.like(cb.upper(root.get(Rayon_.libelle)), queryValue),
                cb.like(cb.upper(root.get(Rayon_.code)), queryValue)
            );
    }

    /**
     * Filtre sur l'exclusion du chiffre d'affaires à déclarer.
     *
     * <p>Un prédicat et non un filtre en mémoire : sans lui, la pagination compterait les lignes
     * avant filtrage et rendrait des pages incomplètes.
     */
    default Specification<Rayon> isExclude(boolean exclus) {
        return (root, _, cb) -> cb.equal(root.get(Rayon_.exclude), exclus);
    }

    default Specification<Rayon> notSansEmplacement() {
        return (root, _, cb) -> cb.notEqual(root.get(Rayon_.code), SANS_EMPLACEMENT_CODE);
    }

}
