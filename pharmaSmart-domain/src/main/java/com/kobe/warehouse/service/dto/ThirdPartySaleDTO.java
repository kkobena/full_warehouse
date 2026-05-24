package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.Util;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.Poste;
import com.kobe.warehouse.domain.RemiseClient;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

public class ThirdPartySaleDTO extends SaleDTO {

    private Integer ayantDroitId;
    private String ayantDroitFirstName;
    private String ayantDroitLastName;
    private String ayantDroitNum;
    private List<ClientTiersPayantDTO> tiersPayants = new ArrayList<>();
    private List<ThirdPartySaleLineDTO> thirdPartySaleLines = new ArrayList<>();
    private Integer partTiersPayant;
    private Integer partAssure;
    private String numBon;
    private boolean sansBon;
    private AssuredCustomerDTO ayantDroit;
    private boolean hasPriceOption;

    public ThirdPartySaleDTO() {
        super();
    }

    public ThirdPartySaleDTO(ThirdPartySales thirdPartySales) {
        super(thirdPartySales);
        super.setCategorie("VO");
        this.partAssure = thirdPartySales.getPartAssure();
        this.partTiersPayant = thirdPartySales.getPartTiersPayant();
        AssuredCustomer assuredCustomer = thirdPartySales.getAyantDroit();
        this.hasPriceOption = thirdPartySales.isHasPriceOption();
        if (assuredCustomer != null) {
            this.ayantDroitId = assuredCustomer.getId();
            this.ayantDroitFirstName = assuredCustomer.getFirstName();
            this.ayantDroitLastName = assuredCustomer.getLastName();
            this.ayantDroitNum = assuredCustomer.getNumAyantDroit();
            this.ayantDroit = new AssuredCustomerDTO(assuredCustomer);
        }
        buildTiersPayantDTOFromSale(thirdPartySales.getThirdPartySaleLines());
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Pré-remplit le builder avec les champs scalaires de l'entité.
     * Les champs suivants sont laissés à la charge de l'appelant :
     * customer, salesLines, payments, thirdPartySaleLines, tiersPayants, numBon.
     */
    public static Builder from(ThirdPartySales thirdPartySales) {
        Builder builder = new Builder();
        // --- champs SaleDTO ---
        SaleId saleId = thirdPartySales.getId();
        builder.saleId = saleId;
        builder.id = saleId.getId();
        builder.commentaire = thirdPartySales.getCommentaire();
        builder.canceled = thirdPartySales.isCanceled();
        builder.discountAmount = thirdPartySales.getDiscountAmount();
        builder.categorie = "VO";
        builder.salesAmount = thirdPartySales.getSalesAmount();
        builder.htAmount = thirdPartySales.getHtAmount();
        builder.netAmount = thirdPartySales.getNetAmount();
        builder.taxAmount = thirdPartySales.getTaxAmount();
        builder.costAmount = thirdPartySales.getCostAmount();
        builder.amountToBePaid = thirdPartySales.getAmountToBePaid();
        builder.statut = thirdPartySales.getStatut();
        builder.createdAt = thirdPartySales.getCreatedAt();
        builder.updatedAt = thirdPartySales.getUpdatedAt();
        AppUser user = thirdPartySales.getUser();
        builder.userFullName = user.getFirstName() + " " + user.getLastName();
        builder.numberTransaction = thirdPartySales.getNumberTransaction();
        builder.natureVente = thirdPartySales.getNatureVente();
        builder.typePrescription = thirdPartySales.getTypePrescription();
        UserDTO seller =  UserDTO.user(thirdPartySales.getSeller());
        UserDTO cassier =  UserDTO.user(thirdPartySales.getCaissier());
        builder.seller = seller;
        builder.cassier = cassier;
        builder.cassierId = cassier.getId();
        builder.sellerId = seller.getId();
        builder.differe = thirdPartySales.isDiffere();
        Poste caisse = thirdPartySales.getCaisse();
        if (Objects.nonNull(caisse)) {
            builder.caisseNum = caisse.getPosteNumber();
        }
        Poste lastCaisse = thirdPartySales.getLastCaisse();
        if (Objects.nonNull(lastCaisse)) {
            builder.caisseEndNum = lastCaisse.getPosteNumber();
        }
        builder.tvaEmbededs = Util.transformTvaEmbeded(thirdPartySales.getTvaEmbeded());
        builder.montantRendu = thirdPartySales.getMonnaie();
        builder.restToPay = thirdPartySales.getRestToPay();
        if (Objects.nonNull(thirdPartySales.getRemise())) {
            if (thirdPartySales.getRemise() instanceof RemiseProduit remiseProduit) {
                builder.remise = new RemiseProduitDTO(remiseProduit);
            } else {
                builder.remise = new RemiseClientDTO((RemiseClient) thirdPartySales.getRemise());
            }
        }
        // --- champs ThirdPartySaleDTO ---
        builder.partAssure = thirdPartySales.getPartAssure();
        builder.partTiersPayant = thirdPartySales.getPartTiersPayant();
        builder.hasPriceOption = thirdPartySales.isHasPriceOption();
        AssuredCustomer ayantDroit = thirdPartySales.getAyantDroit();
        if (ayantDroit != null) {
            builder.ayantDroitId = ayantDroit.getId();
            builder.ayantDroitFirstName = ayantDroit.getFirstName();
            builder.ayantDroitLastName = ayantDroit.getLastName();
            builder.ayantDroitNum = ayantDroit.getNumAyantDroit();
            builder.ayantDroit = new AssuredCustomerDTO(ayantDroit);
        }
        return builder;
    }

    public static Builder lite(ThirdPartySales thirdPartySales) {
        Builder builder = new Builder();
        // --- champs SaleDTO ---
        SaleId saleId = thirdPartySales.getId();
        builder.saleId = saleId;
        builder.id = saleId.getId();
        builder.commentaire = thirdPartySales.getCommentaire();
        builder.canceled = thirdPartySales.isCanceled();
        builder.discountAmount = thirdPartySales.getDiscountAmount();
        builder.categorie = "VO";
        builder.salesAmount = thirdPartySales.getSalesAmount();
        builder.htAmount = thirdPartySales.getHtAmount();
        builder.netAmount = thirdPartySales.getNetAmount();
        builder.taxAmount = thirdPartySales.getTaxAmount();
        builder.costAmount = thirdPartySales.getCostAmount();
        builder.amountToBePaid = thirdPartySales.getAmountToBePaid();
        builder.statut = thirdPartySales.getStatut();
        builder.createdAt = thirdPartySales.getCreatedAt();
        builder.updatedAt = thirdPartySales.getUpdatedAt();
        builder.numberTransaction = thirdPartySales.getNumberTransaction();
        builder.natureVente = thirdPartySales.getNatureVente();
        builder.typePrescription = thirdPartySales.getTypePrescription();
        builder.differe = thirdPartySales.isDiffere();
        builder.montantRendu = thirdPartySales.getMonnaie();
        builder.restToPay = thirdPartySales.getRestToPay();

        // --- champs ThirdPartySaleDTO ---
        builder.partAssure = thirdPartySales.getPartAssure();
        builder.partTiersPayant = thirdPartySales.getPartTiersPayant();
        builder.hasPriceOption = thirdPartySales.isHasPriceOption();


        return builder;
    }

    public AssuredCustomerDTO getAyantDroit() {
        return ayantDroit;
    }

    public ThirdPartySaleDTO setAyantDroit(AssuredCustomerDTO ayantDroit) {
        this.ayantDroit = ayantDroit;
        return this;
    }

    public boolean isHasPriceOption() {
        return hasPriceOption;
    }

    public void setHasPriceOption(boolean hasPriceOption) {
        this.hasPriceOption = hasPriceOption;
    }

    public List<ClientTiersPayantDTO> getTiersPayants() {
        if (tiersPayants != null) {
            tiersPayants.sort(Comparator.comparing(ClientTiersPayantDTO::getCategorie));
        }
        return tiersPayants;
    }

    public ThirdPartySaleDTO setTiersPayants(List<ClientTiersPayantDTO> tiersPayants) {
        this.tiersPayants = tiersPayants;
        return this;
    }

    private void buildTiersPayantDTOFromSale(List<ThirdPartySaleLine> thirdPartySaleLines) {
        List<ClientTiersPayantDTO> clientTiersPayantDTOS = new ArrayList<>();
        List<ThirdPartySaleLineDTO> thirdPartySaleLineDTOS = new ArrayList<>();
        thirdPartySaleLines.forEach(thirdPartySaleLine -> {
            if (thirdPartySaleLine.getClientTiersPayant().getPriorite() == PrioriteTiersPayant.R0) {
                this.numBon = thirdPartySaleLine.getNumBon();
            }

            thirdPartySaleLineDTOS.add(new ThirdPartySaleLineDTO(thirdPartySaleLine));
            clientTiersPayantDTOS.add(
                new ClientTiersPayantDTO(thirdPartySaleLine.getClientTiersPayant()).setNumBon(thirdPartySaleLine.getNumBon())
            );
        });
        if (StringUtils.isEmpty(this.numBon) && !thirdPartySaleLineDTOS.isEmpty()) {
            this.numBon = thirdPartySaleLines.getFirst().getNumBon();
        }
        this.setThirdPartySaleLines(thirdPartySaleLineDTOS);
        this.setTiersPayants(clientTiersPayantDTOS);
    }

    public Integer getAyantDroitId() {
        return ayantDroitId;
    }

    public ThirdPartySaleDTO setAyantDroitId(Integer ayantDroitId) {
        this.ayantDroitId = ayantDroitId;
        return this;
    }

    public String getAyantDroitFirstName() {
        return ayantDroitFirstName;
    }

    public ThirdPartySaleDTO setAyantDroitFirstName(String ayantDroitFirstName) {
        this.ayantDroitFirstName = ayantDroitFirstName;
        return this;
    }

    public String getAyantDroitLastName() {
        return ayantDroitLastName;
    }

    public ThirdPartySaleDTO setAyantDroitLastName(String ayantDroitLastName) {
        this.ayantDroitLastName = ayantDroitLastName;
        return this;
    }

    public String getAyantDroitNum() {
        return ayantDroitNum;
    }

    public ThirdPartySaleDTO setAyantDroitNum(String ayantDroitNum) {
        this.ayantDroitNum = ayantDroitNum;
        return this;
    }

    public List<ThirdPartySaleLineDTO> getThirdPartySaleLines() {
        return thirdPartySaleLines;
    }

    public ThirdPartySaleDTO setThirdPartySaleLines(List<ThirdPartySaleLineDTO> thirdPartySaleLines) {
        this.thirdPartySaleLines = thirdPartySaleLines;
        return this;
    }

    public Integer getPartTiersPayant() {
        return partTiersPayant;
    }

    public ThirdPartySaleDTO setPartTiersPayant(Integer partTiersPayant) {
        this.partTiersPayant = partTiersPayant;
        return this;
    }

    public Integer getPartAssure() {
        return partAssure;
    }

    public ThirdPartySaleDTO setPartAssure(Integer partAssure) {
        this.partAssure = partAssure;
        return this;
    }

    public String getNumBon() {
        return numBon;
    }

    public ThirdPartySaleDTO setNumBon(String numBon) {
        this.numBon = numBon;
        return this;
    }

    public boolean isSansBon() {
        return sansBon;
    }

    public ThirdPartySaleDTO setSansBon(boolean sansBon) {
        this.sansBon = sansBon;
        return this;
    }

    public static final class Builder {

        // ---- Champs SaleDTO ----
        private Long id;
        private SaleId saleId;
        private Integer discountAmount;
        private String numberTransaction;
        private Integer customerId;
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
        private boolean imported;
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
        private AssuredCustomerDTO customer;
        private UserDTO cassier;
        private UserDTO seller;
        private Integer cassierId;
        private Integer sellerId;
        private String caisseEndNum;
        private String caisseNum;
        private String categorie;
        private String posteName;
        private List<TvaEmbeded> tvaEmbededs = new ArrayList<>();
        private String commentaire;
        private boolean canceled;
        private MagasinDTO magasin;

        // ---- Champs ThirdPartySaleDTO ----
        private Integer ayantDroitId;
        private String ayantDroitFirstName;
        private String ayantDroitLastName;
        private String ayantDroitNum;
        private List<ClientTiersPayantDTO> tiersPayants = new ArrayList<>();
        private List<ThirdPartySaleLineDTO> thirdPartySaleLines = new ArrayList<>();
        private Integer partTiersPayant;
        private Integer partAssure;
        private String numBon;
        private boolean sansBon;
        private AssuredCustomerDTO ayantDroit;
        private boolean hasPriceOption;
        private String num;

        private Builder() {}

        Builder(ThirdPartySaleDTO existing) {
            // SaleDTO fields
            this.id = existing.getId();
            this.saleId = existing.getSaleId();
            this.discountAmount = existing.getDiscountAmount();
            this.numberTransaction = existing.getNumberTransaction();
            this.customerId = existing.getCustomerId();
            this.salesAmount = existing.getSalesAmount();
            this.userFullName = existing.getUserFullName();
            this.htAmount = existing.getHtAmount();
            this.netAmount = existing.getNetAmount();
            this.taxAmount = existing.getTaxAmount();
            this.costAmount = existing.getCostAmount();
            this.statut = existing.getStatut();
            this.createdAt = existing.getCreatedAt();
            this.updatedAt = existing.getUpdatedAt();
            this.salesLines = existing.getSalesLines();
            this.payments = existing.getPayments();
            this.sellerUserName = existing.getSellerUserName();
            this.canceledSale = existing.getCanceledSale();
            this.effectiveUpdateDate = existing.getEffectiveUpdateDate();
            this.toIgnore = existing.isToIgnore();
            this.ticketNumber = existing.getTicketNumber();
            this.payrollAmount = existing.getPayrollAmount();
            this.amountToBePaid = existing.getAmountToBePaid();
            this.amountToBeTakenIntoAccount = existing.getAmountToBeTakenIntoAccount();
            this.montantVerse = existing.getMontantVerse();
            this.montantRendu = existing.getMontantRendu();
            this.remise = existing.getRemise();
            this.restToPay = existing.getRestToPay();
            this.customerNum = existing.getCustomerNum();
            this.copy = existing.getCopy();
            this.imported = existing.isImported();
            this.differe = existing.isDiffere();
            this.avoir = existing.isAvoir();
            this.margeUg = existing.getMargeUg();
            this.montantttcUg = existing.getMontantttcUg();
            this.montantnetUg = existing.getMontantnetUg();
            this.montantTvaUg = existing.getMontantTvaUg();
            this.marge = existing.getMarge();
            this.montantRendue = existing.getMontantRendue();
            this.natureVente = existing.getNatureVente();
            this.typePrescription = existing.getTypePrescription();
            this.paymentStatus = existing.getPaymentStatus();
            this.customer = (AssuredCustomerDTO) existing.getCustomer();
            this.cassier = existing.getCassier();
            this.seller = existing.getSeller();
            this.cassierId = existing.getCassierId();
            this.sellerId = existing.getSellerId();
            this.caisseEndNum = existing.getCaisseEndNum();
            this.caisseNum = existing.getCaisseNum();
            this.categorie = existing.getCategorie();
            this.posteName = existing.getPosteName();
            this.tvaEmbededs = existing.getTvaEmbededs();
            this.commentaire = existing.getCommentaire();
            this.canceled = existing.isCanceled();
            this.magasin = existing.getMagasin();
            // ThirdPartySaleDTO fields
            this.ayantDroitId = existing.getAyantDroitId();
            this.ayantDroitFirstName = existing.getAyantDroitFirstName();
            this.ayantDroitLastName = existing.getAyantDroitLastName();
            this.ayantDroitNum = existing.getAyantDroitNum();
            this.tiersPayants = existing.getTiersPayants();
            this.thirdPartySaleLines = existing.getThirdPartySaleLines();
            this.partTiersPayant = existing.getPartTiersPayant();
            this.partAssure = existing.getPartAssure();
            this.numBon = existing.getNumBon();
            this.sansBon = existing.isSansBon();
            this.ayantDroit = existing.getAyantDroit();
            this.hasPriceOption = existing.isHasPriceOption();
        }

        // ---- Setters SaleDTO ----

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder saleId(SaleId saleId) {
            this.saleId = saleId;
            return this;
        }

        public Builder discountAmount(Integer discountAmount) {
            this.discountAmount = discountAmount;
            return this;
        }

        public Builder numberTransaction(String numberTransaction) {
            this.numberTransaction = numberTransaction;
            return this;
        }

        public Builder customerId(Integer customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder salesAmount(Integer salesAmount) {
            this.salesAmount = salesAmount;
            return this;
        }

        public Builder userFullName(String userFullName) {
            this.userFullName = userFullName;
            return this;
        }
        public Builder num(String num) {
            this.num = num;
            return this;
        }

        public Builder htAmount(Integer htAmount) {
            this.htAmount = htAmount;
            return this;
        }

        public Builder netAmount(Integer netAmount) {
            this.netAmount = netAmount;
            return this;
        }

        public Builder taxAmount(Integer taxAmount) {
            this.taxAmount = taxAmount;
            return this;
        }

        public Builder costAmount(Integer costAmount) {
            this.costAmount = costAmount;
            return this;
        }

        public Builder statut(SalesStatut statut) {
            this.statut = statut;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder salesLines(List<SaleLineDTO> salesLines) {
            this.salesLines = salesLines;
            return this;
        }

        public Builder payments(List<PaymentDTO> payments) {
            this.payments = payments;
            return this;
        }

        public Builder sellerUserName(String sellerUserName) {
            this.sellerUserName = sellerUserName;
            return this;
        }

        public Builder canceledSale(SaleDTO canceledSale) {
            this.canceledSale = canceledSale;
            return this;
        }

        public Builder effectiveUpdateDate(LocalDateTime effectiveUpdateDate) {
            this.effectiveUpdateDate = effectiveUpdateDate;
            return this;
        }

        public Builder toIgnore(boolean toIgnore) {
            this.toIgnore = toIgnore;
            return this;
        }

        public Builder ticketNumber(String ticketNumber) {
            this.ticketNumber = ticketNumber;
            return this;
        }

        public Builder payrollAmount(Integer payrollAmount) {
            this.payrollAmount = payrollAmount;
            return this;
        }

        public Builder amountToBePaid(Integer amountToBePaid) {
            this.amountToBePaid = amountToBePaid;
            return this;
        }

        public Builder amountToBeTakenIntoAccount(Integer amountToBeTakenIntoAccount) {
            this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
            return this;
        }

        public Builder montantVerse(Integer montantVerse) {
            this.montantVerse = montantVerse;
            return this;
        }

        public Builder montantRendu(Integer montantRendu) {
            this.montantRendu = montantRendu;
            return this;
        }

        public Builder remise(RemiseDTO remise) {
            this.remise = remise;
            return this;
        }

        public Builder restToPay(Integer restToPay) {
            this.restToPay = restToPay;
            return this;
        }

        public Builder customerNum(String customerNum) {
            this.customerNum = customerNum;
            return this;
        }

        public Builder copy(Boolean copy) {
            this.copy = copy;
            return this;
        }

        public Builder imported(boolean imported) {
            this.imported = imported;
            return this;
        }

        public Builder differe(boolean differe) {
            this.differe = differe;
            return this;
        }

        public Builder avoir(boolean avoir) {
            this.avoir = avoir;
            return this;
        }

        public Builder margeUg(Integer margeUg) {
            this.margeUg = margeUg;
            return this;
        }

        public Builder montantttcUg(Integer montantttcUg) {
            this.montantttcUg = montantttcUg;
            return this;
        }

        public Builder montantnetUg(Integer montantnetUg) {
            this.montantnetUg = montantnetUg;
            return this;
        }

        public Builder montantTvaUg(Integer montantTvaUg) {
            this.montantTvaUg = montantTvaUg;
            return this;
        }

        public Builder marge(Integer marge) {
            this.marge = marge;
            return this;
        }

        public Builder montantRendue(int montantRendue) {
            this.montantRendue = montantRendue;
            return this;
        }

        public Builder natureVente(NatureVente natureVente) {
            this.natureVente = natureVente;
            return this;
        }

        public Builder typePrescription(TypePrescription typePrescription) {
            this.typePrescription = typePrescription;
            return this;
        }

        public Builder paymentStatus(PaymentStatus paymentStatus) {
            this.paymentStatus = paymentStatus;
            return this;
        }

        public Builder customer(AssuredCustomerDTO customer) {
            this.customer = customer;
            return this;
        }

        public Builder cassier(UserDTO cassier) {
            this.cassier = cassier;
            return this;
        }

        public Builder seller(UserDTO seller) {
            this.seller = seller;
            return this;
        }

        public Builder cassierId(Integer cassierId) {
            this.cassierId = cassierId;
            return this;
        }

        public Builder sellerId(Integer sellerId) {
            this.sellerId = sellerId;
            return this;
        }

        public Builder caisseEndNum(String caisseEndNum) {
            this.caisseEndNum = caisseEndNum;
            return this;
        }

        public Builder caisseNum(String caisseNum) {
            this.caisseNum = caisseNum;
            return this;
        }

        public Builder categorie(String categorie) {
            this.categorie = categorie;
            return this;
        }

        public Builder posteName(String posteName) {
            this.posteName = posteName;
            return this;
        }

        public Builder tvaEmbededs(List<TvaEmbeded> tvaEmbededs) {
            this.tvaEmbededs = tvaEmbededs;
            return this;
        }

        public Builder commentaire(String commentaire) {
            this.commentaire = commentaire;
            return this;
        }

        public Builder canceled(boolean canceled) {
            this.canceled = canceled;
            return this;
        }

        public Builder magasin(MagasinDTO magasin) {
            this.magasin = magasin;
            return this;
        }

        // ---- Setters ThirdPartySaleDTO ----

        public Builder ayantDroitId(Integer ayantDroitId) {
            this.ayantDroitId = ayantDroitId;
            return this;
        }

        public Builder ayantDroitFirstName(String ayantDroitFirstName) {
            this.ayantDroitFirstName = ayantDroitFirstName;
            return this;
        }

        public Builder ayantDroitLastName(String ayantDroitLastName) {
            this.ayantDroitLastName = ayantDroitLastName;
            return this;
        }

        public Builder ayantDroitNum(String ayantDroitNum) {
            this.ayantDroitNum = ayantDroitNum;
            return this;
        }

        public Builder tiersPayants(List<ClientTiersPayantDTO> tiersPayants) {
            this.tiersPayants = tiersPayants;
            return this;
        }

        public Builder thirdPartySaleLines(List<ThirdPartySaleLineDTO> thirdPartySaleLines) {
            this.thirdPartySaleLines = thirdPartySaleLines;
            return this;
        }

        public Builder partTiersPayant(Integer partTiersPayant) {
            this.partTiersPayant = partTiersPayant;
            return this;
        }

        public Builder partAssure(Integer partAssure) {
            this.partAssure = partAssure;
            return this;
        }

        public Builder numBon(String numBon) {
            this.numBon = numBon;
            return this;
        }

        public Builder sansBon(boolean sansBon) {
            this.sansBon = sansBon;
            return this;
        }

        public Builder ayantDroit(AssuredCustomerDTO ayantDroit) {
            this.ayantDroit = ayantDroit;
            return this;
        }

        public Builder hasPriceOption(boolean hasPriceOption) {
            this.hasPriceOption = hasPriceOption;
            return this;
        }

        public ThirdPartySaleDTO build() {
            ThirdPartySaleDTO dto = new ThirdPartySaleDTO();
            // SaleDTO fields
            dto.setId(id);
            dto.setSaleId(saleId);
            dto.setDiscountAmount(discountAmount);
            dto.setNumberTransaction(numberTransaction);
            dto.setCustomerId(customerId);
            dto.setSalesAmount(salesAmount);
            dto.setUserFullName(userFullName);
            dto.setHtAmount(htAmount);
            dto.setNetAmount(netAmount);
            dto.setTaxAmount(taxAmount);
            dto.setCostAmount(costAmount);
            dto.setStatut(statut);
            dto.setCreatedAt(createdAt);
            dto.setUpdatedAt(updatedAt);
            dto.setSalesLines(salesLines);
            dto.setPayments(payments);
            dto.setSellerUserName(sellerUserName);
            dto.setCanceledSale(canceledSale);
            dto.setEffectiveUpdateDate(effectiveUpdateDate);
            dto.setToIgnore(toIgnore);
            dto.setTicketNumber(ticketNumber);
            dto.setPayrollAmount(payrollAmount);
            dto.setAmountToBePaid(amountToBePaid);
            dto.setAmountToBeTakenIntoAccount(amountToBeTakenIntoAccount);
            dto.setMontantVerse(montantVerse);
            dto.setMontantRendu(montantRendu);
            dto.setRemise(remise);
            dto.setRestToPay(restToPay);
            dto.setCustomerNum(customerNum);
            dto.setCopy(copy);
            dto.setImported(imported);
            dto.setDiffere(differe);
            dto.setAvoir(avoir);
            dto.setMargeUg(margeUg);
            dto.setMontantttcUg(montantttcUg);
            dto.setMontantnetUg(montantnetUg);
            dto.setMontantTvaUg(montantTvaUg);
            dto.setMarge(marge);
            dto.setMontantRendue(montantRendue);
            dto.setNatureVente(natureVente);
            dto.setTypePrescription(typePrescription);
            dto.setPaymentStatus(paymentStatus);
            dto.setCustomer(customer);
            dto.setCassier(cassier);
            dto.setSeller(seller);
            dto.setCassierId(cassierId);
            dto.setSellerId(sellerId);
            dto.setCaisseEndNum(caisseEndNum);
            dto.setCaisseNum(caisseNum);
            dto.setCategorie(categorie);
            dto.setPosteName(posteName);
            dto.setTvaEmbededs(tvaEmbededs);
            dto.setCommentaire(commentaire);
            dto.setCanceled(canceled);
            dto.setMagasin(magasin);
            // ThirdPartySaleDTO fields
            if (!Objects.equals(customer.getId(), ayantDroitId)){
                dto.setAyantDroitId(ayantDroitId);
                dto.setAyantDroit(ayantDroit);
            }

            dto.setAyantDroitFirstName(ayantDroitFirstName);
            dto.setAyantDroitLastName(ayantDroitLastName);
            dto.setAyantDroitNum(ayantDroitNum);
            dto.setTiersPayants(tiersPayants);
            dto.setThirdPartySaleLines(thirdPartySaleLines);
            dto.setPartTiersPayant(partTiersPayant);
            dto.setPartAssure(partAssure);
            dto.setNumBon(numBon);
            dto.setSansBon(sansBon);
            dto.setHasPriceOption(hasPriceOption);
            return dto;
        }

        public ThirdPartySaleDTO buildLite() {
            ThirdPartySaleDTO dto = new ThirdPartySaleDTO();
            // SaleDTO fields
            dto.setId(id);
            dto.setSaleId(saleId);
            dto.setDiscountAmount(discountAmount);
            dto.setNumberTransaction(numberTransaction);
            dto.setSalesAmount(salesAmount);
            dto.setHtAmount(htAmount);
            dto.setNetAmount(netAmount);
            dto.setTaxAmount(taxAmount);
            dto.setCostAmount(costAmount);
            dto.setCreatedAt(createdAt);
            dto.setUpdatedAt(updatedAt);
            dto.setPayrollAmount(payrollAmount);
            dto.setAmountToBePaid(amountToBePaid);
            dto.setRestToPay(restToPay);
            dto.setDiffere(differe);
            dto.setAvoir(avoir);
            dto.setNatureVente(natureVente);
            dto.setTypePrescription(typePrescription);
            dto.setCategorie(categorie);
            dto.setPartTiersPayant(partTiersPayant);
            dto.setPartAssure(partAssure);
            dto.setNumBon(numBon);
            dto.setSansBon(sansBon);
            dto.setHasPriceOption(hasPriceOption);
            return dto;
        }
    }
}
