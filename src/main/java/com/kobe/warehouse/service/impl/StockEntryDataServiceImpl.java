package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Commande_;
import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.Fournisseur_;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.OrderLine_;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.User_;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.service.FileResourceService;
import com.kobe.warehouse.service.dto.DeliveryReceiptDTO;
import com.kobe.warehouse.service.dto.filter.DeliveryReceiptFilterDTO;
import com.kobe.warehouse.service.stock.DeliveryReceiptReportReportService;
import com.kobe.warehouse.service.stock.StockEntryDataService;
import com.kobe.warehouse.service.stock.impl.EtiquetteExportReportServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class StockEntryDataServiceImpl extends FileResourceService implements StockEntryDataService {

    private final EntityManager em;
    private final CommandeRepository commandeRepository;
    private final DeliveryReceiptReportReportService receiptReportService;
    private final OrderLineRepository orderLineRepository;
    private final EtiquetteExportReportServiceImpl etiquetteExportService;

    public StockEntryDataServiceImpl(
        EntityManager em,
        CommandeRepository commandeRepository,
        DeliveryReceiptReportReportService receiptReportService,
        OrderLineRepository orderLineRepository,
        EtiquetteExportReportServiceImpl etiquetteExportService
    ) {
        this.em = em;
        this.commandeRepository = commandeRepository;

        this.receiptReportService = receiptReportService;
        this.orderLineRepository = orderLineRepository;
        this.etiquetteExportService = etiquetteExportService;
    }

    @Override
    public Page<DeliveryReceiptDTO> fetchAllReceipts(DeliveryReceiptFilterDTO deliveryReceiptFilterDTO, Pageable pageable) {
        long count = receiptCount(deliveryReceiptFilterDTO);
        if (count == 0) {
            new PageImpl<>(Collections.emptyList(), pageable, count);
        }
        return new PageImpl<>(fetchAllDeliveryReceipts(deliveryReceiptFilterDTO, pageable), pageable, count);
    }

    @Override
    public List<DeliveryReceiptDTO> fetchAllDeliveryReceipts(DeliveryReceiptFilterDTO deliveryReceiptFilterDTO, Pageable pageable) {
        return fetchDeliveryReceipts(deliveryReceiptFilterDTO, pageable).stream().map(DeliveryReceiptDTO::new).toList();
    }

    @Override
    public Optional<DeliveryReceiptDTO> findOneById(Long id) {
        return commandeRepository.findById(id).map(DeliveryReceiptDTO::new);
    }



    @Override
    public Resource exportToPdf(Long id) throws IOException {
        return this.getResource(receiptReportService.print(commandeRepository.getReferenceById(id)));
    }

    private long receiptCount(DeliveryReceiptFilterDTO deliveryReceiptFilterDTO) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<OrderLine> root = cq.from(OrderLine.class);
        cq.select(cb.countDistinct(root.get(OrderLine_.commande)));
        List<Predicate> predicates = predicatesFetch(deliveryReceiptFilterDTO, cb, root);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Long> q = em.createQuery(cq);
        Long v = q.getSingleResult();
        return v != null ? v : 0;
    }

    private List<Commande> fetchDeliveryReceipts(DeliveryReceiptFilterDTO deliveryReceiptFilterDTO, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Commande> cq = cb.createQuery(Commande.class);
        Root<OrderLine> root = cq.from(OrderLine.class);
        cq
            .select(root.get(OrderLine_.commande))
            .distinct(true)
            .orderBy(cb.desc(root.get(OrderLine_.commande).get(Commande_.updatedAt)));
        List<Predicate> predicates = predicatesFetch(deliveryReceiptFilterDTO, cb, root);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Commande> q = em.createQuery(cq);
        if (!deliveryReceiptFilterDTO.isAll()) {
            q.setFirstResult((int) pageable.getOffset());
            q.setMaxResults(pageable.getPageSize());
        }

        return q.getResultList();
    }

    private List<Predicate> predicatesFetch(
        DeliveryReceiptFilterDTO deliveryReceiptFilterDTO,
        CriteriaBuilder cb,
        Root<OrderLine> root
    ) {
        List<Predicate> predicates = new ArrayList<>();
        if (Objects.nonNull(deliveryReceiptFilterDTO.getStatut()) ) {
            predicates.add(
                cb.equal(
                    root.get(OrderLine_.commande).get(Commande_.orderStatus),
                    deliveryReceiptFilterDTO.getStatut()
                )
            );
        }
        if (StringUtils.hasLength(deliveryReceiptFilterDTO.getSearchByRef())) {
            predicates.add(
                cb.or(
                    cb.like(
                        cb.upper(root.get(OrderLine_.commande).get(Commande_.receiptReference)),
                        deliveryReceiptFilterDTO.getSearchByRef() + "%"
                    ),
                    cb.like(
                        cb.upper(root.get(OrderLine_.commande).get(Commande_.orderReference)),
                        deliveryReceiptFilterDTO.getSearchByRef() + "%"
                    )
                )
            );
        }
        if (StringUtils.hasLength(deliveryReceiptFilterDTO.getSearch())) {
            String search = deliveryReceiptFilterDTO.getSearch().toUpperCase() + "%";
            predicates.add(
                cb.or(
                    cb.like(cb.upper(root.get(OrderLine_.commande).get(Commande_.receiptReference)), search),
                    cb.like(cb.upper(root.get(OrderLine_.commande).get(Commande_.orderReference)), search),
                    cb.like(cb.upper(root.get(OrderLine_.fournisseurProduit).get(FournisseurProduit_.codeCip)), search),
                    cb.like(
                        cb.upper(root.get(OrderLine_.fournisseurProduit).get(FournisseurProduit_.produit).get(Produit_.codeEan)),
                        search
                    ),
                    cb.like(
                        cb.upper(root.get(OrderLine_.fournisseurProduit).get(FournisseurProduit_.produit).get(Produit_.libelle)),
                        search
                    ),
                    cb.like(
                        cb.upper(
                            root.get(OrderLine_.commande).get(Commande_.fournisseur).get(Fournisseur_.libelle)
                        ),
                        search
                    )
                )
            );
        }
        if (Objects.nonNull(deliveryReceiptFilterDTO.getFournisseurId())) {
            predicates.add(
                cb.equal(
                    root.get(OrderLine_.commande).get(Commande_.fournisseur).get(Fournisseur_.id),
                    deliveryReceiptFilterDTO.getFournisseurId()
                )
            );
        }
        if (Objects.nonNull(deliveryReceiptFilterDTO.getUserId())) {
            predicates.add(
                cb.or(
                    cb.equal(
                        root.get(OrderLine_.commande).get(Commande_.user).get(User_.id),
                        deliveryReceiptFilterDTO.getUserId()
                    )
                )
            );
        }
        if (Objects.nonNull(deliveryReceiptFilterDTO.getFromDate()) && Objects.nonNull(deliveryReceiptFilterDTO.getToDate())) {
            predicates.add(
                cb.between(
                    cb.function("DATE", LocalDate.class, root.get(OrderLine_.commande).get(Commande_.updatedAt)),
                    deliveryReceiptFilterDTO.getFromDate(),
                    deliveryReceiptFilterDTO.getToDate()
                )
            );
        } else if (Objects.nonNull(deliveryReceiptFilterDTO.getFromDate())) {
            predicates.add(
                cb.equal(
                    cb.function("DATE", LocalDate.class, root.get(OrderLine_.commande).get(Commande_.updatedAt)),
                    deliveryReceiptFilterDTO.getFromDate()
                )
            );
        } else if (Objects.nonNull(deliveryReceiptFilterDTO.getToDate())) {
            predicates.add(
                cb.equal(
                    cb.function("DATE", LocalDate.class, root.get(OrderLine_.commande).get(Commande_.updatedAt)),
                    deliveryReceiptFilterDTO.getToDate()
                )
            );
        }
        return predicates;
    }

    @Override
    public Resource printEtiquette(Long id, int startAt) throws IOException {
        return this.etiquetteExportService.print(this.orderLineRepository.findAllByCommandeId(id), startAt);
    }
}
