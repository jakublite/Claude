package de.jakub.seminarverwaltung.unterlage;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.jakub.seminarverwaltung.benutzer.Benutzer;
import de.jakub.seminarverwaltung.seminar.Seminar;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Schulungsunterlage {

    @Id
    @GeneratedValue
    private Long id;

    @NotBlank
    private String titel;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Art art;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Bezugsart bezugsart;

    /** Pflicht bei Bezugsart LEIHEN */
    private LocalDate verfuegbarBis;

    @NotNull
    @PositiveOrZero
    private BigDecimal gebuehr;

    private boolean bezahlt;

    @ManyToOne
    private Seminar seminar;

    /** Wer die Unterlage geliehen oder gekauft hat */
    @ManyToOne
    private Benutzer benutzer;

    /** Geliehen und das Rückgabedatum ist überschritten (in einer Oberfläche z. B. rot markieren). */
    public boolean isUeberfaellig() {
        return bezugsart == Bezugsart.LEIHEN && verfuegbarBis != null && verfuegbarBis.isBefore(LocalDate.now());
    }

    @JsonIgnore
    @AssertTrue(message = "verfuegbarBis ist beim Leihen Pflicht")
    public boolean isVerfuegbarBisGesetztBeiLeihe() {
        return bezugsart != Bezugsart.LEIHEN || verfuegbarBis != null;
    }
}
