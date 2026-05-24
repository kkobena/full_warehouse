package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.service.dto.CommandeDashboardDTO;
import com.kobe.warehouse.service.dto.CommandeDTO;
import com.kobe.warehouse.service.dto.CommandeEntryDTO;
import com.kobe.warehouse.service.dto.CommandeLiteDTO;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.dto.filter.CommandeFilterDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.AchatDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommandeDataService {
    CommandeDTO findOneById(CommandeId id);

    Optional<CommandeEntryDTO> getCommandeById(CommandeId id);

    Resource exportCommandeToCsv(CommandeId id) throws IOException;

    byte[] exportCommandeToPdf(CommandeId id);

    List<OrderLineDTO> filterCommandeLines(CommandeFilterDTO commandeFilter);

    Page<CommandeLiteDTO> fetchCommandes(CommandeFilterDTO commandeFilterDTO, Pageable pageable);

    Page<OrderLineDTO> filterCommandeLines(CommandeId commandeId, Pageable pageable);

    Resource getRuptureCsv(String reference);

    List<AchatDTO> fetchReportTableauPharmacienData(MvtParam mvtParam);

    CommandeDashboardDTO getDashboard();
}
