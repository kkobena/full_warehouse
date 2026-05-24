package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.PaymentTransaction}
 **/
@StaticMetamodel(PaymentTransaction.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class PaymentTransaction_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #transactionDate
	 **/
	public static final String TRANSACTION_DATE = "transactionDate";
	
	/**
	 * @see #expectedAmount
	 **/
	public static final String EXPECTED_AMOUNT = "expectedAmount";
	
	/**
	 * @see #paidAmount
	 **/
	public static final String PAID_AMOUNT = "paidAmount";
	
	/**
	 * @see #reelAmount
	 **/
	public static final String REEL_AMOUNT = "reelAmount";
	
	/**
	 * @see #montantVerse
	 **/
	public static final String MONTANT_VERSE = "montantVerse";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";
	
	/**
	 * @see #paymentMode
	 **/
	public static final String PAYMENT_MODE = "paymentMode";
	
	/**
	 * @see #cashRegister
	 **/
	public static final String CASH_REGISTER = "cashRegister";
	
	/**
	 * @see #categorieChiffreAffaire
	 **/
	public static final String CATEGORIE_CHIFFRE_AFFAIRE = "categorieChiffreAffaire";
	
	/**
	 * @see #credit
	 **/
	public static final String CREDIT = "credit";
	
	/**
	 * @see #typeFinancialTransaction
	 **/
	public static final String TYPE_FINANCIAL_TRANSACTION = "typeFinancialTransaction";
	
	/**
	 * @see #commentaire
	 **/
	public static final String COMMENTAIRE = "commentaire";
	
	/**
	 * @see #transactionNumber
	 **/
	public static final String TRANSACTION_NUMBER = "transactionNumber";
	
	/**
	 * @see #type
	 **/
	public static final String TYPE = "type";
	
	/**
	 * @see #banque
	 **/
	public static final String BANQUE = "banque";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.PaymentTransaction}
	 **/
	public static volatile EntityType<PaymentTransaction> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PaymentTransaction#id}
	 **/
	public static volatile SingularAttribute<PaymentTransaction, Long> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PaymentTransaction#transactionDate}
	 **/
	public static volatile SingularAttribute<PaymentTransaction, LocalDate> transactionDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PaymentTransaction#expectedAmount}
	 **/
	public static volatile SingularAttribute<PaymentTransaction, Integer> expectedAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PaymentTransaction#paidAmount}
	 **/
	public static volatile SingularAttribute<PaymentTransaction, Integer> paidAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PaymentTransaction#reelAmount}
	 **/
	public static volatile SingularAttribute<PaymentTransaction, Integer> reelAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PaymentTransaction#montantVerse}
	 **/
	public static volatile SingularAttribute<PaymentTransaction, Integer> montantVerse;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PaymentTransaction#createdAt}
	 **/
	public static volatile SingularAttribute<PaymentTransaction, LocalDateTime> createdAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PaymentTransaction#paymentMode}
	 **/
	public static volatile SingularAttribute<PaymentTransaction, PaymentMode> paymentMode;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PaymentTransaction#cashRegister}
	 **/
	public static volatile SingularAttribute<PaymentTransaction, CashRegister> cashRegister;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PaymentTransaction#categorieChiffreAffaire}
	 **/
	public static volatile SingularAttribute<PaymentTransaction, CategorieChiffreAffaire> categorieChiffreAffaire;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PaymentTransaction#credit}
	 **/
	public static volatile SingularAttribute<PaymentTransaction, Boolean> credit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PaymentTransaction#typeFinancialTransaction}
	 **/
	public static volatile SingularAttribute<PaymentTransaction, TypeFinancialTransaction> typeFinancialTransaction;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PaymentTransaction#commentaire}
	 **/
	public static volatile SingularAttribute<PaymentTransaction, String> commentaire;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PaymentTransaction#transactionNumber}
	 **/
	public static volatile SingularAttribute<PaymentTransaction, String> transactionNumber;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PaymentTransaction#type}
	 **/
	public static volatile SingularAttribute<PaymentTransaction, String> type;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PaymentTransaction#banque}
	 **/
	public static volatile SingularAttribute<PaymentTransaction, Banque> banque;

}

