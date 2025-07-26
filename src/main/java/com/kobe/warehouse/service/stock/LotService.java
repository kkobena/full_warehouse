package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.LotSold;
import com.kobe.warehouse.service.dto.LotDTO;
import com.kobe.warehouse.service.excel.model.ExportFormat;
import com.kobe.warehouse.service.stock.dto.LotFilterParam;
import com.kobe.warehouse.service.stock.dto.LotPerimeDTO;
import com.kobe.warehouse.service.stock.dto.LotPerimeValeurSum;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

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

    ResponseEntity<byte[]> generatePdf(LotFilterParam lotFilterParam);

    void export(HttpServletResponse response, ExportFormat type, LotFilterParam lotFilterParam) throws IOException;
}
