package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Fournisseur_;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.Suggestion;
import com.kobe.warehouse.domain.Suggestion_;
import com.kobe.warehouse.service.dto.projection.SuggestionProjection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class SuggestionCustomRepositoryImpl implements SuggestionCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<SuggestionProjection> getAllSuggestion(Specification<Suggestion> specification, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SuggestionProjection> query = cb.createQuery(SuggestionProjection.class);

        Root<Suggestion> root = query.from(Suggestion.class);
        query
            .select(
                cb.construct(
                    SuggestionProjection.class,
                    root.get(Suggestion_.id),
                    root.get(Suggestion_.suggessionReference),
                    root.get(Suggestion_.createdAt),
                    root.get(Suggestion_.updatedAt),
                    root.get(Suggestion_.typeSuggession),
                    root.get(Suggestion_.statut),
                    root.get(Suggestion_.fournisseur).get(Fournisseur_.id),
                    root.get(Suggestion_.fournisseur).get(Fournisseur_.libelle)
                )
            )
            .orderBy(cb.desc(root.get(Suggestion_.updatedAt)));

        // Appliquer la specification pour ajouter des critères à la requête
        Predicate predicate = specification.toPredicate(root, query, cb);
        query.where(predicate);

        TypedQuery<SuggestionProjection> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<SuggestionProjection> results = typedQuery.getResultList();

        return new PageImpl<>(results, pageable, count(specification));
    }

    private Long count(Specification<Suggestion> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Suggestion> root = countQuery.from(Suggestion.class);
        countQuery.select(cb.count(root));
        Predicate predicate = specification.toPredicate(root, countQuery, cb);
        countQuery.where(predicate);
        return entityManager.createQuery(countQuery).getSingleResult();
    }
}
