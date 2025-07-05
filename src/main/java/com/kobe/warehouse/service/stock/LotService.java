package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.LotSold;
import com.kobe.warehouse.service.dto.LotDTO;
import java.util.List;

public interface LotService {
    LotDTO addLot(LotDTO lot);

    LotDTO editLot(LotDTO lot);

    void remove(LotDTO lot);

    void remove(Long lotId);

    List<Lot> findByProduitId(Long produitId);

    List<Lot> findProduitLots(Long produitId);

    void updateLots(List<LotSold> lots);
}
