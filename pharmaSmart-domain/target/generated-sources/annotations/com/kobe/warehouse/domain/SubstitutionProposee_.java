package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.SubstitutionStatut;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.SubstitutionProposee}
 **/
@StaticMetamodel(SubstitutionProposee.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class SubstitutionProposee_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #commande
	 **/
	public static final String COMMANDE = "commande";
	
	/**
	 * @see #orderLine
	 **/
	public static final String ORDER_LINE = "orderLine";
	
	/**
	 * @see #fournisseur
	 **/
	public static final String FOURNISSEUR = "fournisseur";
	
	/**
	 * @see #cipPropose
	 **/
	public static final String CIP_PROPOSE = "cipPropose";
	
	/**
	 * @see #designation
	 **/
	public static final String DESIGNATION = "designation";
	
	/**
	 * @see #typeCodification
	 **/
	public static final String TYPE_CODIFICATION = "typeCodification";
	
	/**
	 * @see #quantite
	 **/
	public static final String QUANTITE = "quantite";
	
	/**
	 * @see #statut
	 **/
	public static final String STATUT = "statut";
	
	/**
	 * @see #codeReponse
	 **/
	public static final String CODE_REPONSE = "codeReponse";
	
	/**
	 * @see #additif
	 **/
	public static final String ADDITIF = "additif";
	
	/**
	 * @see #typeRemplacement
	 **/
	public static final String TYPE_REMPLACEMENT = "typeRemplacement";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.SubstitutionProposee}
	 **/
	public static volatile EntityType<SubstitutionProposee> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SubstitutionProposee#id}
	 **/
	public static volatile SingularAttribute<SubstitutionProposee, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SubstitutionProposee#commande}
	 **/
	public static volatile SingularAttribute<SubstitutionProposee, Commande> commande;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SubstitutionProposee#orderLine}
	 **/
	public static volatile SingularAttribute<SubstitutionProposee, OrderLine> orderLine;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SubstitutionProposee#fournisseur}
	 **/
	public static volatile SingularAttribute<SubstitutionProposee, Fournisseur> fournisseur;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SubstitutionProposee#cipPropose}
	 **/
	public static volatile SingularAttribute<SubstitutionProposee, String> cipPropose;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SubstitutionProposee#designation}
	 **/
	public static volatile SingularAttribute<SubstitutionProposee, String> designation;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SubstitutionProposee#typeCodification}
	 **/
	public static volatile SingularAttribute<SubstitutionProposee, String> typeCodification;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SubstitutionProposee#quantite}
	 **/
	public static volatile SingularAttribute<SubstitutionProposee, Integer> quantite;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SubstitutionProposee#statut}
	 **/
	public static volatile SingularAttribute<SubstitutionProposee, SubstitutionStatut> statut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SubstitutionProposee#codeReponse}
	 **/
	public static volatile SingularAttribute<SubstitutionProposee, String> codeReponse;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SubstitutionProposee#additif}
	 **/
	public static volatile SingularAttribute<SubstitutionProposee, String> additif;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SubstitutionProposee#typeRemplacement}
	 **/
	public static volatile SingularAttribute<SubstitutionProposee, String> typeRemplacement;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SubstitutionProposee#createdAt}
	 **/
	public static volatile SingularAttribute<SubstitutionProposee, LocalDateTime> createdAt;

}

