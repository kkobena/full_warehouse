package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.AjustementStatut;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Ajust}
 **/
@StaticMetamodel(Ajust.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Ajust_ {

	
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
	 * @see #statut
	 **/
	public static final String STATUT = "statut";
	
	/**
	 * @see #commentaire
	 **/
	public static final String COMMENTAIRE = "commentaire";
	
	/**
	 * @see #ajustements
	 **/
	public static final String AJUSTEMENTS = "ajustements";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Ajust}
	 **/
	public static volatile EntityType<Ajust> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ajust#id}
	 **/
	public static volatile SingularAttribute<Ajust, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ajust#dateMtv}
	 **/
	public static volatile SingularAttribute<Ajust, LocalDateTime> dateMtv;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ajust#user}
	 **/
	public static volatile SingularAttribute<Ajust, AppUser> user;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ajust#statut}
	 **/
	public static volatile SingularAttribute<Ajust, AjustementStatut> statut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ajust#commentaire}
	 **/
	public static volatile SingularAttribute<Ajust, String> commentaire;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ajust#ajustements}
	 **/
	public static volatile ListAttribute<Ajust, Ajustement> ajustements;

}

