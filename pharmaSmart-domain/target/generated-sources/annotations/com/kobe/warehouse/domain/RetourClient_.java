package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ModeReglementRetour;
import com.kobe.warehouse.domain.enumeration.MotifRetourClient;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.RetourClient}
 **/
@StaticMetamodel(RetourClient.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class RetourClient_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #reference
	 **/
	public static final String REFERENCE = "reference";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";
	
	/**
	 * @see #validatedAt
	 **/
	public static final String VALIDATED_AT = "validatedAt";
	
	/**
	 * @see #motif
	 **/
	public static final String MOTIF = "motif";
	
	/**
	 * @see #modeReglement
	 **/
	public static final String MODE_REGLEMENT = "modeReglement";
	
	/**
	 * @see #commentaire
	 **/
	public static final String COMMENTAIRE = "commentaire";
	
	/**
	 * @see #montantTotal
	 **/
	public static final String MONTANT_TOTAL = "montantTotal";
	
	/**
	 * @see #originalSaleId
	 **/
	public static final String ORIGINAL_SALE_ID = "originalSaleId";
	
	/**
	 * @see #originalSaleDate
	 **/
	public static final String ORIGINAL_SALE_DATE = "originalSaleDate";
	
	/**
	 * @see #originalSaleRef
	 **/
	public static final String ORIGINAL_SALE_REF = "originalSaleRef";
	
	/**
	 * @see #echangeSaleRef
	 **/
	public static final String ECHANGE_SALE_REF = "echangeSaleRef";
	
	/**
	 * @see #avecEchange
	 **/
	public static final String AVEC_ECHANGE = "avecEchange";
	
	/**
	 * @see #customer
	 **/
	public static final String CUSTOMER = "customer";
	
	/**
	 * @see #createdBy
	 **/
	public static final String CREATED_BY = "createdBy";
	
	/**
	 * @see #validatedBy
	 **/
	public static final String VALIDATED_BY = "validatedBy";
	
	/**
	 * @see #lines
	 **/
	public static final String LINES = "lines";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.RetourClient}
	 **/
	public static volatile EntityType<RetourClient> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClient#id}
	 **/
	public static volatile SingularAttribute<RetourClient, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClient#reference}
	 **/
	public static volatile SingularAttribute<RetourClient, String> reference;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClient#createdAt}
	 **/
	public static volatile SingularAttribute<RetourClient, LocalDateTime> createdAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClient#validatedAt}
	 **/
	public static volatile SingularAttribute<RetourClient, LocalDateTime> validatedAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClient#motif}
	 **/
	public static volatile SingularAttribute<RetourClient, MotifRetourClient> motif;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClient#modeReglement}
	 **/
	public static volatile SingularAttribute<RetourClient, ModeReglementRetour> modeReglement;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClient#commentaire}
	 **/
	public static volatile SingularAttribute<RetourClient, String> commentaire;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClient#montantTotal}
	 **/
	public static volatile SingularAttribute<RetourClient, Integer> montantTotal;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClient#originalSaleId}
	 **/
	public static volatile SingularAttribute<RetourClient, Long> originalSaleId;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClient#originalSaleDate}
	 **/
	public static volatile SingularAttribute<RetourClient, LocalDate> originalSaleDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClient#originalSaleRef}
	 **/
	public static volatile SingularAttribute<RetourClient, String> originalSaleRef;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClient#echangeSaleRef}
	 **/
	public static volatile SingularAttribute<RetourClient, String> echangeSaleRef;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClient#avecEchange}
	 **/
	public static volatile SingularAttribute<RetourClient, Boolean> avecEchange;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClient#customer}
	 **/
	public static volatile SingularAttribute<RetourClient, Customer> customer;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClient#createdBy}
	 **/
	public static volatile SingularAttribute<RetourClient, AppUser> createdBy;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClient#validatedBy}
	 **/
	public static volatile SingularAttribute<RetourClient, AppUser> validatedBy;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClient#lines}
	 **/
	public static volatile ListAttribute<RetourClient, RetourClientLine> lines;

}

