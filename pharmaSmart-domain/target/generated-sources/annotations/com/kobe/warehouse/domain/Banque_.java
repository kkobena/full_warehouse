package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Banque}
 **/
@StaticMetamodel(Banque.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Banque_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #nom
	 **/
	public static final String NOM = "nom";
	
	/**
	 * @see #code
	 **/
	public static final String CODE = "code";
	
	/**
	 * @see #adresse
	 **/
	public static final String ADRESSE = "adresse";
	
	/**
	 * @see #beneficiaire
	 **/
	public static final String BENEFICIAIRE = "beneficiaire";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Banque}
	 **/
	public static volatile EntityType<Banque> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Banque#id}
	 **/
	public static volatile SingularAttribute<Banque, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Banque#nom}
	 **/
	public static volatile SingularAttribute<Banque, String> nom;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Banque#code}
	 **/
	public static volatile SingularAttribute<Banque, String> code;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Banque#adresse}
	 **/
	public static volatile SingularAttribute<Banque, String> adresse;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Banque#beneficiaire}
	 **/
	public static volatile SingularAttribute<Banque, String> beneficiaire;

}

