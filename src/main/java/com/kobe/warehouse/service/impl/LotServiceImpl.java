package com.kobe.warehouse.service.impl;

import static com.kobe.warehouse.service.utils.ServiceUtil.buildPeremptionStatut;
import static java.util.Objects.nonNull;

import com.kobe.warehouse.domain.FamilleProduit;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.LotSold;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.dto.LotDTO;
import com.kobe.warehouse.service.stock.LotService;
import com.kobe.warehouse.service.stock.dto.LotFilterParam;
import com.kobe.warehouse.service.stock.dto.LotPerimeDTO;
import com.kobe.warehouse.service.stock.dto.LotPerimeValeurSum;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional
public class LotServiceImpl implements LotService {

    private final LotRepository lotRepository;
    private final AppConfigurationService appConfigurationService;
    private final ProduitRepository produitRepository;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public LotServiceImpl(
        LotRepository lotRepository,
        AppConfigurationService appConfigurationService,
        ProduitRepository produitRepository
    ) {
        this.lotRepository = lotRepository;

        this.appConfigurationService = appConfigurationService;
        this.produitRepository = produitRepository;
    }

    @Override
    public LotDTO addLot(LotDTO lot) {
        return new LotDTO(this.lotRepository.saveAndFlush(lot.toEntity()));
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
    public void remove(Long lotId) {
        this.lotRepository.deleteById(lotId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lot> findByProduitId(Long produitId) {
        return this.lotRepository.findByProduitId(
                produitId,
                LocalDate.now().minusDays(this.appConfigurationService.getNombreJourPeremption())
            );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lot> findProduitLots(Long produitId) {
        return this.lotRepository.findByProduitId(produitId);
    }

    @Override
    public void updateLots(List<LotSold> lots) {
        if (!CollectionUtils.isEmpty(lots)) {
            lots.forEach(lot -> {
                Lot entity = this.lotRepository.getReferenceById(lot.id());
                entity.setQuantity(entity.getQuantity() - lot.quantity());
                this.lotRepository.save(entity);
            });
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LotPerimeDTO> findLotsPerimes(LotFilterParam lotFilterParam, Pageable pageable) {
        boolean useLot = this.appConfigurationService.useLot().orElse(false);
        
        if (useLot) {
            return buildLotPerimePage(lotFilterParam, buildPageable(pageable, true));
        }
        return buildProduitPerimePage(lotFilterParam, buildPageable(pageable, false));
    }

    private Pageable buildPageable(Pageable pageable, boolean useLot) {
        String sortBy = useLot ? "expiryDate" : "perimeAt";
        Sort sort = Sort.by(Direction.DESC, sortBy);
        if (pageable.isPaged()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        }

        return Pageable.unpaged(sort);
    }

    @Override
    @Transactional(readOnly = true)
    public LotPerimeValeurSum findPerimeSum(LotFilterParam lotFilterParam) {
        boolean useLot = this.appConfigurationService.useLot().orElse(false);
        if (useLot) {
            return this.lotRepository.fetchPerimeSum(this.lotRepository.buildCombinedSpecification(lotFilterParam));
        }
        return this.produitRepository.fetchPerimeSum(this.produitRepository.buildCombinedSpecification(lotFilterParam));
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
                lotPerime.setDatePeremption(produit.getPerimeAt().format(dateFormatter));
                lotPerime.setPeremptionStatut(buildPeremptionStatut(produit.getPerimeAt()));
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
        lotPerime.setFounisseur(fournisseurProduit.getFournisseur().getLibelle());
        lotPerime.setProduitName(produit.getLibelle());
        lotPerime.setProduitCode(fournisseurProduit.getCodeCip());

        if (nonNull(rayon)) {
            lotPerime.setRayonName(rayon.getLibelle());
        }
    }
}
