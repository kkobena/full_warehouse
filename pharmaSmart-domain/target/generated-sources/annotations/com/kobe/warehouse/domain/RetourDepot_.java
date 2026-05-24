package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.RetourDepot}
 **/
@StaticMetamodel(RetourDepot.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class RetourDepot_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #dateMtv
	 **/
	public static final String DATE_MTV = "dateMtv";
	
	/**
	 * @see #user
	 **/
	public static final String USER = "user";
	
	/**
	 * @see #retourDepotItems
	 **/
	public static final String RETOUR_DEPOT_ITEMS = "retourDepotItems";
	
	/**
	 * @see #venteDepot
	 **/
	public static final String VENTE_DEPOT = "venteDepot";
	
	/**
	 * @see #depot
	 **/
	public static final String DEPOT = "depot";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.RetourDepot}
	 **/
	public static volatile EntityType<RetourDepot> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourDepot#id}
	 **/
	public static volatile SingularAttribute<RetourDepot, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourDepot#dateMtv}
	 **/
	public static volatile SingularAttribute<RetourDepot, LocalDateTime> dateMtv;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourDepot#user}
	 **/
	public static volatile SingularAttribute<RetourDepot, AppUser> user;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourDepot#retourDepotItems}
	 **/
	public static volatile ListAttribute<RetourDepot, RetourDepotItem> retourDepotItems;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourDepot#venteDepot}
	 **/
	public static volatile SingularAttribute<RetourDepot, VenteDepot> venteDepot;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourDepot#depot}
	 **/
	public static volatile SingularAttribute<RetourDepot, Magasin> depot;

}

