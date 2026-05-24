package com.kobe.warehouse.service.report;

import com.kobe.warehouse.domain.MvStockAlert;
import com.kobe.warehouse.domain.enumeration.StockAlertType;
import com.kobe.warehouse.repository.MvStockAlertRepository;
import com.kobe.warehouse.service.dto.report.StockAlertDTO;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StockAlertReportServiceImpl implements StockAlertReportService {

    private final MvStockAlertRepository mvStockAlertRepository;

    public StockAlertReportServiceImpl(MvStockAlertRepository mvStockAlertRepository) {
        this.mvStockAlertRepository = mvStockAlertRepository;
    }

    @Override
    @Cacheable(value = "stockAlerts", key = "#alertTypes != null ? #alertTypes.toString() + #pageable.toString() : 'all_' + #pageable.toString()")
    public Page<StockAlertDTO> getStockAlerts(List<StockAlertType> alertTypes, Pageable pageable) {
        Page<MvStockAlert> page = (alertTypes != null && !alertTypes.isEmpty())
            ? mvStockAlertRepository.findAllByAlertTypeIn(EnumSet.copyOf(alertTypes), pageable)
            : mvStockAlertRepository.findAll(pageable);
        return page.map(this::toDTO);
    }

    @Override
    public Map<StockAlertType, Long> getStockAlertsCount() {
        Map<StockAlertType, Long> counts = new EnumMap<>(StockAlertType.class);
        counts.put(StockAlertType.RUPTURE, 0L);
        counts.put(StockAlertType.ALERTE, 0L);
        counts.put(StockAlertType.PEREMPTION, 0L);

        mvStockAlertRepository.findAll().forEach(alert -> {
            if (alert.getAlertType() != null) {
                counts.merge(alert.getAlertType(), 1L, Long::sum);
            }
        });

        return counts;
    }


    private StockAlertDTO toDTO(MvStockAlert entity) {
        return new StockAlertDTO(
            entity.getProduitId(),
            entity.getLibelle(),
            entity.getCodeCip(),
            entity.getStockQuantity(),
            entity.getSeuilMin(),
            entity.getExpiryDate(),
            entity.getAlertType()
        );
    }
}
