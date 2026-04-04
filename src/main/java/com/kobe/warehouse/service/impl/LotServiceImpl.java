package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.FamilleProduit;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.LotSold;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.enumeration.StatutLot;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.repository.LotStockLocationRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.service.OrderLineService;
import com.kobe.warehouse.service.dto.LotDTO;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stock.LotService;
import com.kobe.warehouse.service.stock.LotServiceReportService;
import com.kobe.warehouse.service.stock.dto.LotFilterParam;
import com.kobe.warehouse.service.stock.dto.LotLocationDTO;
import com.kobe.warehouse.service.stock.dto.LotPerimeDTO;
import com.kobe.warehouse.service.stock.dto.LotPerimeValeurSum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.kobe.warehouse.service.utils.ServiceUtil.buildPeremptionStatut;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@Transactional
public class LotServiceImpl implements LotService {

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final LotRepository lotRepository;
    private final LotStockLocationRepository lotStockLocationRepository;
    private final AppConfigurationService appConfigurationService;
    private final ProduitRepository produitRepository;
    private final LotServiceReportService lotServiceReportService;
    private final OrderLineService orderLineService;

    public LotServiceImpl(
        LotRepository lotRepository,
        LotStockLocationRepository lotStockLocationRepository,
        AppConfigurationService appConfigurationService,
        ProduitRepository produitRepository,
        LotServiceReportService lotServiceReportService,
        OrderLineService orderLineService
    ) {
        this.lotRepository = lotRepository;
        this.lotStockLocationRepository = lotStockLocationRepository;
        this.appConfigurationService = appConfigurationService;
        this.produitRepository = produitRepository;
        this.lotServiceReportService = lotServiceReportService;
        this.orderLineService = orderLineService;
    }

    @Override
    public LotDTO addLot(LotDTO lot) {
        OrderLine orderLine = this.orderLineService.findOneById(lot.getReceiptItemId()).orElse(null);
        Lot lotEntity = lot.toEntity();
        lotEntity.setCurrentQuantity(lotEntity.getQuantity());
        lotEntity.setCreatedDate(LocalDateTime.now());
        lotEntity.setPrixUnit(orderLine.getOrderUnitPrice());
        lotEntity.setPrixAchat(orderLine.getOrderCostAmount());
        lotEntity.setOrderLine(orderLine);
        return new LotDTO(this.lotRepository.saveAndFlush(lotEntity));
    }

    @Override
    public LotDTO editLot(LotDTO lot) {
        Lot entity = this.lotRepository.getReferenceById(lot.getId());
        entity.setFreeQty(Optional.ofNullable(lot.getFreeQty()).orElse(0));
        entity.setExpiryDate(lot.getExpiryDate());
        entity.setManufacturingDate(lot.getManufacturingDate());
        entity.setNumLot(lot.getNumLot());
        entity.setQuantity(computeQuantity(lot));
        return new LotDTO(this.lotRepository.saveAndFlush(entity));
    }

    @Override
    public void remove(LotDTO lot) {
        this.lotRepository.deleteById(lot.getId());
    }

