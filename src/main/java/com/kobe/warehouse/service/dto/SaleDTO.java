package com.kobe.warehouse.service.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.kobe.warehouse.Util;
import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Poste;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = CashSaleDTO.class, name = "VNO"),
  @JsonSubTypes.Type(value = ThirdPartySaleDTO.class, name = "VO")
})
public class SaleDTO implements Serializable {
  private Long id;
  private Integer discountAmount;
  private String numberTransaction;
  private Long customerId;
  private Integer salesAmount;
  private String userFullName;
  private Integer htAmount;
  private Integer netAmount;
  private Integer taxAmount;
  private Integer costAmount;
  private SalesStatut statut;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private List<SaleLineDTO> salesLines = new ArrayList<>();
  private List<PaymentDTO> payments = new ArrayList<>();
  private String sellerUserName;
  private SaleDTO canceledSale;
  private LocalDateTime effectiveUpdateDate;
  private boolean toIgnore;
  private String ticketNumber;
  private Integer payrollAmount;
  private Integer amountToBePaid;
  private Integer amountToBeTakenIntoAccount;
  private Integer montantVerse;
  private Integer montantRendu;
  private RemiseDTO remise;
  private Integer restToPay;
  private String customerNum;
  private Boolean copy = false;
  private boolean imported = false;
  private boolean differe;
  private boolean avoir;
  private Integer margeUg = 0;
  private Integer montantttcUg = 0;
  private Integer montantnetUg = 0;
  private Integer montantTvaUg = 0;
  private Integer marge = 0;
  private int montantRendue;
  private NatureVente natureVente;
  private TypePrescription typePrescription;
  private PaymentStatus paymentStatus;
  private CustomerDTO customer;
  private UserDTO cassier, seller;
  private Long cassierId;
  private Long sellerId;
  private List<TicketDTO> tickets = new ArrayList<>();
  private String caisseEndNum;
  private String caisseNum;
  private String categorie;
  private String posteName;
  private List<TvaEmbeded> tvaEmbededs = new ArrayList<>();
  private String commentaire;

  public SaleDTO() {}

