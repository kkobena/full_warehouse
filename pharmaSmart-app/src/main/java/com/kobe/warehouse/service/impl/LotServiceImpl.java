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
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.enumeration.StatutLot;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.repository.LotStockLocationRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.service.OrderLineService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.LotDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stock.LotService;
import com.kobe.warehouse.service.stock.LotServiceReportService;
import com.kobe.warehouse.service.stock.LotStockLocationService;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
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
    private final LotStockLocationService lotStockLocationService;
    private final StorageService storageService;

    public LotServiceImpl(
        LotRepository lotRepository,
        LotStockLocationRepository lotStockLocationRepository,
        AppConfigurationService appConfigurationService,
        ProduitRepository produitRepository,
        LotServiceReportService lotServiceReportService,
        OrderLineService orderLineService,
        LotStockLocationService lotStockLocationService,
        StorageService storageService
    ) {
        this.lotRepository = lotRepository;
        this.lotStockLocationRepository = lotStockLocationRepository;
        this.appConfigurationService = appConfigurationService;
        this.produitRepository = produitRepository;
        this.lotServiceReportService = lotServiceReportService;
        this.orderLineService = orderLineService;
        this.lotStockLocationService = lotStockLocationService;
        this.storageService = storageService;
    }

    @Override
    public List<LotDTO> addLotBatch(List<LotDTO> lots) {
        return lots.stream().map(this::addLot).toList();
    }

    @Override
    public LotDTO addLot(LotDTO lot) {
        OrderLine orderLine = this.orderLineService.findOneById(lot.getReceiptItemId())
            .orElseThrow(() -> new GenericError("Ligne de commande introuvable", "ligneCommandeIntrouvable"));
        Integer qtyReceived = orderLine.getQuantityReceived();
        if (Objects.nonNull(qtyReceived) && qtyReceived > 0) {
            int alreadyCovered = orderLine.getLots().stream()
                .mapToInt(l -> Optional.ofNullable(l.getQuantity()).orElse(0))
                .sum();
            int newQty = Optional.ofNullable(lot.getQuantityReceived()).orElse(0)
                + Optional.ofNullable(lot.getFreeQty()).orElse(0);
            int total = qtyReceived + Optional.of(orderLine.getFreeQty()).orElse(0);
            if (alreadyCovered + newQty > total) {
                throw new GenericError(
                    "La quantité totale des lots (" + (alreadyCovered + newQty) + ") dépasserait la quantité reçue (" + total + ")",
                    "lotsDepassentQuantiteRecue"
                );
            }
        }
        if (lot.getNumLot() != null) {
            Optional<Lot> existing = orderLine.getLots().stream()
                .filter(l -> lot.getNumLot().equals(l.getNumLot()))
                .findFirst();
            if (existing.isPresent()) {
                Lot existingLot = existing.get();
                int addQty = Optional.ofNullable(lot.getQuantityReceived()).orElse(0)
                    + Optional.ofNullable(lot.getFreeQty()).orElse(0);
                existingLot.setQuantity(existingLot.getQuantity() + addQty);
                existingLot.setCurrentQuantity(existingLot.getCurrentQuantity() + addQty);
                existingLot.setFreeQty(existingLot.getFreeQty() + Optional.ofNullable(lot.getFreeQty()).orElse(0));
                return new LotDTO(this.lotRepository.saveAndFlush(existingLot));
            }
        }
        Lot lotEntity = lot.toEntity();
        lotEntity.setCurrentQuantity(lotEntity.getQuantity());
        lotEntity.setCreatedDate(LocalDateTime.now());
        lotEntity.setPrixUnit(orderLine.getOrderUnitPrice());
        lotEntity.setPrixAchat(orderLine.getOrderCostAmount());
        lotEntity.setOrderLine(orderLine);
        lotEntity.setProduit(orderLine.getFournisseurProduit().getProduit());
        return new LotDTO(this.lotRepository.saveAndFlush(lotEntity));
    }

    @Override
    public LotDTO addLotSurProduit(LotDTO lot) {
        if (lot.getProduitId() == null) {
            throw new GenericError("Le produitId est obligatoire pour la saisie de lot hors commande", "produitIdManquant");
        }
        if (lot.getNumLot() == null || lot.getNumLot().isBlank()) {
            throw new GenericError("Le numéro de lot est obligatoire", "numLotManquant");
        }
        if (lot.getExpiryDate() == null) {
            throw new GenericError("La date de péremption est obligatoire", "expiryDateManquante");
        }

        Produit produit = this.produitRepository.findById(lot.getProduitId())
            .orElseThrow(() -> new GenericError("Produit introuvable", "produitIntrouvable"));

        // Résoudre le storage cible : celui fourni par l'UI, sinon le storage principal de l'utilisateur connecté
        Storage storage = lot.getStorageId() != null
            ? storageService.getOne(lot.getStorageId())
            : storageService.getDefaultConnectedUserMainStorage();

        int lotQty = Optional.ofNullable(lot.getQuantityReceived()).orElse(0)
            + Optional.ofNullable(lot.getFreeQty()).orElse(0);
        if (lotQty <= 0) {
            throw new GenericError("La quantité doit être supérieure à 0", "quantiteInvalide");
        }

        // Vérifier que la quantité ne dépasse pas le stock dispo sur ce storage
        int stockStorage = produit.getStockProduits().stream()
            .filter(sp -> sp.getStorage().getId().equals(storage.getId()))
            .mapToInt(StockProduit::getQtyStock)
            .sum();
        if (lotQty > stockStorage) {
            throw new GenericError(
                "La quantité du lot (" + lotQty + ") ne peut pas dépasser le stock disponible sur cet emplacement (" + stockStorage + ")",
                "quantiteDepasseStock"
            );
        }

        FournisseurProduit fp = produit.getFournisseurProduitPrincipal();
        int prixAchat = fp != null ? fp.getPrixAchat() : produit.getCostAmount();
        int prixUnit = fp != null ? fp.getPrixUni() : produit.getRegularUnitPrice();

        Lot lotEntity = lot.toEntity();
        lotEntity.setCurrentQuantity(lotEntity.getQuantity());
        lotEntity.setCreatedDate(LocalDateTime.now());
        lotEntity.setPrixAchat(prixAchat);
        lotEntity.setPrixUnit(prixUnit);
        lotEntity.setProduit(produit);
        // Pas d'OrderLine → saisie hors commande
        lotEntity = this.lotRepository.saveAndFlush(lotEntity);

        // Enregistrer la localisation du lot dans le storage cible
        lotStockLocationService.credit(lotEntity, storage, lotQty);

        return new LotDTO(lotEntity);
    }

    @Override
    public LotDTO editLot(LotDTO lot) {
        Lot entity = this.lotRepository.getReferenceById(lot.getId());
        int ug = Optional.ofNullable(lot.getUgQuantityReceived()).orElse(Optional.ofNullable(lot.getFreeQty()).orElse(0));
        entity.setFreeQty(ug);
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
            produitId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Lot> findByProduitIdAndNumLot(Integer produitId, String numLot) {
        return this.lotRepository.findByNumLotAndProduitId(numLot, produitId);
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
        int ug = Optional.ofNullable(lot.getUgQuantityReceived()).orElse(Optional.ofNullable(lot.getFreeQty()).orElse(0));
        return lot.getQuantityReceived() + ug;
    }

    private Page<LotPerimeDTO> buildLotPerimePage(LotFilterParam lotFilterParam, Pageable pageable) {
        return this.lotRepository.findAll(this.lotRepository.buildCombinedSpecification(lotFilterParam), pageable).map(lot -> {
            // Fix NPE : un lot hors commande (saisi depuis la fiche produit) n'a pas d'OrderLine
            FournisseurProduit fournisseurProduit;
            Produit produit;
            if (lot.getOrderLine() != null) {
                fournisseurProduit = lot.getOrderLine().getFournisseurProduit();
                produit = fournisseurProduit.getProduit();
            } else if (lot.getProduit() != null) {
                produit = lot.getProduit();
                fournisseurProduit = produit.getFournisseurProduitPrincipal();
            } else {
                // Lot orphelin sans produit ni commande → DTO minimal
                LotPerimeDTO orphan = new LotPerimeDTO();
                orphan.setId(lot.getId());
                orphan.setNumLot(lot.getNumLot());
                orphan.setQuantity(lot.getQuantity());
                if (lot.getExpiryDate() != null) {
                    orphan.setDatePeremption(lot.getExpiryDate().format(dateFormatter));
                    orphan.setPeremptionStatut(buildPeremptionStatut(lot.getExpiryDate()));
                }
                return orphan;
            }
            LotPerimeDTO lotPerime = new LotPerimeDTO();
            lotPerime.setProduitId(produit.getId());
            lotPerime.setId(lot.getId());
            lotPerime.setNumLot(lot.getNumLot());
            lotPerime.setQuantity(lot.getQuantity());
            if (lot.getExpiryDate() != null) {
                lotPerime.setDatePeremption(lot.getExpiryDate().format(dateFormatter));
                lotPerime.setPeremptionStatut(buildPeremptionStatut(lot.getExpiryDate()));
            }
            if (fournisseurProduit != null) {
                buildCommon(lotPerime, produit, fournisseurProduit);
            } else {
                // Lot hors commande sans fournisseur principal → infos produit uniquement
                lotPerime.setProduitName(produit.getLibelle());
                FamilleProduit familleProduit = produit.getFamille();
                if (familleProduit != null) {
                    lotPerime.setFamilleProduitName(familleProduit.getLibelle());
                }
            }

            // Construire la liste des emplacements depuis LotStockLocation
            if (lotFilterParam.getStorageId() != null) {
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
