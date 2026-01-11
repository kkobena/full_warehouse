package com.kobe.warehouse.service.reassort;

import com.kobe.warehouse.domain.LigneReassort;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.service.reassort.dto.RepartionQueryDto;
import com.kobe.warehouse.service.reassort.dto.RepartionSearchQueryDto;
import com.kobe.warehouse.service.reassort.dto.RepartitionStockProduitDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface RepartitionStockService {

    void process(Set<LigneReassort> ligneReassorts);

    void processReassortStockRayon(Set<LigneReassort> ligneReassorts);

    void process(List<RepartionQueryDto> datas);

    Page<RepartitionStockProduitDto> fetchRepartitionStockProduits(RepartionSearchQueryDto searchQueryDto, Pageable pageable);

    /*
    Lors de l'ajout de stock de reserve , on peut transférer transférer du stock du stock de rayon vers le stock de reserve
     */
    void transferStockBetweenStorages(StockProduit stockProduitDest);

}
