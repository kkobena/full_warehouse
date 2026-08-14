package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Magasin_;
import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import java.util.EnumSet;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data  repository for the Magasin entity.
 */
@SuppressWarnings("unused")
@Repository
public interface MagasinRepository extends JpaRepository<Magasin, Integer>,
    JpaSpecificationExecutor<Magasin> {

    boolean existsByTypeMagasin(TypeMagasin typeMagasin);

    /**
     * Officine de rattachement de l'installation, utilisée comme pivot de vérification de licence.
     *
     * <p>Le contrôle s'exécute au démarrage, hors de toute session utilisateur : on ne peut donc
     * pas
     * passer par le magasin de l'utilisateur courant. C'est l'officine — et non un dépôt
     * d'extension — qui porte la raison sociale imprimée sur les documents, donc celle à laquelle
     * la licence est délivrée (cf. docs/PLAN-GESTION-LICENCE.md §3.4).
     */
    Optional<Magasin> findFirstByTypeMagasinOrderByIdAsc(TypeMagasin typeMagasin);

    default Specification<Magasin> hasTypes(EnumSet<TypeMagasin> typeMagasins) {
        return (root, query, cb) -> root.get(Magasin_.typeMagasin).in(typeMagasins);
    }
}
