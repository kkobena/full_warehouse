package com.kobe.warehouse.domain.nav;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.nav.NavItemUserOrder}
 **/
@StaticMetamodel(NavItemUserOrder.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class NavItemUserOrder_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #userLogin
	 **/
	public static final String USER_LOGIN = "userLogin";
	
	/**
	 * @see #navItem
	 **/
	public static final String NAV_ITEM = "navItem";
	
	/**
	 * @see #ordre
	 **/
	public static final String ORDRE = "ordre";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.nav.NavItemUserOrder}
	 **/
	public static volatile EntityType<NavItemUserOrder> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItemUserOrder#id}
	 **/
	public static volatile SingularAttribute<NavItemUserOrder, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItemUserOrder#userLogin}
	 **/
	public static volatile SingularAttribute<NavItemUserOrder, String> userLogin;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItemUserOrder#navItem}
	 **/
	public static volatile SingularAttribute<NavItemUserOrder, NavItem> navItem;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItemUserOrder#ordre}
	 **/
	public static volatile SingularAttribute<NavItemUserOrder, Integer> ordre;

}

