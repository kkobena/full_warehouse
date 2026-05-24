package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.CashFundStatut;
import com.kobe.warehouse.domain.enumeration.CashFundType;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.CashFund}
 **/
@StaticMetamodel(CashFund.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class CashFund_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #amount
	 **/
	public static final String AMOUNT = "amount";
	
	/**
	 * @see #user
	 **/
	public static final String USER = "user";
	
	/**
	 * @see #created
	 **/
	public static final String CREATED = "created";
	
	/**
	 * @see #updated
	 **/
	public static final String UPDATED = "updated";
	
	/**
	 * @see #cashFundType
	 **/
	public static final String CASH_FUND_TYPE = "cashFundType";
	
	/**
	 * @see #cashRegister
	 **/
	public static final String CASH_REGISTER = "cashRegister";
	
	/**
	 * @see #statut
	 **/
	public static final String STATUT = "statut";
	
	/**
	 * @see #validatedBy
	 **/
	public static final String VALIDATED_BY = "validatedBy";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.CashFund}
	 **/
	public static volatile EntityType<CashFund> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashFund#id}
	 **/
	public static volatile SingularAttribute<CashFund, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashFund#amount}
	 **/
	public static volatile SingularAttribute<CashFund, Integer> amount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashFund#user}
	 **/
	public static volatile SingularAttribute<CashFund, AppUser> user;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashFund#created}
	 **/
	public static volatile SingularAttribute<CashFund, LocalDateTime> created;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashFund#updated}
	 **/
	public static volatile SingularAttribute<CashFund, LocalDateTime> updated;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashFund#cashFundType}
	 **/
	public static volatile SingularAttribute<CashFund, CashFundType> cashFundType;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashFund#cashRegister}
	 **/
	public static volatile SingularAttribute<CashFund, CashRegister> cashRegister;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashFund#statut}
	 **/
	public static volatile SingularAttribute<CashFund, CashFundStatut> statut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashFund#validatedBy}
	 **/
	public static volatile SingularAttribute<CashFund, AppUser> validatedBy;

}

