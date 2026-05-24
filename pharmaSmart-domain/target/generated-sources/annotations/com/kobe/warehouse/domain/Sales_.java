package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.OrigineVente;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Sales}
 **/
@StaticMetamodel(Sales.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Sales_ {

	
	/**
	 * @see #salesLines
	 **/
	public static final String SALES_LINES = "salesLines";
	
	/**
	 * @see #remise
	 **/
	public static final String REMISE = "remise";
	
	/**
	 * @see #user
	 **/
	public static final String USER = "user";
	
	/**
	 * @see #seller
	 **/
	public static final String SELLER = "seller";
	
	/**
	 * @see #caissier
	 **/
	public static final String CAISSIER = "caissier";
	
	/**
	 * @see #payments
	 **/
	public static final String PAYMENTS = "payments";
	
	/**
	 * @see #magasin
	 **/
	public static final String MAGASIN = "magasin";
	
	/**
	 * @see #canceledSale
	 **/
	public static final String CANCELED_SALE = "canceledSale";
	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #saleDate
	 **/
	public static final String SALE_DATE = "saleDate";
	
	/**
	 * @see #type
	 **/
	public static final String TYPE = "type";
	
	/**
	 * @see #numberTransaction
	 **/
	public static final String NUMBER_TRANSACTION = "numberTransaction";
	
	/**
	 * @see #discountAmount
	 **/
	public static final String DISCOUNT_AMOUNT = "discountAmount";
	
	/**
	 * @see #salesAmount
	 **/
	public static final String SALES_AMOUNT = "salesAmount";
	
	/**
	 * @see #htAmount
	 **/
	public static final String HT_AMOUNT = "htAmount";
	
	/**
	 * @see #netAmount
	 **/
	public static final String NET_AMOUNT = "netAmount";
	
	/**
	 * @see #taxAmount
	 **/
	public static final String TAX_AMOUNT = "taxAmount";
	
	/**
	 * @see #costAmount
	 **/
	public static final String COST_AMOUNT = "costAmount";
	
	/**
	 * @see #amountToBePaid
	 **/
	public static final String AMOUNT_TO_BE_PAID = "amountToBePaid";
	
	/**
	 * @see #payrollAmount
	 **/
	public static final String PAYROLL_AMOUNT = "payrollAmount";
	
	/**
	 * @see #restToPay
	 **/
	public static final String REST_TO_PAY = "restToPay";
	
	/**
	 * @see #amountToBeTakenIntoAccount
	 **/
	public static final String AMOUNT_TO_BE_TAKEN_INTO_ACCOUNT = "amountToBeTakenIntoAccount";
	
	/**
	 * @see #statut
	 **/
	public static final String STATUT = "statut";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";
	
	/**
	 * @see #updatedAt
	 **/
	public static final String UPDATED_AT = "updatedAt";
	
	/**
	 * @see #effectiveUpdateDate
	 **/
	public static final String EFFECTIVE_UPDATE_DATE = "effectiveUpdateDate";
	
	/**
	 * @see #toIgnore
	 **/
	public static final String TO_IGNORE = "toIgnore";
	
	/**
	 * @see #copy
	 **/
	public static final String COPY = "copy";
	
	/**
	 * @see #imported
	 **/
	public static final String IMPORTED = "imported";
	
	/**
	 * @see #paymentStatus
	 **/
	public static final String PAYMENT_STATUS = "paymentStatus";
	
	/**
	 * @see #natureVente
	 **/
	public static final String NATURE_VENTE = "natureVente";
	
	/**
	 * @see #origineVente
	 **/
	public static final String ORIGINE_VENTE = "origineVente";
	
	/**
	 * @see #typePrescription
	 **/
	public static final String TYPE_PRESCRIPTION = "typePrescription";
	
	/**
	 * @see #differe
	 **/
	public static final String DIFFERE = "differe";
	
	/**
	 * @see #categorieChiffreAffaire
	 **/
	public static final String CATEGORIE_CHIFFRE_AFFAIRE = "categorieChiffreAffaire";
	
	/**
	 * @see #caisse
	 **/
	public static final String CAISSE = "caisse";
	
	/**
	 * @see #lastCaisse
	 **/
	public static final String LAST_CAISSE = "lastCaisse";
	
	/**
	 * @see #customer
	 **/
	public static final String CUSTOMER = "customer";
	
	/**
	 * @see #canceled
	 **/
	public static final String CANCELED = "canceled";
	
	/**
	 * @see #cancelComment
	 **/
	public static final String CANCEL_COMMENT = "cancelComment";
	
	/**
	 * @see #cancelledBy
	 **/
	public static final String CANCELLED_BY = "cancelledBy";
	
	/**
	 * @see #tvaEmbeded
	 **/
	public static final String TVA_EMBEDED = "tvaEmbeded";
	
	/**
	 * @see #commentaire
	 **/
	public static final String COMMENTAIRE = "commentaire";
	
	/**
	 * @see #monnaie
	 **/
	public static final String MONNAIE = "monnaie";
	
	/**
	 * @see #cashRegister
	 **/
	public static final String CASH_REGISTER = "cashRegister";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Sales}
	 **/
	public static volatile EntityType<Sales> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#salesLines}
	 **/
	public static volatile SetAttribute<Sales, SalesLine> salesLines;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#remise}
	 **/
	public static volatile SingularAttribute<Sales, Remise> remise;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#user}
	 **/
	public static volatile SingularAttribute<Sales, AppUser> user;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#seller}
	 **/
	public static volatile SingularAttribute<Sales, AppUser> seller;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#caissier}
	 **/
	public static volatile SingularAttribute<Sales, AppUser> caissier;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#payments}
	 **/
	public static volatile SetAttribute<Sales, SalePayment> payments;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#magasin}
	 **/
	public static volatile SingularAttribute<Sales, Magasin> magasin;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#canceledSale}
	 **/
	public static volatile SingularAttribute<Sales, Sales> canceledSale;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#id}
	 **/
	public static volatile SingularAttribute<Sales, Long> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#saleDate}
	 **/
	public static volatile SingularAttribute<Sales, LocalDate> saleDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#type}
	 **/
	public static volatile SingularAttribute<Sales, String> type;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#numberTransaction}
	 **/
	public static volatile SingularAttribute<Sales, String> numberTransaction;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#discountAmount}
	 **/
	public static volatile SingularAttribute<Sales, Integer> discountAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#salesAmount}
	 **/
	public static volatile SingularAttribute<Sales, Integer> salesAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#htAmount}
	 **/
	public static volatile SingularAttribute<Sales, Integer> htAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#netAmount}
	 **/
	public static volatile SingularAttribute<Sales, Integer> netAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#taxAmount}
	 **/
	public static volatile SingularAttribute<Sales, Integer> taxAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#costAmount}
	 **/
	public static volatile SingularAttribute<Sales, Integer> costAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#amountToBePaid}
	 **/
	public static volatile SingularAttribute<Sales, Integer> amountToBePaid;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#payrollAmount}
	 **/
	public static volatile SingularAttribute<Sales, Integer> payrollAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#restToPay}
	 **/
	public static volatile SingularAttribute<Sales, Integer> restToPay;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#amountToBeTakenIntoAccount}
	 **/
	public static volatile SingularAttribute<Sales, Integer> amountToBeTakenIntoAccount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#statut}
	 **/
	public static volatile SingularAttribute<Sales, SalesStatut> statut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#createdAt}
	 **/
	public static volatile SingularAttribute<Sales, LocalDateTime> createdAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#updatedAt}
	 **/
	public static volatile SingularAttribute<Sales, LocalDateTime> updatedAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#effectiveUpdateDate}
	 **/
	public static volatile SingularAttribute<Sales, LocalDateTime> effectiveUpdateDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#toIgnore}
	 **/
	public static volatile SingularAttribute<Sales, Boolean> toIgnore;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#copy}
	 **/
	public static volatile SingularAttribute<Sales, Boolean> copy;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#imported}
	 **/
	public static volatile SingularAttribute<Sales, Boolean> imported;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#paymentStatus}
	 **/
	public static volatile SingularAttribute<Sales, PaymentStatus> paymentStatus;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#natureVente}
	 **/
	public static volatile SingularAttribute<Sales, NatureVente> natureVente;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#origineVente}
	 **/
	public static volatile SingularAttribute<Sales, OrigineVente> origineVente;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#typePrescription}
	 **/
	public static volatile SingularAttribute<Sales, TypePrescription> typePrescription;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#differe}
	 **/
	public static volatile SingularAttribute<Sales, Boolean> differe;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#categorieChiffreAffaire}
	 **/
	public static volatile SingularAttribute<Sales, CategorieChiffreAffaire> categorieChiffreAffaire;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#caisse}
	 **/
	public static volatile SingularAttribute<Sales, Poste> caisse;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#lastCaisse}
	 **/
	public static volatile SingularAttribute<Sales, Poste> lastCaisse;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#customer}
	 **/
	public static volatile SingularAttribute<Sales, Customer> customer;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#canceled}
	 **/
	public static volatile SingularAttribute<Sales, Boolean> canceled;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#cancelComment}
	 **/
	public static volatile SingularAttribute<Sales, String> cancelComment;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#cancelledBy}
	 **/
	public static volatile SingularAttribute<Sales, AppUser> cancelledBy;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#tvaEmbeded}
	 **/
	public static volatile SingularAttribute<Sales, String> tvaEmbeded;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#commentaire}
	 **/
	public static volatile SingularAttribute<Sales, String> commentaire;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#monnaie}
	 **/
	public static volatile SingularAttribute<Sales, Integer> monnaie;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Sales#cashRegister}
	 **/
	public static volatile SingularAttribute<Sales, CashRegister> cashRegister;

}

