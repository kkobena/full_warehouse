package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.CashRegister}
 **/
@StaticMetamodel(CashRegister.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class CashRegister_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #user
	 **/
	public static final String USER = "user";
	
	/**
	 * @see #initAmount
	 **/
	public static final String INIT_AMOUNT = "initAmount";
	
	/**
	 * @see #finalAmount
	 **/
	public static final String FINAL_AMOUNT = "finalAmount";
	
	/**
	 * @see #canceledAmount
	 **/
	public static final String CANCELED_AMOUNT = "canceledAmount";
	
	/**
	 * @see #beginTime
	 **/
	public static final String BEGIN_TIME = "beginTime";
	
	/**
	 * @see #endTime
	 **/
	public static final String END_TIME = "endTime";
	
	/**
	 * @see #created
	 **/
	public static final String CREATED = "created";
	
	/**
	 * @see #updated
	 **/
	public static final String UPDATED = "updated";
	
	/**
	 * @see #statut
	 **/
	public static final String STATUT = "statut";
	
	/**
	 * @see #cashFund
	 **/
	public static final String CASH_FUND = "cashFund";
	
	/**
	 * @see #ticketing
	 **/
	public static final String TICKETING = "ticketing";
	
	/**
	 * @see #updatedUser
	 **/
	public static final String UPDATED_USER = "updatedUser";
	
	/**
	 * @see #cashRegisterItems
	 **/
	public static final String CASH_REGISTER_ITEMS = "cashRegisterItems";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.CashRegister}
	 **/
	public static volatile EntityType<CashRegister> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashRegister#id}
	 **/
	public static volatile SingularAttribute<CashRegister, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashRegister#user}
	 **/
	public static volatile SingularAttribute<CashRegister, AppUser> user;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashRegister#initAmount}
	 **/
	public static volatile SingularAttribute<CashRegister, Long> initAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashRegister#finalAmount}
	 **/
	public static volatile SingularAttribute<CashRegister, Long> finalAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashRegister#canceledAmount}
	 **/
	public static volatile SingularAttribute<CashRegister, Integer> canceledAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashRegister#beginTime}
	 **/
	public static volatile SingularAttribute<CashRegister, LocalDateTime> beginTime;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashRegister#endTime}
	 **/
	public static volatile SingularAttribute<CashRegister, LocalDateTime> endTime;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashRegister#created}
	 **/
	public static volatile SingularAttribute<CashRegister, LocalDateTime> created;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashRegister#updated}
	 **/
	public static volatile SingularAttribute<CashRegister, LocalDateTime> updated;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashRegister#statut}
	 **/
	public static volatile SingularAttribute<CashRegister, CashRegisterStatut> statut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashRegister#cashFund}
	 **/
	public static volatile SingularAttribute<CashRegister, CashFund> cashFund;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashRegister#ticketing}
	 **/
	public static volatile SingularAttribute<CashRegister, Ticketing> ticketing;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashRegister#updatedUser}
	 **/
	public static volatile SingularAttribute<CashRegister, AppUser> updatedUser;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashRegister#cashRegisterItems}
	 **/
	public static volatile ListAttribute<CashRegister, CashRegisterItem> cashRegisterItems;

}

