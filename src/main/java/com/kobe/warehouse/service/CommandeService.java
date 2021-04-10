package com.kobe.warehouse.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.InventoryTransaction;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.InventoryTransactionRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.dto.ProduitDTO;

@Service
@Transactional
public class CommandeService {
	@Autowired
	private CommandeRepository commandeRepository;
	@Autowired
	private ReferenceService referenceService;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ProduitRepository produitRepository;
	@Autowired
	private InventoryTransactionRepository inventoryTransactionRepository;

	public Commande createCommande(Set<ProduitDTO> produitDTO) {
		Optional<User> user = SecurityUtils.getCurrentUserLogin()
				.flatMap(login -> userRepository.findOneByLogin(login));

		Commande commande = new Commande();
		commande.setCreatedAt(Instant.now());
		commande.setUpdatedAt(Instant.now());
		commande.setReceiptDate(LocalDate.now());
		commande.setOrderRefernce(referenceService.buildNumCommande());
		commande.setDateDimension(Constants.DateDimension(LocalDate.now()));
		commande.setDiscountAmount(0);
		commande.setGrossAmount(0);
		commande.setTaxAmount(0);
		commande.setNetAmount(0);
		commande.setOrderStatus(OrderStatut.RECEIVED);
		int orderAmount = 0;
		for (ProduitDTO produitDTO2 : produitDTO) {
			Produit produit = produitRepository.getOne(produitDTO2.getId());
			int quantityBefor = produit.getQuantity();
			int quantityAfter = produitDTO2.getQuantityReceived() + quantityBefor;
			OrderLine orderLine = new OrderLine();
			orderLine.setReceiptDate(LocalDate.now());
			orderLine.setProduit(produit);
			orderLine.setCreatedAt(Instant.now());
			orderLine.setUpdatedAt(Instant.now());
			orderLine.setDiscountAmount(0);
			orderLine.setNetAmount(0);
			orderLine.setTaxAmount(0);
			orderLine.setGrossAmount(0);
			orderLine.setQuantityReturned(0);
			orderLine.setQuantityReceived(produitDTO2.getQuantityReceived());
			orderLine.setQuantityRequested(produitDTO2.getQuantityReceived());
			orderLine.setCostAmount(produitDTO2.getCostAmount());
			orderLine.setOrderAmount(produitDTO2.getQuantityReceived() * produitDTO2.getCostAmount());
			commande.addOrderLine(orderLine);
			orderAmount += orderLine.getOrderAmount();
			produit.setQuantity(quantityAfter);
			produit.setCostAmount(produitDTO2.getCostAmount());
			produit.setUpdatedAt(orderLine.getCreatedAt());
			produitRepository.save(produit);
			InventoryTransaction inventoryTransaction = inventoryTransactionRepository
					.buildInventoryTransaction(orderLine, user.get());
			inventoryTransaction.setQuantityBefor(quantityBefor);
			inventoryTransaction.setQuantityAfter(quantityAfter);
			inventoryTransaction.setRegularUnitPrice(produit.getRegularUnitPrice());
			inventoryTransaction.setCostAmount(produitDTO2.getCostAmount());
			inventoryTransactionRepository.save(inventoryTransaction);
		}
		commande.setOrderAmount(orderAmount);

		commande.setUser(user.get());

		return commandeRepository.save(commande);
	}

	public Optional<Commande> getCommande(Long id) {
		return commandeRepository.findById(id);
	}

	public Page<Commande> getAllCommandes(Pageable pageable) {
		return commandeRepository.findAll(pageable);
	}
}
