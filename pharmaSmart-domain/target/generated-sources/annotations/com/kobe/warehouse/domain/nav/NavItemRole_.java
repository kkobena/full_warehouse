package com.kobe.warehouse.domain.nav;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.nav.NavItemRole}
 **/
@StaticMetamodel(NavItemRole.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class NavItemRole_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #navItem
	 **/
	public static final String NAV_ITEM = "navItem";
	
	/**
	 * @see #roleName
	 **/
	public static final String ROLE_NAME = "roleName";
	
	/**
	 * @see #canDisplay
	 **/
	public static final String CAN_DISPLAY = "canDisplay";
	
	/**
	 * @see #canAccess
	 **/
	public static final String CAN_ACCESS = "canAccess";
	
	/**
	 * @see #canCreate
	 **/
	public static final String CAN_CREATE = "canCreate";
	
	/**
	 * @see #canEdit
	 **/
	public static final String CAN_EDIT = "canEdit";
	
	/**
	 * @see #canDelete
	 **/
	public static final String CAN_DELETE = "canDelete";
	
	/**
	 * @see #canExport
	 **/
	public static final String CAN_EXPORT = "canExport";
	
	/**
	 * @see #canExecute
	 **/
	public static final String CAN_EXECUTE = "canExecute";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.nav.NavItemRole}
	 **/
	public static volatile EntityType<NavItemRole> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItemRole#id}
	 **/
	public static volatile SingularAttribute<NavItemRole, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItemRole#navItem}
	 **/
	public static volatile SingularAttribute<NavItemRole, NavItem> navItem;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItemRole#roleName}
	 **/
	public static volatile SingularAttribute<NavItemRole, String> roleName;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItemRole#canDisplay}
	 **/
	public static volatile SingularAttribute<NavItemRole, Boolean> canDisplay;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItemRole#canAccess}
	 **/
	public static volatile SingularAttribute<NavItemRole, Boolean> canAccess;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItemRole#canCreate}
	 **/
	public static volatile SingularAttribute<NavItemRole, Boolean> canCreate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItemRole#canEdit}
	 **/
	public static volatile SingularAttribute<NavItemRole, Boolean> canEdit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItemRole#canDelete}
	 **/
	public static volatile SingularAttribute<NavItemRole, Boolean> canDelete;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItemRole#canExport}
	 **/
	public static volatile SingularAttribute<NavItemRole, Boolean> canExport;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.nav.NavItemRole#canExecute}
	 **/
	public static volatile SingularAttribute<NavItemRole, Boolean> canExecute;

}

