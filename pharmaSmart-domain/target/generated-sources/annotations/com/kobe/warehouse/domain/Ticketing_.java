package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Ticketing}
 **/
@StaticMetamodel(Ticketing.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Ticketing_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #numberOf10Thousand
	 **/
	public static final String NUMBER_OF10_THOUSAND = "numberOf10Thousand";
	
	/**
	 * @see #numberOf5Thousand
	 **/
	public static final String NUMBER_OF5_THOUSAND = "numberOf5Thousand";
	
	/**
	 * @see #numberOf2Thousand
	 **/
	public static final String NUMBER_OF2_THOUSAND = "numberOf2Thousand";
	
	/**
	 * @see #numberOf1Thousand
	 **/
	public static final String NUMBER_OF1_THOUSAND = "numberOf1Thousand";
	
	/**
	 * @see #numberOf500Hundred
	 **/
	public static final String NUMBER_OF500_HUNDRED = "numberOf500Hundred";
	
	/**
	 * @see #numberOf200Hundred
	 **/
	public static final String NUMBER_OF200_HUNDRED = "numberOf200Hundred";
	
	/**
	 * @see #numberOf100Hundred
	 **/
	public static final String NUMBER_OF100_HUNDRED = "numberOf100Hundred";
	
	/**
	 * @see #numberOf50
	 **/
	public static final String NUMBER_OF50 = "numberOf50";
	
	/**
	 * @see #numberOf25
	 **/
	public static final String NUMBER_OF25 = "numberOf25";
	
	/**
	 * @see #numberOf10
	 **/
	public static final String NUMBER_OF10 = "numberOf10";
	
	/**
	 * @see #numberOf5
	 **/
	public static final String NUMBER_OF5 = "numberOf5";
	
	/**
	 * @see #numberOf1
	 **/
	public static final String NUMBER_OF1 = "numberOf1";
	
	/**
	 * @see #otherAmount
	 **/
	public static final String OTHER_AMOUNT = "otherAmount";
	
	/**
	 * @see #totalAmount
	 **/
	public static final String TOTAL_AMOUNT = "totalAmount";
	
	/**
	 * @see #created
	 **/
	public static final String CREATED = "created";
	
	/**
	 * @see #cashRegister
	 **/
	public static final String CASH_REGISTER = "cashRegister";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Ticketing}
	 **/
	public static volatile EntityType<Ticketing> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ticketing#id}
	 **/
	public static volatile SingularAttribute<Ticketing, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ticketing#numberOf10Thousand}
	 **/
	public static volatile SingularAttribute<Ticketing, Integer> numberOf10Thousand;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ticketing#numberOf5Thousand}
	 **/
	public static volatile SingularAttribute<Ticketing, Integer> numberOf5Thousand;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ticketing#numberOf2Thousand}
	 **/
	public static volatile SingularAttribute<Ticketing, Integer> numberOf2Thousand;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ticketing#numberOf1Thousand}
	 **/
	public static volatile SingularAttribute<Ticketing, Integer> numberOf1Thousand;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ticketing#numberOf500Hundred}
	 **/
	public static volatile SingularAttribute<Ticketing, Integer> numberOf500Hundred;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ticketing#numberOf200Hundred}
	 **/
	public static volatile SingularAttribute<Ticketing, Integer> numberOf200Hundred;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ticketing#numberOf100Hundred}
	 **/
	public static volatile SingularAttribute<Ticketing, Integer> numberOf100Hundred;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ticketing#numberOf50}
	 **/
	public static volatile SingularAttribute<Ticketing, Integer> numberOf50;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ticketing#numberOf25}
	 **/
	public static volatile SingularAttribute<Ticketing, Integer> numberOf25;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ticketing#numberOf10}
	 **/
	public static volatile SingularAttribute<Ticketing, Integer> numberOf10;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ticketing#numberOf5}
	 **/
	public static volatile SingularAttribute<Ticketing, Integer> numberOf5;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ticketing#numberOf1}
	 **/
	public static volatile SingularAttribute<Ticketing, Integer> numberOf1;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ticketing#otherAmount}
	 **/
	public static volatile SingularAttribute<Ticketing, Integer> otherAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ticketing#totalAmount}
	 **/
	public static volatile SingularAttribute<Ticketing, Long> totalAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ticketing#created}
	 **/
	public static volatile SingularAttribute<Ticketing, LocalDateTime> created;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ticketing#cashRegister}
	 **/
	public static volatile SingularAttribute<Ticketing, CashRegister> cashRegister;

}

