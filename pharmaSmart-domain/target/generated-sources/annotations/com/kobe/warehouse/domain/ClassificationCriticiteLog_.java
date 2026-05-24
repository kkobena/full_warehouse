package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.domain.enumeration.ClassificationType;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.ClassificationCriticiteLog}
 **/
@StaticMetamodel(ClassificationCriticiteLog.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class ClassificationCriticiteLog_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #produit
	 **/
	public static final String PRODUIT = "produit";
	
	/**
	 * @see #ancienneClasse
	 **/
	public static final String ANCIENNE_CLASSE = "ancienneClasse";
	
	/**
	 * @see #nouvelleClasse
	 **/
	public static final String NOUVELLE_CLASSE = "nouvelleClasse";
	
	/**
	 * @see #vmm12Mois
	 **/
	public static final String VMM12_MOIS = "vmm12Mois";
	
	/**
	 * @see #ca12Mois
	 **/
	public static final String CA12_MOIS = "ca12Mois";
	
	/**
	 * @see #rotationAnnuelle
	 **/
	public static final String ROTATION_ANNUELLE = "rotationAnnuelle";
	
	/**
	 * @see #frequenceVenteMois
	 **/
	public static final String FREQUENCE_VENTE_MOIS = "frequenceVenteMois";
	
	/**
	 * @see #scoreTotal
	 **/
	public static final String SCORE_TOTAL = "scoreTotal";
	
	/**
	 * @see #raisonChangement
	 **/
	public static final String RAISON_CHANGEMENT = "raisonChangement";
	
	/**
	 * @see #classificationType
	 **/
	public static final String CLASSIFICATION_TYPE = "classificationType";
	
	/**
	 * @see #user
	 **/
	public static final String USER = "user";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.ClassificationCriticiteLog}
	 **/
	public static volatile EntityType<ClassificationCriticiteLog> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationCriticiteLog#id}
	 **/
	public static volatile SingularAttribute<ClassificationCriticiteLog, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationCriticiteLog#produit}
	 **/
	public static volatile SingularAttribute<ClassificationCriticiteLog, Produit> produit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationCriticiteLog#ancienneClasse}
	 **/
	public static volatile SingularAttribute<ClassificationCriticiteLog, ClasseCriticite> ancienneClasse;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationCriticiteLog#nouvelleClasse}
	 **/
	public static volatile SingularAttribute<ClassificationCriticiteLog, ClasseCriticite> nouvelleClasse;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationCriticiteLog#vmm12Mois}
	 **/
	public static volatile SingularAttribute<ClassificationCriticiteLog, Integer> vmm12Mois;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationCriticiteLog#ca12Mois}
	 **/
	public static volatile SingularAttribute<ClassificationCriticiteLog, Long> ca12Mois;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationCriticiteLog#rotationAnnuelle}
	 **/
	public static volatile SingularAttribute<ClassificationCriticiteLog, BigDecimal> rotationAnnuelle;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationCriticiteLog#frequenceVenteMois}
	 **/
	public static volatile SingularAttribute<ClassificationCriticiteLog, Integer> frequenceVenteMois;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationCriticiteLog#scoreTotal}
	 **/
	public static volatile SingularAttribute<ClassificationCriticiteLog, BigDecimal> scoreTotal;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationCriticiteLog#raisonChangement}
	 **/
	public static volatile SingularAttribute<ClassificationCriticiteLog, String> raisonChangement;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationCriticiteLog#classificationType}
	 **/
	public static volatile SingularAttribute<ClassificationCriticiteLog, ClassificationType> classificationType;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationCriticiteLog#user}
	 **/
	public static volatile SingularAttribute<ClassificationCriticiteLog, AppUser> user;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationCriticiteLog#createdAt}
	 **/
	public static volatile SingularAttribute<ClassificationCriticiteLog, LocalDateTime> createdAt;

}

