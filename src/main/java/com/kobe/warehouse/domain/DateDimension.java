package com.kobe.warehouse.domain;


import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.Getter;

/**
 * A DateDimension.
 */
@Getter
@Entity
@Table(name = "date_dimension")
public class DateDimension implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "date_key")
    private Integer dateKey;

    @NotNull
    @Column(name = "full_date", nullable = false)
    private LocalDate fullDate;

    @NotNull
    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @NotNull
    @Column(name = "day_num_in_month", nullable = false)
    private Integer dayNumInMonth;

    @NotNull
    @Column(name = "day_num_overall", nullable = false)
    private Integer dayNumOverall;

    @NotNull
    @Column(name = "day_name", nullable = false)
    private String dayName;

    @NotNull
    @Column(name = "day_abbrev", nullable = false)
    private String dayAbbrev;

    @NotNull
    @Column(name = "weekday_flag", nullable = false)
    private String weekdayFlag;

    @NotNull
    @Column(name = "week_num_in_year", nullable = false)
    private Integer weekNumInYear;

    @NotNull
    @Column(name = "week_num_overall", nullable = false)
    private Integer weekNumOverall;

    @NotNull
    @Column(name = "week_begin_date", nullable = false)
    private LocalDate weekBeginDate;

    @NotNull
    @Column(name = "week_begin_date_key", nullable = false)
    private Integer weekBeginDateKey;

    @NotNull
    @Column(name = "month", nullable = false)
    private Integer month;

    @NotNull
    @Column(name = "month_num_overall", nullable = false)
    private Integer monthNumOverall;

    @NotNull
    @Column(name = "month_name", nullable = false)
    private String monthName;

    @NotNull
    @Column(name = "month_abbrev", nullable = false)
    private String monthAbbrev;

    @NotNull
    @Column(name = "quarter", nullable = false)
    private Integer quarter;

    @NotNull
    @Column(name = "year", nullable = false)
    private Integer year;

    @NotNull
    @Column(name = "yearmo", nullable = false)
    private Integer yearmo;

    @NotNull
    @Column(name = "fiscal_month", nullable = false)
    private Integer fiscalMonth;

    @NotNull
    @Column(name = "fiscal_quarter", nullable = false)
    private Integer fiscalQuarter;

    @NotNull
    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    @NotNull
    @Column(name = "last_day_in_month_flag", nullable = false)
    private String lastDayInMonthFlag;

    @NotNull
    @Column(name = "same_day_year_ago_date", nullable = false)
    private LocalDate sameDayYearAgoDate;



    @OneToMany(mappedBy = "dateDimension")
    private Set<Commande> commandes = new HashSet<>();

    @OneToMany(mappedBy = "dateDimension")
    private Set<Payment> payments = new HashSet<>();


    @OneToMany(mappedBy = "dateDimension")
    private Set<Sales> sales = new HashSet<>();
    @OneToMany(mappedBy = "dateDimension")
    private List<Decondition> deconditions = new ArrayList<>();


    public void setDateKey(Integer dateKey) {
        this.dateKey = dateKey;
    }

    public DateDimension dateKey(Integer dateKey) {
        this.dateKey = dateKey;
        return this;
    }

    public void setFullDate(LocalDate fullDate) {
        this.fullDate = fullDate;
    }

    public DateDimension fullDate(LocalDate fullDate) {
        this.fullDate = fullDate;
        return this;
    }

    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public DateDimension dayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
        return this;
    }

    public void setDayNumInMonth(Integer dayNumInMonth) {
        this.dayNumInMonth = dayNumInMonth;
    }

    public DateDimension dayNumInMonth(Integer dayNumInMonth) {
        this.dayNumInMonth = dayNumInMonth;
        return this;
    }

    public void setDayNumOverall(Integer dayNumOverall) {
        this.dayNumOverall = dayNumOverall;
    }

    public DateDimension dayNumOverall(Integer dayNumOverall) {
        this.dayNumOverall = dayNumOverall;
        return this;
    }

    public void setDayName(String dayName) {
        this.dayName = dayName;
    }

    public DateDimension dayName(String dayName) {
        this.dayName = dayName;
        return this;
    }

    public void setDayAbbrev(String dayAbbrev) {
        this.dayAbbrev = dayAbbrev;
    }

    public DateDimension dayAbbrev(String dayAbbrev) {
        this.dayAbbrev = dayAbbrev;
        return this;
    }

    public void setWeekdayFlag(String weekdayFlag) {
        this.weekdayFlag = weekdayFlag;
    }

    public void setLastDayInMonthFlag(String lastDayInMonthFlag) {
        this.lastDayInMonthFlag = lastDayInMonthFlag;
    }

    public void setWeekNumInYear(Integer weekNumInYear) {
        this.weekNumInYear = weekNumInYear;
    }

    public DateDimension weekNumInYear(Integer weekNumInYear) {
        this.weekNumInYear = weekNumInYear;
        return this;
    }

    public void setWeekNumOverall(Integer weekNumOverall) {
        this.weekNumOverall = weekNumOverall;
    }

    public DateDimension weekNumOverall(Integer weekNumOverall) {
        this.weekNumOverall = weekNumOverall;
        return this;
    }

    public void setWeekBeginDate(LocalDate weekBeginDate) {
        this.weekBeginDate = weekBeginDate;
    }

    public DateDimension weekBeginDate(LocalDate weekBeginDate) {
        this.weekBeginDate = weekBeginDate;
        return this;
    }

    public void setWeekBeginDateKey(Integer weekBeginDateKey) {
        this.weekBeginDateKey = weekBeginDateKey;
    }

    public DateDimension weekBeginDateKey(Integer weekBeginDateKey) {
        this.weekBeginDateKey = weekBeginDateKey;
        return this;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public DateDimension month(Integer month) {
        this.month = month;
        return this;
    }

    public void setMonthNumOverall(Integer monthNumOverall) {
        this.monthNumOverall = monthNumOverall;
    }

    public DateDimension monthNumOverall(Integer monthNumOverall) {
        this.monthNumOverall = monthNumOverall;
        return this;
    }

    public void setMonthName(String monthName) {
        this.monthName = monthName;
    }

    public DateDimension monthName(String monthName) {
        this.monthName = monthName;
        return this;
    }

    public void setMonthAbbrev(String monthAbbrev) {
        this.monthAbbrev = monthAbbrev;
    }

    public DateDimension monthAbbrev(String monthAbbrev) {
        this.monthAbbrev = monthAbbrev;
        return this;
    }

    public void setQuarter(Integer quarter) {
        this.quarter = quarter;
    }

    public DateDimension quarter(Integer quarter) {
        this.quarter = quarter;
        return this;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public DateDimension year(Integer year) {
        this.year = year;
        return this;
    }

    public void setYearmo(Integer yearmo) {
        this.yearmo = yearmo;
    }

    public DateDimension yearmo(Integer yearmo) {
        this.yearmo = yearmo;
        return this;
    }

    public void setFiscalMonth(Integer fiscalMonth) {
        this.fiscalMonth = fiscalMonth;
    }

    public DateDimension fiscalMonth(Integer fiscalMonth) {
        this.fiscalMonth = fiscalMonth;
        return this;
    }

    public void setFiscalQuarter(Integer fiscalQuarter) {
        this.fiscalQuarter = fiscalQuarter;
    }

    public DateDimension fiscalQuarter(Integer fiscalQuarter) {
        this.fiscalQuarter = fiscalQuarter;
        return this;
    }

    public void setFiscalYear(Integer fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public DateDimension fiscalYear(Integer fiscalYear) {
        this.fiscalYear = fiscalYear;
        return this;
    }

    public void setSameDayYearAgoDate(LocalDate sameDayYearAgoDate) {
        this.sameDayYearAgoDate = sameDayYearAgoDate;
    }

    public DateDimension sameDayYearAgoDate(LocalDate sameDayYearAgoDate) {
        this.sameDayYearAgoDate = sameDayYearAgoDate;
        return this;
    }


    public void setCommandes(Set<Commande> commandes) {
        this.commandes = commandes;
    }

    public DateDimension commandes(Set<Commande> commandes) {
        this.commandes = commandes;
        return this;
    }

    public DateDimension addCommande(Commande commande) {
        commandes.add(commande);
        commande.setDateDimension(this);
        return this;
    }

    public DateDimension removeCommande(Commande commande) {
        commandes.remove(commande);
        commande.setDateDimension(null);
        return this;
    }


    public void setPayments(Set<Payment> payments) {
        this.payments = payments;
    }

    public DateDimension payments(Set<Payment> payments) {
        this.payments = payments;
        return this;
    }

    public DateDimension addPayment(Payment payment) {
        payments.add(payment);
        payment.setDateDimension(this);
        return this;
    }

    public DateDimension removePayment(Payment payment) {
        payments.remove(payment);
        payment.setDateDimension(null);
        return this;
    }


    public void setSales(Set<Sales> sales) {
        this.sales = sales;
    }

    public DateDimension sales(Set<Sales> sales) {
        this.sales = sales;
        return this;
    }

    public DateDimension addSales(Sales sales) {
        this.sales.add(sales);
        sales.setDateDimension(this);
        return this;
    }

    public DateDimension removeSales(Sales sales) {
        this.sales.remove(sales);
        sales.setDateDimension(null);
        return this;
    }

    public void setDeconditions(List<Decondition> deconditions) {
        this.deconditions = deconditions;
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DateDimension)) {
            return false;
        }
        return dateKey != null && dateKey.equals(((DateDimension) o).dateKey);
    }

    @Override
    public int hashCode() {
        return 31;
    }


    @Override
    public String toString() {
        return "DateDimension{" +

            ", dateKey=" + getDateKey() +
            ", fullDate='" + getFullDate() + "'" +
            ", dayOfWeek=" + getDayOfWeek() +
            ", dayNumInMonth=" + getDayNumInMonth() +
            ", dayNumOverall=" + getDayNumOverall() +
            ", dayName='" + getDayName() + "'" +
            ", dayAbbrev='" + getDayAbbrev() + "'" +

            ", weekNumInYear=" + getWeekNumInYear() +
            ", weekNumOverall=" + getWeekNumOverall() +
            ", weekBeginDate='" + getWeekBeginDate() + "'" +
            ", weekBeginDateKey=" + getWeekBeginDateKey() +
            ", month=" + getMonth() +
            ", monthNumOverall=" + getMonthNumOverall() +
            ", monthName='" + getMonthName() + "'" +
            ", monthAbbrev='" + getMonthAbbrev() + "'" +
            ", quarter=" + getQuarter() +
            ", year=" + getYear() +
            ", yearmo=" + getYearmo() +
            ", fiscalMonth=" + getFiscalMonth() +
            ", fiscalQuarter=" + getFiscalQuarter() +
            ", fiscalYear=" + getFiscalYear() +

            ", sameDayYearAgoDate='" + getSameDayYearAgoDate() + "'" +
            "}";
    }
}
