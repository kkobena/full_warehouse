package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.AppUser_;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.Commande_;
import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.Fournisseur_;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.OrderLine_;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.service.dto.DeliveryReceiptDTO;
import com.kobe.warehouse.service.dto.DeliveryTotalsDTO;
import com.kobe.warehouse.service.dto.filter.DeliveryReceiptFilterDTO;
import com.kobe.warehouse.service.dto.projection.DeliveryReceiptItemProjection;
import com.kobe.warehouse.service.dto.projection.DeliveryReceiptProjection;
import com.kobe.warehouse.service.settings.FileResourceService;
import com.kobe.warehouse.service.stock.DeliveryReceiptReportReportService;
import com.kobe.warehouse.service.stock.StockEntryDataService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

@Service
@Transactional(readOnly = true)
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
    @Transactional
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
    public Optional<DeliveryReceiptDTO> findOneById(CommandeId id) {
        return commandeRepository.findById(id).map(DeliveryReceiptDTO::new);
    }

    @Override
    public byte[] exportToPdf(CommandeId id) {
        return receiptReportService.export(commandeRepository.getReferenceById(id));
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
        cq.select(root.get(OrderLine_.commande)).distinct(true).orderBy(cb.desc(root.get(OrderLine_.commande).get(Commande_.updatedAt)));
        List<Predicate> predicates = predicatesFetch(deliveryReceiptFilterDTO, cb, root);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Commande> q = em.createQuery(cq);
        if (!deliveryReceiptFilterDTO.isAll()) {
            q.setFirstResult((int) pageable.getOffset());
            q.setMaxResults(pageable.getPageSize());
        }

        return q.getResultList();
    }

    private List<Predicate> predicatesFetch(DeliveryReceiptFilterDTO deliveryReceiptFilterDTO, CriteriaBuilder cb, Root<OrderLine> root) {
        List<Predicate> predicates = new ArrayList<>();
        if (CollectionUtils.isEmpty(deliveryReceiptFilterDTO.getStatuts())) {
            predicates.add(cb.equal(root.get(OrderLine_.commande).get(Commande_.orderStatus), deliveryReceiptFilterDTO.getStatut()));
        } else {
            predicates.add(root.get(OrderLine_.commande).get(Commande_.orderStatus).in(deliveryReceiptFilterDTO.getStatuts()));
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
                    cb.like(cb.upper(root.get(OrderLine_.fournisseurProduit).get(FournisseurProduit_.codeEan)), search),
                    cb.like(
                        cb.upper(root.get(OrderLine_.fournisseurProduit).get(FournisseurProduit_.produit).get(Produit_.codeEanLaboratoire)),
                        search
                    ),
                    cb.like(
                        cb.upper(root.get(OrderLine_.fournisseurProduit).get(FournisseurProduit_.produit).get(Produit_.libelle)),
                        search
                    ),
                    cb.like(cb.upper(root.get(OrderLine_.commande).get(Commande_.fournisseur).get(Fournisseur_.libelle)), search)
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
                cb.or(cb.equal(root.get(OrderLine_.commande).get(Commande_.user).get(AppUser_.id), deliveryReceiptFilterDTO.getUserId()))
            );
        }
        predicates.add(
            cb.between(
                root.get(OrderLine_.commande).get(Commande_.orderDate),
                deliveryReceiptFilterDTO.getFromDate(),
                deliveryReceiptFilterDTO.getToDate()
            )
        );
        return predicates;
    }

    @Override
    public byte[] printEtiquette(CommandeId commandeId, int startAt) {
        return this.etiquetteExportService.export(
            this.orderLineRepository.findAllByCommandeIdAndCommandeOrderDate(commandeId.getId(), commandeId.getOrderDate()),
            startAt
        );
    }

    @Override
    public Slice<DeliveryReceiptProjection> fetchAllReceipts(String searchTerm) {
        return commandeRepository.fetchAllReceipts(searchTerm, LocalDate.now().minusMonths(6), Pageable.ofSize(10));
    }

    @Override
    public List<DeliveryReceiptItemProjection> findAllByCommandeIdAndCommandeOrderDate(CommandeId commandeId) {
        return orderLineRepository.findDetailAllByCommandeIdAndCommandeOrderDate(commandeId.getId(), commandeId.getOrderDate());
    }

    @Override
    public Page<DeliveryReceiptDTO> fetchAllWithoutDetail(DeliveryReceiptFilterDTO deliveryReceiptFilterDTO, Pageable pageable) {
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Direction.DESC, "updatedAt"));
        return commandeRepository
            .findAll(buildSpecification(deliveryReceiptFilterDTO), sorted)
            .map(c -> new DeliveryReceiptDTO(c, List.of()));
    }

    @Override
    public long countByStatut(OrderStatut statut) {
        return commandeRepository.countByOrderStatus(statut);
    }

    @Override
    public DeliveryTotalsDTO computeTotals(DeliveryReceiptFilterDTO filter) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<Commande> root = cq.from(Commande.class);
        Predicate predicate = buildSpecification(filter).toPredicate(root, cq, cb);
        cq.select(cb.tuple(
            cb.count(root),
            cb.coalesce(cb.sum(root.get(Commande_.grossAmount)), 0),
            cb.coalesce(cb.sum(root.get(Commande_.htAmount)), 0),
            cb.coalesce(cb.sum(root.get(Commande_.taxAmount)), 0)
        )).where(predicate);
        Tuple row = em.createQuery(cq).getSingleResult();
        return new DeliveryTotalsDTO(
            ((Number) row.get(0)).longValue(),
            ((Number) row.get(1)).longValue(),
            ((Number) row.get(2)).longValue(),
            ((Number) row.get(3)).longValue()
        );
    }

    private Specification<Commande> buildSpecification(DeliveryReceiptFilterDTO filter) {
        Specification<Commande> spec = (_, query, cb) -> cb.conjunction();


        if (!CollectionUtils.isEmpty(filter.getStatuts())) {
            spec = spec.and(commandeRepository.byStatut(EnumSet.copyOf(filter.getStatuts())));
        } else if (filter.getStatut() != null) {
            spec = spec.and(commandeRepository.hasOrderStatut(filter.getStatut()));
        }


        if (filter.getFromDate() != null && filter.getToDate() != null) {
            spec = spec.and(commandeRepository.between(filter.getFromDate(), filter.getToDate()));
        }else{
            spec = spec.and(commandeRepository.between(LocalDate.now().minusMonths(5), LocalDate.now()));
        }

        // Recherche par référence uniquement (sans join OrderLine)
        if (StringUtils.hasLength(filter.getSearchByRef())) {
            spec = spec.and(commandeRepository.bySearchRef(filter.getSearchByRef()));
        }

        // Recherche texte complète (join OrderLine + produit)
        if (StringUtils.hasLength(filter.getSearch())) {
            spec = spec.and(commandeRepository.bySearchTerm(filter.getSearch()));
        }


        if (filter.getFournisseurId() != null) {
            spec = spec.and(commandeRepository.byFournisseur(filter.getFournisseurId().intValue()));
        }
        if (filter.getUserId() != null) {
            spec = spec.and(commandeRepository.byUser(filter.getUserId().intValue()));
        }

        return spec;
    }
}
