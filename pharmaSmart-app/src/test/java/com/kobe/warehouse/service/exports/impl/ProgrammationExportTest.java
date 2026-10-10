package com.kobe.warehouse.service.exports.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobe.warehouse.domain.enumeration.PeriodeRelative;
import com.kobe.warehouse.domain.enumeration.ScheduledReportFrequency;
import com.kobe.warehouse.service.errors.GenericError;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Exports — programmation et périodes relatives")
class ProgrammationExportTest {

    /** Vendredi 9 octobre 2026, 10 h. */
    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 10, 9, 10, 0);

    @Test
    @DisplayName("toujours dans le futur : aujourd'hui si l'heure n'est pas passée, sinon la fois suivante")
    void prochaineExecution() {
        assertThat(ProgrammationExport.calculerProchaine(ScheduledReportFrequency.DAILY, LocalTime.of(18, 0), null, MAINTENANT)).isEqualTo(
            LocalDateTime.of(2026, 10, 9, 18, 0)
        );
        assertThat(ProgrammationExport.calculerProchaine(ScheduledReportFrequency.DAILY, LocalTime.of(7, 0), null, MAINTENANT)).isEqualTo(
            LocalDateTime.of(2026, 10, 10, 7, 0)
        );
        assertThat(ProgrammationExport.calculerProchaine(ScheduledReportFrequency.WEEKLY, LocalTime.of(7, 0), 1, MAINTENANT)).isEqualTo(
            LocalDateTime.of(2026, 10, 12, 7, 0)
        );
        assertThat(ProgrammationExport.calculerProchaine(ScheduledReportFrequency.MONTHLY, null, 1, MAINTENANT)).isEqualTo(LocalDateTime.of(2026, 11, 1, 7, 0));
        assertThat(ProgrammationExport.calculerProchaine(ScheduledReportFrequency.MONTHLY, LocalTime.of(7, 0), 20, MAINTENANT)).isEqualTo(
            LocalDateTime.of(2026, 10, 20, 7, 0)
        );
    }

    @Test
    @DisplayName("jour hors bornes ou programmation personnalisée refusés")
    void refus() {
        assertThatThrownBy(() -> ProgrammationExport.calculerProchaine(ScheduledReportFrequency.MONTHLY, null, 31, MAINTENANT)).isInstanceOf(GenericError.class);
        assertThatThrownBy(() -> ProgrammationExport.calculerProchaine(ScheduledReportFrequency.CUSTOM, null, null, MAINTENANT)).isInstanceOf(GenericError.class);
    }

    @Test
    @DisplayName("une période relative se recalcule à chaque exécution")
    void periodesRelatives() {
        LocalDate aujourdhui = MAINTENANT.toLocalDate();
        assertThat(PeriodeRelative.MOIS_PRECEDENT.calculer(aujourdhui)).containsExactly(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        assertThat(PeriodeRelative.SEMAINE_PRECEDENTE.calculer(aujourdhui)).containsExactly(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 4));
        assertThat(PeriodeRelative.TRIMESTRE_PRECEDENT.calculer(aujourdhui)).containsExactly(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30));
        assertThat(PeriodeRelative.ANNEE_PRECEDENTE.calculer(aujourdhui)).containsExactly(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        assertThat(PeriodeRelative.HIER.calculer(aujourdhui)).containsExactly(LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 8));
    }
}
