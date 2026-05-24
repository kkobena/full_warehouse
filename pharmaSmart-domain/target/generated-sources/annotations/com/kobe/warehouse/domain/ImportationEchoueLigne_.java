package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.ImportationEchoueLigne}
 **/
@StaticMetamodel(ImportationEchoueLigne.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class ImportationEchoueLigne_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #importationEchoue
	 **/
	public static final String IMPORTATION_ECHOUE = "importationEchoue";
	
	/**
	 * @see #quantityReceived
	 **/
	public static final String QUANTITY_RECEIVED = "quantityReceived";
	
	/**
	 * @see #ug
	 **/
	public static final String UG = "ug";
	
	/**
	 * @see #prixUn
	 **/
	public static final String PRIX_UN = "prixUn";
	
	/**
	 * @see #prixAchat
	 **/
	public static final String PRIX_ACHAT = "prixAchat";
	
	/**
	 * @see #produitCip
	 **/
	public static final String PRODUIT_CIP = "produitCip";
	
	/**
	 * @see #produitEan
	 **/
	public static final String PRODUIT_EAN = "produitEan";
	
	/**
	 * @see #codeTva
	 **/
	public static final String CODE_TVA = "codeTva";
	
	/**
	 * @see #datePeremption
	 **/
	public static final String DATE_PEREMPTION = "datePeremption";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.ImportationEchoueLigne}
	 **/
	public static volatile EntityType<ImportationEchoueLigne> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ImportationEchoueLigne#id}
	 **/
	public static volatile SingularAttribute<ImportationEchoueLigne, Long> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ImportationEchoueLigne#importationEchoue}
	 **/
	public static volatile SingularAttribute<ImportationEchoueLigne, ImportationEchoue> importationEchoue;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ImportationEchoueLigne#quantityReceived}
	 **/
	public static volatile SingularAttribute<ImportationEchoueLigne, Integer> quantityReceived;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ImportationEchoueLigne#ug}
	 **/
	public static volatile SingularAttribute<ImportationEchoueLigne, Integer> ug;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ImportationEchoueLigne#prixUn}
	 **/
	public static volatile SingularAttribute<ImportationEchoueLigne, Integer> prixUn;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ImportationEchoueLigne#prixAchat}
	 **/
	public static volatile SingularAttribute<ImportationEchoueLigne, Integer> prixAchat;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ImportationEchoueLigne#produitCip}
	 **/
	public static volatile SingularAttribute<ImportationEchoueLigne, String> produitCip;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ImportationEchoueLigne#produitEan}
	 **/
	public static volatile SingularAttribute<ImportationEchoueLigne, String> produitEan;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ImportationEchoueLigne#codeTva}
	 **/
	public static volatile SingularAttribute<ImportationEchoueLigne, Integer> codeTva;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ImportationEchoueLigne#datePeremption}
	 **/
	public static volatile SingularAttribute<ImportationEchoueLigne, LocalDate> datePeremption;

}

