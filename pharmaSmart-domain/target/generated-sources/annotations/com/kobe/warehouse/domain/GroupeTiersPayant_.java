package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.OrdreTrisFacture;
import com.kobe.warehouse.domain.enumeration.Periodicite;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.GroupeTiersPayant}
 **/
@StaticMetamodel(GroupeTiersPayant.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class GroupeTiersPayant_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #name
	 **/
	public static final String NAME = "name";
	
	/**
	 * @see #adresse
	 **/
	public static final String ADRESSE = "adresse";
	
	/**
	 * @see #telephone
	 **/
	public static final String TELEPHONE = "telephone";
	
	/**
	 * @see #telephoneFixe
	 **/
	public static final String TELEPHONE_FIXE = "telephoneFixe";
	
	/**
	 * @see #ordreTrisFacture
	 **/
	public static final String ORDRE_TRIS_FACTURE = "ordreTrisFacture";
	
	/**
	 * @see #email
	 **/
	public static final String EMAIL = "email";
	
	/**
	 * @see #delaiReglement
	 **/
	public static final String DELAI_REGLEMENT = "delaiReglement";
	
	/**
	 * @see #periodiciteFactureDefinitive
	 **/
	public static final String PERIODICITE_FACTURE_DEFINITIVE = "periodiciteFactureDefinitive";
	
	/**
	 * @see #periodiciteFactureProvisoire
	 **/
	public static final String PERIODICITE_FACTURE_PROVISOIRE = "periodiciteFactureProvisoire";
	
	/**
	 * @see #inclureFacturationAutoDefinitive
	 **/
	public static final String INCLURE_FACTURATION_AUTO_DEFINITIVE = "inclureFacturationAutoDefinitive";
	
	/**
	 * @see #inclureFacturationAutoProvisoire
	 **/
	public static final String INCLURE_FACTURATION_AUTO_PROVISOIRE = "inclureFacturationAutoProvisoire";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.GroupeTiersPayant}
	 **/
	public static volatile EntityType<GroupeTiersPayant> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeTiersPayant#id}
	 **/
	public static volatile SingularAttribute<GroupeTiersPayant, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeTiersPayant#name}
	 **/
	public static volatile SingularAttribute<GroupeTiersPayant, String> name;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeTiersPayant#adresse}
	 **/
	public static volatile SingularAttribute<GroupeTiersPayant, String> adresse;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeTiersPayant#telephone}
	 **/
	public static volatile SingularAttribute<GroupeTiersPayant, String> telephone;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeTiersPayant#telephoneFixe}
	 **/
	public static volatile SingularAttribute<GroupeTiersPayant, String> telephoneFixe;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeTiersPayant#ordreTrisFacture}
	 **/
	public static volatile SingularAttribute<GroupeTiersPayant, OrdreTrisFacture> ordreTrisFacture;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeTiersPayant#email}
	 **/
	public static volatile SingularAttribute<GroupeTiersPayant, String> email;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeTiersPayant#delaiReglement}
	 **/
	public static volatile SingularAttribute<GroupeTiersPayant, Integer> delaiReglement;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeTiersPayant#periodiciteFactureDefinitive}
	 **/
	public static volatile SingularAttribute<GroupeTiersPayant, Periodicite> periodiciteFactureDefinitive;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeTiersPayant#periodiciteFactureProvisoire}
	 **/
	public static volatile SingularAttribute<GroupeTiersPayant, Periodicite> periodiciteFactureProvisoire;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeTiersPayant#inclureFacturationAutoDefinitive}
	 **/
	public static volatile SingularAttribute<GroupeTiersPayant, Boolean> inclureFacturationAutoDefinitive;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeTiersPayant#inclureFacturationAutoProvisoire}
	 **/
	public static volatile SingularAttribute<GroupeTiersPayant, Boolean> inclureFacturationAutoProvisoire;

}

