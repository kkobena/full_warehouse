package com.kobe.warehouse.domain.nav;

import com.kobe.warehouse.domain.enumeration.NavBadgeType;
import com.kobe.warehouse.domain.enumeration.NavTargetType;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.nav.NavItem}
 **/
@StaticMetamodel(NavItem.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class NavItem_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #code
	 **/
	public static final String CODE = "code";
	
	/**
	 * @see #libelle
	 **/
	public static final String LIBELLE = "libelle";
	
	/**
	 * @see #icon
	 **/
	public static final String ICON = "icon";
	
	/**
	 * @see #routerLink
	 **/
	public static final String ROUTER_LINK = "routerLink";
	
	/**
	 * @see #parent
	 **/
	public static final String PARENT = "parent";
	
	/**
	 * @see #children
	 **/
	public static final String CHILDREN = "children";
	
	/**
	 * @see #ordre
	 **/
	public static final String ORDRE = "ordre";
	
	/**
	 * @see #niveau
	 **/
	public static final String NIVEAU = "niveau";
	
	/**
	 * @see #badgeType
	 **/
	public static final String BADGE_TYPE = "badgeType";
	
	/**
	 * @see #targetType
	 **/
	public static final String TARGET_TYPE = "targetType";
	
	/**
	 * @see #actif
	 **/
	public static final String ACTIF = "actif";
	
	/**
	 * @see #created
	 **/
	public static final String CREATED = "created";
	
	/**
	 * @see #updated
	 **/
	public static final String UPDATED = "updated";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.nav.NavItem}
	 **/
	public static volatile EntityType<NavItem> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItem#id}
	 **/
	public static volatile SingularAttribute<NavItem, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItem#code}
	 **/
	public static volatile SingularAttribute<NavItem, String> code;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItem#libelle}
	 **/
	public static volatile SingularAttribute<NavItem, String> libelle;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItem#icon}
	 **/
	public static volatile SingularAttribute<NavItem, String> icon;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItem#routerLink}
	 **/
	public static volatile SingularAttribute<NavItem, String> routerLink;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItem#parent}
	 **/
	public static volatile SingularAttribute<NavItem, NavItem> parent;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItem#children}
	 **/
	public static volatile ListAttribute<NavItem, NavItem> children;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItem#ordre}
	 **/
	public static volatile SingularAttribute<NavItem, Integer> ordre;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItem#niveau}
	 **/
	public static volatile SingularAttribute<NavItem, Integer> niveau;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItem#badgeType}
	 **/
	public static volatile SingularAttribute<NavItem, NavBadgeType> badgeType;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItem#targetType}
	 **/
	public static volatile SingularAttribute<NavItem, NavTargetType> targetType;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItem#actif}
	 **/
	public static volatile SingularAttribute<NavItem, Boolean> actif;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItem#created}
	 **/
	public static volatile SingularAttribute<NavItem, LocalDateTime> created;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItem#updated}
	 **/
	public static volatile SingularAttribute<NavItem, LocalDateTime> updated;

}

