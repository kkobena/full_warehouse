package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.ProductsToDestroy;
import com.kobe.warehouse.domain.ProductsToDestroy_;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroySumDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class ProductsToDestroyCustomRepositoryImpl implements ProductsToDestroyCustomRepository {

    private final EntityManager entityManager;

    public ProductsToDestroyCustomRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public ProductToDestroySumDTO getSum(Specification<ProductsToDestroy> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ProductToDestroySumDTO> query = cb.createQuery(ProductToDestroySumDTO.class);
        Root<ProductsToDestroy> root = query.from(ProductsToDestroy.class);
        query.select(
            cb.construct(
                ProductToDestroySumDTO.class,
               cb.coalesce(cb.sum(root.get(ProductsToDestroy_.quantity)), 0),
                cb.coalesce(cb.sumAsLong(cb.prod(root.get(ProductsToDestroy_.quantity), root.get(ProductsToDestroy_.prixAchat))), 0),
                cb.coalesce(cb.sumAsLong(cb.prod(root.get(ProductsToDestroy_.quantity), root.get(ProductsToDestroy_.prixUnit))), 0),
                cb.countDistinct(root.get(ProductsToDestroy_.fournisseurProduit).get(FournisseurProduit_.produit))
            )
        );

        query.where(specification.toPredicate(root, query, cb));

        TypedQuery<ProductToDestroySumDTO> typedQuery = entityManager.createQuery(query);
        return typedQuery.getSingleResult();
    }
}
