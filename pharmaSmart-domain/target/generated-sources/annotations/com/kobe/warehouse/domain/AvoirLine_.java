package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.AvoirLine}
 **/
@StaticMetamodel(AvoirLine.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class AvoirLine_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #avoir
	 **/
	public static final String AVOIR = "avoir";
	
	/**
	 * @see #saleLineId
	 **/
	public static final String SALE_LINE_ID = "saleLineId";
	
	/**
	 * @see #saleLineDate
	 **/
	public static final String SALE_LINE_DATE = "saleLineDate";
	
	/**
	 * @see #montantAvoir
	 **/
	public static final String MONTANT_AVOIR = "montantAvoir";
	
	/**
	 * @see #motifRejet
	 **/
	public static final String MOTIF_REJET = "motifRejet";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.AvoirLine}
	 **/
	public static volatile EntityType<AvoirLine> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirLine#id}
	 **/
	public static volatile SingularAttribute<AvoirLine, Long> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirLine#avoir}
	 **/
	public static volatile SingularAttribute<AvoirLine, AvoirTiersPayant> avoir;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirLine#saleLineId}
	 **/
	public static volatile SingularAttribute<AvoirLine, Long> saleLineId;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirLine#saleLineDate}
	 **/
	public static volatile SingularAttribute<AvoirLine, LocalDate> saleLineDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirLine#montantAvoir}
	 **/
	public static volatile SingularAttribute<AvoirLine, BigDecimal> montantAvoir;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirLine#motifRejet}
	 **/
	public static volatile SingularAttribute<AvoirLine, String> motifRejet;

}

