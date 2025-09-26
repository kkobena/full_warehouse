package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Commande_;
import com.kobe.warehouse.domain.Customer_;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.service.dto.records.AchatRecord;
import com.kobe.warehouse.service.reglement.differe.dto.Differe;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Objects.nonNull;

@Repository
@Transactional(readOnly = true)
public class CustomCommandeRepositoryImpl implements CustomCommandeRepository{

    private static final Logger LOG = LoggerFactory.getLogger(CustomCommandeRepositoryImpl.class);
    private  final EntityManager em;

    public CustomCommandeRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public AchatRecord getAchatPeriode(Specification<Commande> specification) {
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<AchatRecord> query = cb.createQuery(AchatRecord.class);
            Root<Commande> root = query.from(Commande.class);

            query
                .select(
                    cb.construct(
                        AchatRecord.class,
                        cb.sumAsLong(root.get(Commande_.htAmount)),
                        cb.sumAsLong(root.get(Commande_.discountAmount)),
                        cb.sumAsLong(root.get(Commande_.taxAmount)),
                        cb.count(root.get(Commande_.id)
                    )
                ));

            if (nonNull(specification)) {
                Predicate predicate = specification.toPredicate(root, query, cb);
                query.where(predicate);
            }

            TypedQuery<AchatRecord> typedQuery = em.createQuery(query);


           return typedQuery.getSingleResult();
        } catch (Exception e) {
            LOG.info(e.getLocalizedMessage());
            return null;
        }
    }
}
