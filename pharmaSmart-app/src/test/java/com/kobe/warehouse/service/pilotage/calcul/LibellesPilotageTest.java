package com.kobe.warehouse.service.pilotage.calcul;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Pilotage — libellés des tranches")
class LibellesPilotageTest {

    @ParameterizedTest(name = "{0} du {1} au {2} → {3}")
    @CsvSource(
        delimiter = '|',
        value = {
            "MOIS      | 2026-09-01 | 2026-09-30 | Sept. 2026",
            "MOIS      | 2026-10-01 | 2026-10-10 | Oct. 2026 (au 10)",
            "MOIS      | 2026-10-05 | 2026-10-31 | Oct. 2026 (dès le 5)",
            "MOIS      | 2026-10-05 | 2026-10-10 | Oct. 2026 (du 5 au 10)",
            "TRIMESTRE | 2026-10-01 | 2026-12-31 | T4 2026",
            "TRIMESTRE | 2026-10-01 | 2026-10-10 | T4 2026 (au 10/10)",
            "ANNEE     | 2025-01-01 | 2025-12-31 | 2025",
            "SEMAINE   | 2026-10-05 | 2026-10-11 | Sem. du 05/10",
            "SEMAINE   | 2026-10-01 | 2026-10-04 | Sem. du 28/09 (dès le 01/10)",
            "JOUR      | 2026-10-10 | 2026-10-10 | 10/10/2026",
        }
    )
    void libellerTranche(Granularite granularite, LocalDate du, LocalDate au, String libelle) {
        assertThat(LibellesPilotage.libellerTranche(new PeriodeDTO(du, au), granularite)).isEqualTo(libelle);
    }
}
