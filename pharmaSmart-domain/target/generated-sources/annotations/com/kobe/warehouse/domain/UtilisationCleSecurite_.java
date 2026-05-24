package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.nav.NavItem;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.UtilisationCleSecurite}
 **/
@StaticMetamodel(UtilisationCleSecurite.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class UtilisationCleSecurite_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #cleSecuriteOwner
	 **/
	public static final String CLE_SECURITE_OWNER = "cleSecuriteOwner";
	
	/**
	 * @see #connectedUser
	 **/
	public static final String CONNECTED_USER = "connectedUser";
	
	/**
	 * @see #caisse
	 **/
	public static final String CAISSE = "caisse";
	
	/**
	 * @see #mvtDate
	 **/
	public static final String MVT_DATE = "mvtDate";
	
	/**
	 * @see #navItem
	 **/
	public static final String NAV_ITEM = "navItem";
	
	/**
	 * @see #commentaire
	 **/
	public static final String COMMENTAIRE = "commentaire";
	
	/**
	 * @see #entityId
	 **/
	public static final String ENTITY_ID = "entityId";
	
	/**
	 * @see #entityName
	 **/
	public static final String ENTITY_NAME = "entityName";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.UtilisationCleSecurite}
	 **/
	public static volatile EntityType<UtilisationCleSecurite> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.UtilisationCleSecurite#id}
	 **/
	public static volatile SingularAttribute<UtilisationCleSecurite, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.UtilisationCleSecurite#cleSecuriteOwner}
	 **/
	public static volatile SingularAttribute<UtilisationCleSecurite, AppUser> cleSecuriteOwner;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.UtilisationCleSecurite#connectedUser}
	 **/
	public static volatile SingularAttribute<UtilisationCleSecurite, AppUser> connectedUser;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.UtilisationCleSecurite#caisse}
	 **/
	public static volatile SingularAttribute<UtilisationCleSecurite, String> caisse;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.UtilisationCleSecurite#mvtDate}
	 **/
	public static volatile SingularAttribute<UtilisationCleSecurite, LocalDateTime> mvtDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.UtilisationCleSecurite#navItem}
	 **/
	public static volatile SingularAttribute<UtilisationCleSecurite, NavItem> navItem;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.UtilisationCleSecurite#commentaire}
	 **/
	public static volatile SingularAttribute<UtilisationCleSecurite, String> commentaire;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.UtilisationCleSecurite#entityId}
	 **/
	public static volatile SingularAttribute<UtilisationCleSecurite, Long> entityId;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.UtilisationCleSecurite#entityName}
	 **/
	public static volatile SingularAttribute<UtilisationCleSecurite, String> entityName;

}