    @Override
    public void remove(Integer lotId) {
        this.lotRepository.deleteById(lotId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lot> findByProduitId(Integer produitId) {
        return this.lotRepository.findByProduitId(
            produitId,
            LocalDate.now().minusDays(this.appConfigurationService.getNombreJourPeremption()),
            StatutLot.AVAILABLE
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lot> findProduitLots(Integer produitId) {
        return this.lotRepository.findByProduitId(produitId);
    }

    @Override
    public void updateLots(List<LotSold> lots) {
        if (!CollectionUtils.isEmpty(lots)) {
            lots.forEach(lot -> {
                Lot entity = this.lotRepository.getReferenceById(lot.id());
                int currentQty = entity.getCurrentQuantity() - lot.quantity();
                entity.setCurrentQuantity(Math.max(currentQty, 0));
                if (entity.getCurrentQuantity() <= 0) {
                    entity.setStatut(StatutLot.SOLD);
                }
                this.lotRepository.save(entity);
            });
        }
    }

    @Override
    public void adjustLots(Produit produit, int qtyDelta) {
        if (qtyDelta == 0) return;

        if (qtyDelta < 0) {
            // AJUSTEMENT_OUT : débiter en FEFO (expiry ASC)
            int remaining = Math.abs(qtyDelta);
            List<Lot> lots = lotRepository.findByProduitId(produit.getId());
            for (Lot lot : lots) {
                if (remaining <= 0) break;
                int toTake = Math.min(lot.getCurrentQuantity(), remaining);
                if (toTake <= 0) continue;
                lot.setCurrentQuantity(lot.getCurrentQuantity() - toTake);
                if (lot.getCurrentQuantity() <= 0) {
                    lot.setStatut(StatutLot.SOLD);
                }
                lotRepository.save(lot);
                remaining -= toTake;
            }
        } else {
            // AJUSTEMENT_IN : créditer le lot le plus récemment reçu
            lotRepository.findLastReceivedByProduitId(produit.getId()).ifPresent(lot -> {
                lot.setCurrentQuantity(lot.getCurrentQuantity() + qtyDelta);
                if (lot.getStatut() == StatutLot.SOLD) {
                    lot.setStatut(StatutLot.AVAILABLE);
                }
                lotRepository.save(lot);
            });
        }
    }

    @Override
    public void creditSpecificLot(Lot lot, int qty) {
        if (qty <= 0) return;
        Lot entity = lotRepository.getReferenceById(lot.getId());
        entity.setCurrentQuantity(entity.getCurrentQuantity() + qty);
        if (entity.getStatut() == StatutLot.SOLD) {
            entity.setStatut(StatutLot.AVAILABLE);
        }
        lotRepository.save(entity);
    }

    //TODO logique à  revoir(pour decrement le last IN pour les annulation) ou faire une étude comparative pour voir comment les autres gère ce cas
    @Override
    public void restoreLots(List<LotSold> lots) {
        if (!CollectionUtils.isEmpty(lots)) {
            lots.forEach(lot -> {
                Lot entity = this.lotRepository.getReferenceById(lot.id());
                entity.setCurrentQuantity(entity.getCurrentQuantity() + lot.quantity());
                if (entity.getCurrentQuantity() > 0) {
                    entity.setStatut(StatutLot.AVAILABLE);
                }
                this.lotRepository.save(entity);
            });
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LotPerimeDTO> findLotsPerimes(LotFilterParam lotFilterParam, Pageable pageable) {
        if (isNull(lotFilterParam.getDayCount()) && (isNull(lotFilterParam.getToDate()) && isNull(lotFilterParam.getFromDate()))) {
            lotFilterParam.setDayCount(this.appConfigurationService.getNombreJourAlertPeremption());
        }
        return buildLotPerimePage(lotFilterParam, buildPageable(pageable));


    }

    private Pageable buildPageable(Pageable pageable) {

        Sort sort = Sort.by(Direction.DESC, "expiryDate");
        if (pageable.isPaged()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        }

        return Pageable.unpaged(sort);
    }

    @Override
    @Transactional(readOnly = true)
    public LotPerimeValeurSum findPerimeSum(LotFilterParam lotFilterParam) {
        return this.lotRepository.fetchPerimeSum(this.lotRepository.buildCombinedSpecification(lotFilterParam));

    }


    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> generatePdf(LotFilterParam lotFilterParam) {
        return lotServiceReportService.generatePdf(
            findLotsPerimes(lotFilterParam, Pageable.unpaged()).getContent(),
            findPerimeSum(lotFilterParam),
            lotFilterParam.getFromDate(),
            lotFilterParam.getToDate()
        );
    }

    private int computeQuantity(LotDTO lot) {
        return lot.getQuantityReceived() + Optional.ofNullable(lot.getFreeQty()).orElse(0);
    }

    private Page<LotPerimeDTO> buildLotPerimePage(LotFilterParam lotFilterParam, Pageable pageable) {
        return this.lotRepository.findAll(this.lotRepository.buildCombinedSpecification(lotFilterParam), pageable).map(lot -> {
            FournisseurProduit fournisseurProduit = lot.getOrderLine().getFournisseurProduit();
            Produit produit = fournisseurProduit.getProduit();
            LotPerimeDTO lotPerime = new LotPerimeDTO();
            lotPerime.setProduitId(produit.getId());
            lotPerime.setId(lot.getId());
            lotPerime.setNumLot(lot.getNumLot());
            lotPerime.setQuantity(lot.getQuantity());
            lotPerime.setDatePeremption(lot.getExpiryDate().format(dateFormatter));
            lotPerime.setPeremptionStatut(buildPeremptionStatut(lot.getExpiryDate()));
            buildCommon(lotPerime, produit, fournisseurProduit);

            // Construire la liste des emplacements depuis LotStockLocation
            if (lotFilterParam.getStorageId() != null) {
                // Filtre storage actif → une seule localisation, quantité précise dans ce storage
                this.lotStockLocationRepository
                    .findByLotIdAndStorageId(lot.getId(), lotFilterParam.getStorageId())
                    .ifPresent(lsl -> {
                        lotPerime.setLocations(List.of(new LotLocationDTO(
                            lsl.getStorage().getId(),
                            lsl.getStorage().getName(),
                            lsl.getQty()
                        )));
                        lotPerime.setQuantity(lsl.getQty());
                    });
            } else {
                // Pas de filtre → toutes les localisations disponibles (PRINCIPAL en premier)
                List<LotLocationDTO> locs = this.lotStockLocationRepository
                    .findAvailableByLotId(lot.getId())
                    .stream()
                    .map(lsl -> new LotLocationDTO(
                        lsl.getStorage().getId(),
                        lsl.getStorage().getName(),
                        lsl.getQty()
                    ))
                    .toList();
                lotPerime.setLocations(locs);
            }
            return lotPerime;
        });
    }

    private Page<LotPerimeDTO> buildProduitPerimePage(LotFilterParam lotFilterParam, Pageable pageable) {
        return this.produitRepository.findAll(this.produitRepository.buildCombinedSpecification(lotFilterParam), pageable).map(produit -> {
            FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
            Set<StockProduit> stockProduits = produit.getStockProduits();
            LotPerimeDTO lotPerime = new LotPerimeDTO();
            lotPerime.setProduitId(produit.getId());
            lotPerime.setId(produit.getId());
            lotPerime.setQuantity(stockProduits.stream().mapToInt(StockProduit::getQtyStock).sum());
            //  lotPerime.setDatePeremption(produit.getPerimeAt().format(dateFormatter));
            //  lotPerime.setPeremptionStatut(buildPeremptionStatut(produit.getPerimeAt()));
            buildCommon(lotPerime, produit, fournisseurProduit);

            return lotPerime;
        });
    }

    private void buildCommon(LotPerimeDTO lotPerime, Produit produit, FournisseurProduit fournisseurProduit) {
        FamilleProduit familleProduit = produit.getFamille();
        Rayon rayon = produit.getRayonProduits().stream().findFirst().map(RayonProduit::getRayon).orElse(null);
        lotPerime.setFamilleProduitName(familleProduit.getLibelle());
        lotPerime.setFamilleProduitName(familleProduit.getLibelle());
        lotPerime.setPrixAchat(fournisseurProduit.getPrixAchat());
        lotPerime.setPrixVente(fournisseurProduit.getPrixUni());
        lotPerime.setFournisseur(fournisseurProduit.getFournisseur().getLibelle());
        lotPerime.setProduitName(produit.getLibelle());
        lotPerime.setProduitCode(fournisseurProduit.getCodeCip());

        if (nonNull(rayon)) {
            lotPerime.setRayonName(rayon.getLibelle());
        }
    }


    /*
    @Transactional
    public void pickProduct(Product product, int qtyToPick, String reference) {
        List<Lot> lots = lotRepo.findByProductOrderByExpiryDateAsc(product);
        int remaining = qtyToPick;
        for (Lot lot : lots) {
            if (remaining <= 0) break;
            int take = Math.min(remaining, lot.getQuantity());
            if (take <= 0) continue;
            lot.setQuantity(lot.getQuantity() - take);
            lotRepo.save(lot);
            movementRepo.save(new StockMovement(product, lot, type=SALE, qty=-take, ... ));
    remaining -= take;
}
        if (remaining > 0) {
    throw new InsufficientStockException(product.getSku(), remaining);
    }
    }
     */
}
