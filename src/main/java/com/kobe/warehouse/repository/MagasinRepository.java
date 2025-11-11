package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Laboratoire;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Magasin_;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.EnumSet;

/**
 * Spring Data  repository for the Magasin entity.
 */
@SuppressWarnings("unused")
@Repository
public interface MagasinRepository extends JpaRepository<Magasin, Integer>,JpaSpecificationExecutor<Magasin> {

    default Specification<Magasin> hasTypes(EnumSet<TypeMagasin> typeMagasins) {
        return (root, query, cb) -> root.get(Magasin_.typeMagasin).in(typeMagasins);
    }
}
