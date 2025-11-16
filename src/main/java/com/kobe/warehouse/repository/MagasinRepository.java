package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Magasin_;
import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import java.util.EnumSet;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data  repository for the Magasin entity.
 */
@SuppressWarnings("unused")
@Repository
public interface MagasinRepository extends JpaRepository<Magasin, Integer>, JpaSpecificationExecutor<Magasin> {
    boolean existsByTypeMagasin(TypeMagasin typeMagasin);

    default Specification<Magasin> hasTypes(EnumSet<TypeMagasin> typeMagasins) {
        return (root, query, cb) -> root.get(Magasin_.typeMagasin).in(typeMagasins);
    }
}
