package com.kobe.warehouse.service;

import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.ImportationRepository;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ImportationCommandService {
  private final CommandeRepository commandeRepository;
  private final OrderLineRepository orderLineRepository;
  private final StorageService storageService;
  private final ProduitRepository produitRepository;
  private final TransactionTemplate transactionTemplate;
  private final StockProduitRepository stockProduitRepository;
  private final FournisseurProduitRepository fournisseurProduitRepository;
  private final ImportationRepository importationRepository;

  public ImportationCommandService(
      CommandeRepository commandeRepository,
      OrderLineRepository orderLineRepository,
      StorageService storageService,
      ProduitRepository produitRepository,
      TransactionTemplate transactionTemplate,
      StockProduitRepository stockProduitRepository,
      FournisseurProduitRepository fournisseurProduitRepository,
      ImportationRepository importationRepository) {
    this.commandeRepository = commandeRepository;
    this.orderLineRepository = orderLineRepository;
    this.storageService = storageService;
    this.produitRepository = produitRepository;
    this.transactionTemplate = transactionTemplate;
    this.stockProduitRepository = stockProduitRepository;
    this.fournisseurProduitRepository = fournisseurProduitRepository;
    this.importationRepository = importationRepository;
  }
}
