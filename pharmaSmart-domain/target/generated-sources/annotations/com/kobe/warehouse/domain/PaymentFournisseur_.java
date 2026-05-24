package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.PaymentFournisseur}
 **/
@StaticMetamodel(PaymentFournisseur.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class PaymentFournisseur_ extends PaymentTransaction_ {

	
	/**
	 * @see #commande
	 **/
	public static final String COMMANDE = "commande";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.PaymentFournisseur}
	 **/
	public static volatile EntityType<PaymentFournisseur> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PaymentFournisseur#commande}
	 **/
	public static volatile SingularAttribute<PaymentFournisseur, Commande> commande;

}

