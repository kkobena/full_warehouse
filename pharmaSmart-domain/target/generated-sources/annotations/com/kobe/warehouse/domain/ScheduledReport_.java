package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ScheduledReportFrequency;
import com.kobe.warehouse.domain.enumeration.ScheduledReportType;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.ScheduledReport}
 **/
@StaticMetamodel(ScheduledReport.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class ScheduledReport_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #reportName
	 **/
	public static final String REPORT_NAME = "reportName";
	
	/**
	 * @see #reportType
	 **/
	public static final String REPORT_TYPE = "reportType";
	
	/**
	 * @see #frequency
	 **/
	public static final String FREQUENCY = "frequency";
	
	/**
	 * @see #executionTime
	 **/
	public static final String EXECUTION_TIME = "executionTime";
	
	/**
	 * @see #dayOfWeek
	 **/
	public static final String DAY_OF_WEEK = "dayOfWeek";
	
	/**
	 * @see #dayOfMonth
	 **/
	public static final String DAY_OF_MONTH = "dayOfMonth";
	
	/**
	 * @see #active
	 **/
	public static final String ACTIVE = "active";
	
	/**
	 * @see #emailRecipients
	 **/
	public static final String EMAIL_RECIPIENTS = "emailRecipients";
	
	/**
	 * @see #includePdf
	 **/
	public static final String INCLUDE_PDF = "includePdf";
	
	/**
	 * @see #includeExcel
	 **/
	public static final String INCLUDE_EXCEL = "includeExcel";
	
	/**
	 * @see #lastExecution
	 **/
	public static final String LAST_EXECUTION = "lastExecution";
	
	/**
	 * @see #nextExecution
	 **/
	public static final String NEXT_EXECUTION = "nextExecution";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";
	
	/**
	 * @see #updatedAt
	 **/
	public static final String UPDATED_AT = "updatedAt";
	
	/**
	 * @see #createdBy
	 **/
	public static final String CREATED_BY = "createdBy";
	
	/**
	 * @see #filterParams
	 **/
	public static final String FILTER_PARAMS = "filterParams";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.ScheduledReport}
	 **/
	public static volatile EntityType<ScheduledReport> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ScheduledReport#id}
	 **/
	public static volatile SingularAttribute<ScheduledReport, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ScheduledReport#reportName}
	 **/
	public static volatile SingularAttribute<ScheduledReport, String> reportName;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ScheduledReport#reportType}
	 **/
	public static volatile SingularAttribute<ScheduledReport, ScheduledReportType> reportType;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ScheduledReport#frequency}
	 **/
	public static volatile SingularAttribute<ScheduledReport, ScheduledReportFrequency> frequency;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ScheduledReport#executionTime}
	 **/
	public static volatile SingularAttribute<ScheduledReport, LocalTime> executionTime;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ScheduledReport#dayOfWeek}
	 **/
	public static volatile SingularAttribute<ScheduledReport, Integer> dayOfWeek;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ScheduledReport#dayOfMonth}
	 **/
	public static volatile SingularAttribute<ScheduledReport, Integer> dayOfMonth;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ScheduledReport#active}
	 **/
	public static volatile SingularAttribute<ScheduledReport, Boolean> active;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ScheduledReport#emailRecipients}
	 **/
	public static volatile SingularAttribute<ScheduledReport, Set> emailRecipients;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ScheduledReport#includePdf}
	 **/
	public static volatile SingularAttribute<ScheduledReport, Boolean> includePdf;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ScheduledReport#includeExcel}
	 **/
	public static volatile SingularAttribute<ScheduledReport, Boolean> includeExcel;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ScheduledReport#lastExecution}
	 **/
	public static volatile SingularAttribute<ScheduledReport, LocalDateTime> lastExecution;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ScheduledReport#nextExecution}
	 **/
	public static volatile SingularAttribute<ScheduledReport, LocalDateTime> nextExecution;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ScheduledReport#createdAt}
	 **/
	public static volatile SingularAttribute<ScheduledReport, LocalDateTime> createdAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ScheduledReport#updatedAt}
	 **/
	public static volatile SingularAttribute<ScheduledReport, LocalDateTime> updatedAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ScheduledReport#createdBy}
	 **/
	public static volatile SingularAttribute<ScheduledReport, AppUser> createdBy;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ScheduledReport#filterParams}
	 **/
	public static volatile SingularAttribute<ScheduledReport, Map> filterParams;

}