  public SaleDTO(Sales sale) {
    this.id = sale.getId();
    this.commentaire = sale.getCommentaire();
    this.discountAmount = sale.getDiscountAmount();
    if (sale instanceof ThirdPartySales thirdPartySales) {
      this.customer = new AssuredCustomerDTO((AssuredCustomer) thirdPartySales.getCustomer());
      this.categorie = "VO";
    } else if (sale instanceof CashSale cashSale) {
      if (cashSale.getCustomer() != null) {
        this.customer = new UninsuredCustomerDTO((UninsuredCustomer) cashSale.getCustomer());
      }
      this.categorie = "VNO";
    }
    if (Objects.nonNull(this.customer)) {
      this.customerId = this.customer.getId();
    }
    if (StringUtils.isEmpty(this.categorie)) {
      switch (sale.getNatureVente()) {
        case ASSURANCE:
        case CARNET:
          this.categorie = "VO";
          break;
        case COMPTANT:
          this.categorie = "VNO";
          break;
        default:
          break;
      }
    }
    this.salesAmount = sale.getSalesAmount();
    this.htAmount = sale.getHtAmount();
    this.netAmount = sale.getNetAmount();
    this.taxAmount = sale.getTaxAmount();
    this.costAmount = sale.getCostAmount();
    this.amountToBePaid = sale.getAmountToBePaid();
    this.statut = sale.getStatut();
    this.createdAt = sale.getCreatedAt();
    this.updatedAt = sale.getUpdatedAt();
    this.salesLines =
        sale.getSalesLines().stream()
            .map(SaleLineDTO::new)
            .sorted(Comparator.comparing(SaleLineDTO::getUpdatedAt, Comparator.reverseOrder()))
            .toList();
    this.payments = sale.getPayments().stream().map(PaymentDTO::new).toList();
    User user = sale.getUser();
    this.userFullName = user.getFirstName() + " " + user.getLastName();
    this.numberTransaction = sale.getNumberTransaction();
    this.natureVente = sale.getNatureVente();
    this.typePrescription = sale.getTypePrescription();
    this.seller = new UserDTO(sale.getSeller());
    this.cassier = new UserDTO(sale.getCassier());
    this.cassierId = this.cassier.getId();
    this.sellerId = this.seller.getId();
    this.differe = sale.isDiffere();
    Poste init = sale.getCaisse();
    if (Objects.nonNull(init)) {
      this.caisseNum = init.getPosteNumber();
    }
    Poste p = sale.getLastCaisse();
    if (Objects.nonNull(p)) {
      this.caisseEndNum = p.getPosteNumber();
    }

    this.tvaEmbededs = Util.transformTvaEmbeded(sale.getTvaEmbeded());
    this.montantRendu = sale.getMonnaie();
    this.restToPay = sale.getRestToPay();
    //  this.tickets=sale.getTickets().stream().map(TicketDTO::new).collect(Collectors.toList());
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Integer getDiscountAmount() {
    return discountAmount;
  }

  public void setDiscountAmount(Integer discountAmount) {
    this.discountAmount = discountAmount;
  }

  public String getNumberTransaction() {
    return numberTransaction;
  }

  public void setNumberTransaction(String numberTransaction) {
    this.numberTransaction = numberTransaction;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Long customerId) {
    this.customerId = customerId;
  }

  public Integer getSalesAmount() {
    return salesAmount;
  }

  public void setSalesAmount(Integer salesAmount) {
    this.salesAmount = salesAmount;
  }

  public String getUserFullName() {
    return userFullName;
  }

  public void setUserFullName(String userFullName) {
    this.userFullName = userFullName;
  }

  public Integer getHtAmount() {
    return htAmount;
  }

  public SaleDTO setHtAmount(Integer htAmount) {
    this.htAmount = htAmount;
    return this;
  }

  public Integer getNetAmount() {
    return netAmount;
  }

  public void setNetAmount(Integer netAmount) {
    this.netAmount = netAmount;
  }

  public Integer getTaxAmount() {
    return taxAmount;
  }

  public void setTaxAmount(Integer taxAmount) {
    this.taxAmount = taxAmount;
  }

  public Integer getCostAmount() {
    return costAmount;
  }

  public void setCostAmount(Integer costAmount) {
    this.costAmount = costAmount;
  }

  public SalesStatut getStatut() {
    return statut;
  }

  public void setStatut(SalesStatut statut) {
    this.statut = statut;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public List<SaleLineDTO> getSalesLines() {
    return salesLines;
  }

  public SaleDTO setSalesLines(List<SaleLineDTO> salesLines) {
    this.salesLines = salesLines;
    return this;
  }

  public List<PaymentDTO> getPayments() {
    return payments;
  }

  public SaleDTO setPayments(List<PaymentDTO> payments) {
    this.payments = payments;
    return this;
  }

  public String getSellerUserName() {
    return sellerUserName;
  }

  public SaleDTO setSellerUserName(String sellerUserName) {
    this.sellerUserName = sellerUserName;
    return this;
  }

  public SaleDTO getCanceledSale() {
    return canceledSale;
  }

  public SaleDTO setCanceledSale(SaleDTO canceledSale) {
    this.canceledSale = canceledSale;
    return this;
  }

  public LocalDateTime getEffectiveUpdateDate() {
    return effectiveUpdateDate;
  }

  public SaleDTO setEffectiveUpdateDate(LocalDateTime effectiveUpdateDate) {
    this.effectiveUpdateDate = effectiveUpdateDate;
    return this;
  }

  public boolean isToIgnore() {
    return toIgnore;
  }

  public SaleDTO setToIgnore(boolean toIgnore) {
    this.toIgnore = toIgnore;
    return this;
  }

  public String getTicketNumber() {
    return ticketNumber;
  }

  public SaleDTO setTicketNumber(String ticketNumber) {
    this.ticketNumber = ticketNumber;
    return this;
  }

  public Integer getPayrollAmount() {
    return payrollAmount;
  }

  public SaleDTO setPayrollAmount(Integer payrollAmount) {
    this.payrollAmount = payrollAmount;
    return this;
  }

  public Integer getAmountToBePaid() {
    return amountToBePaid;
  }

  public SaleDTO setAmountToBePaid(Integer amountToBePaid) {
    this.amountToBePaid = amountToBePaid;
    return this;
  }

  public Integer getAmountToBeTakenIntoAccount() {
    return amountToBeTakenIntoAccount;
  }

  public SaleDTO setAmountToBeTakenIntoAccount(Integer amountToBeTakenIntoAccount) {
    this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
    return this;
  }

  public Integer getMontantVerse() {
    return montantVerse;
  }

  public SaleDTO setMontantVerse(Integer montantVerse) {
    this.montantVerse = montantVerse;
    return this;
  }

  public Integer getMontantRendu() {
    return montantRendu;
  }

  public SaleDTO setMontantRendu(Integer montantRendu) {
    this.montantRendu = montantRendu;
    return this;
  }

  public RemiseDTO getRemise() {
    return remise;
  }

  public SaleDTO setRemise(RemiseDTO remise) {
    this.remise = remise;
    return this;
  }

  public Integer getRestToPay() {
    return restToPay;
  }

  public SaleDTO setRestToPay(Integer restToPay) {
    this.restToPay = restToPay;
    return this;
  }

  public String getCustomerNum() {
    return customerNum;
  }

  public SaleDTO setCustomerNum(String customerNum) {
    this.customerNum = customerNum;
    return this;
  }

  public Boolean getCopy() {
    return copy;
  }

  public SaleDTO setCopy(Boolean copy) {
    this.copy = copy;
    return this;
  }

  public boolean isImported() {
    return imported;
  }

  public SaleDTO setImported(boolean imported) {
    this.imported = imported;
    return this;
  }

  public boolean isDiffere() {
    return differe;
  }

  public SaleDTO setDiffere(boolean differe) {
    this.differe = differe;
    return this;
  }

  public boolean isAvoir() {
    return avoir;
  }

  public SaleDTO setAvoir(boolean avoir) {
    this.avoir = avoir;
    return this;
  }

  public Integer getMargeUg() {
    return margeUg;
  }

  public SaleDTO setMargeUg(Integer margeUg) {
    this.margeUg = margeUg;
    return this;
  }

  public Integer getMontantttcUg() {
    return montantttcUg;
  }

  public SaleDTO setMontantttcUg(Integer montantttcUg) {
    this.montantttcUg = montantttcUg;
    return this;
  }

  public Integer getMontantnetUg() {
    return montantnetUg;
  }

  public SaleDTO setMontantnetUg(Integer montantnetUg) {
    this.montantnetUg = montantnetUg;
    return this;
  }

  public Integer getMontantTvaUg() {
    return montantTvaUg;
  }

  public SaleDTO setMontantTvaUg(Integer montantTvaUg) {
    this.montantTvaUg = montantTvaUg;
    return this;
  }

  public Integer getMarge() {
    return marge;
  }

  public SaleDTO setMarge(Integer marge) {
    this.marge = marge;
    return this;
  }

  public int getMontantRendue() {
    return montantRendue;
  }

  public SaleDTO setMontantRendue(int montantRendue) {
    this.montantRendue = montantRendue;
    return this;
  }

  public NatureVente getNatureVente() {
    return natureVente;
  }

  public SaleDTO setNatureVente(NatureVente natureVente) {
    this.natureVente = natureVente;
    return this;
  }

  public TypePrescription getTypePrescription() {
    return typePrescription;
  }

  public SaleDTO setTypePrescription(TypePrescription typePrescription) {
    this.typePrescription = typePrescription;
    return this;
  }

  public PaymentStatus getPaymentStatus() {
    return paymentStatus;
  }

  public SaleDTO setPaymentStatus(PaymentStatus paymentStatus) {
    this.paymentStatus = paymentStatus;
    return this;
  }

  public CustomerDTO getCustomer() {
    return customer;
  }

  public SaleDTO setCustomer(CustomerDTO customer) {
    this.customer = customer;
    return this;
  }

  public UserDTO getCassier() {
    return cassier;
  }

  public SaleDTO setCassier(UserDTO cassier) {
    this.cassier = cassier;
    return this;
  }

  public UserDTO getSeller() {
    return seller;
  }

  public SaleDTO setSeller(UserDTO seller) {
    this.seller = seller;
    return this;
  }

  public Long getCassierId() {
    return cassierId;
  }

  public SaleDTO setCassierId(Long cassierId) {
    this.cassierId = cassierId;
    return this;
  }

  public Long getSellerId() {
    return sellerId;
  }

  public SaleDTO setSellerId(Long sellerId) {
    this.sellerId = sellerId;
    return this;
  }

  public List<TicketDTO> getTickets() {
    return tickets;
  }

  public SaleDTO setTickets(List<TicketDTO> tickets) {
    this.tickets = tickets;
    return this;
  }

  public String getCaisseEndNum() {
    return caisseEndNum;
  }

  public SaleDTO setCaisseEndNum(String caisseEndNum) {
    this.caisseEndNum = caisseEndNum;
    return this;
  }

  public String getCaisseNum() {
    return caisseNum;
  }

  public SaleDTO setCaisseNum(String caisseNum) {
    this.caisseNum = caisseNum;
    return this;
  }

  public String getCategorie() {
    return categorie;
  }

  public SaleDTO setCategorie(String categorie) {
    this.categorie = categorie;
    return this;
  }

  public String getPosteName() {
    return posteName;
  }

  public SaleDTO setPosteName(String posteName) {
    this.posteName = posteName;
    return this;
  }

  public List<TvaEmbeded> getTvaEmbededs() {
    return tvaEmbededs;
  }

  public SaleDTO setTvaEmbededs(List<TvaEmbeded> tvaEmbededs) {
    this.tvaEmbededs = tvaEmbededs;
    return this;
  }

  public String getCommentaire() {
    return commentaire;
  }

  public SaleDTO setCommentaire(String commentaire) {
    this.commentaire = commentaire;
    return this;
  }
}
