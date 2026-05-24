package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.AvoirFournisseurStatut;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.AvoirFournisseur}
 **/
@StaticMetamodel(AvoirFournisseur.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class AvoirFournisseur_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #reference
	 **/
	public static final String REFERENCE = "reference";
	
	/**
	 * @see #dateMtv
	 **/
	public static final String DATE_MTV = "dateMtv";
	
	/**
	 * @see #montant
	 **/
	public static final String MONTANT = "montant";
	
	/**
	 * @see #statut
	 **/
	public static final String STATUT = "statut";
	
	/**
	 * @see #commentaire
	 **/
	public static final String COMMENTAIRE = "commentaire";
	
	/**
	 * @see #user
	 **/
	public static final String USER = "user";
	
	/**
	 * @see #retourBon
	 **/
	public static final String RETOUR_BON = "retourBon";
	
	/**
	 * @see #fournisseur
	 **/
	public static final String FOURNISSEUR = "fournisseur";
	
	/**
	 * @see #lignes
	 **/
	public static final String LIGNES = "lignes";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.AvoirFournisseur}
	 **/
	public static volatile EntityType<AvoirFournisseur> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirFournisseur#id}
	 **/
	public static volatile SingularAttribute<AvoirFournisseur, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirFournisseur#reference}
	 **/
	public static volatile SingularAttribute<AvoirFournisseur, String> reference;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirFournisseur#dateMtv}
	 **/
	public static volatile SingularAttribute<AvoirFournisseur, LocalDateTime> dateMtv;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirFournisseur#montant}
	 **/
	public static volatile SingularAttribute<AvoirFournisseur, Long> montant;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirFournisseur#statut}
	 **/
	public static volatile SingularAttribute<AvoirFournisseur, AvoirFournisseurStatut> statut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirFournisseur#commentaire}
	 **/
	public static volatile SingularAttribute<AvoirFournisseur, String> commentaire;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirFournisseur#user}
	 **/
	public static volatile SingularAttribute<AvoirFournisseur, AppUser> user;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirFournisseur#retourBon}
	 **/
	public static volatile SingularAttribute<AvoirFournisseur, RetourBon> retourBon;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirFournisseur#fournisseur}
	 **/
	public static volatile SingularAttribute<AvoirFournisseur, Fournisseur> fournisseur;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirFournisseur#lignes}
	 **/
	public static volatile ListAttribute<AvoirFournisseur, AvoirFournisseurLine> lignes;

}

