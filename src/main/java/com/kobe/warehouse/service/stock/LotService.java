package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.LotSold;
import com.kobe.warehouse.service.dto.LotDTO;
import com.kobe.warehouse.service.stock.dto.LotFilterParam;
import com.kobe.warehouse.service.stock.dto.LotPerimeDTO;
import com.kobe.warehouse.service.stock.dto.LotPerimeValeurSum;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LotService {
    LotDTO addLot(LotDTO lot);

    LotDTO editLot(LotDTO lot);

    void remove(LotDTO lot);

    void remove(Long lotId);

    List<Lot> findByProduitId(Long produitId);

    List<Lot> findProduitLots(Long produitId);

    void updateLots(List<LotSold> lots);

    Page<LotPerimeDTO> findLotsPerimes(LotFilterParam lotFilterParam, Pageable pageable);

    LotPerimeValeurSum findPerimeSum(LotFilterParam lotFilterParam);
}
