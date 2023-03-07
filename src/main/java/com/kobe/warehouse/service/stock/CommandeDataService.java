package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.service.dto.CommandeDTO;
import com.kobe.warehouse.service.dto.CommandeEntryDTO;
import com.kobe.warehouse.service.dto.CommandeLiteDTO;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.dto.filter.CommandeFilterDTO;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommandeDataService {
  CommandeDTO findOneById(Long id);

  Optional<Commande> getOneById(Long id);

  Optional<CommandeEntryDTO> getCommandeById(Long id);

  Resource exportCommandeToCsv(Long id) throws IOException;

  Resource exportCommandeToPdf(Long id) throws IOException;

  List<OrderLineDTO> filterCommandeLines(CommandeFilterDTO commandeFilter);

  Page<CommandeLiteDTO> fetchCommandes(CommandeFilterDTO commandeFilterDTO, Pageable pageable);

  Page<OrderLineDTO> filterCommandeLines(Long commandeId, Pageable pageable);

  Resource getRuptureCsv(String reference) throws IOException;
}
