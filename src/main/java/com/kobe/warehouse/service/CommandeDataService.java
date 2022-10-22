package com.kobe.warehouse.service;

import com.kobe.warehouse.service.dto.CommandeDTO;
import com.kobe.warehouse.service.dto.CommandeFilterDTO;
import com.kobe.warehouse.service.dto.CommandeLiteDTO;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;

public interface CommandeDataService {
  CommandeDTO findOneById(Long id);

  Resource exportCommandeToCsv(Long id) throws IOException;

  Resource exportCommandeToPdf(Long id) throws IOException;

  List<OrderLineDTO> filterCommandeLines(CommandeFilterDTO commandeFilter);

  Page<CommandeLiteDTO> fetchCommandes(CommandeFilterDTO commandeFilterDTO, Pageable pageable);

  Page<OrderLineDTO> filterCommandeLines(Long commandeId,Pageable pageable);

    Resource getRuptureCsv(String reference) throws IOException;
}
