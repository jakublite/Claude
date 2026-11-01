package de.jakub.seminarverwaltung.seminar;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import de.jakub.seminarverwaltung.benutzer.Benutzer;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

/**
 * Gemeinsame Basis für Online- und Präsenz-Seminare.
 * Im JSON entscheidet das Feld "typ" (ONLINE / PRAESENZ), welche Unterklasse entsteht.
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
// EXISTING_PROPERTY: "typ" kommt beim Schreiben aus getTyp(), damit es auch in Listen (List<Seminar>) erscheint
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "typ")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OnlineSeminar.class, name = "ONLINE"),
        @JsonSubTypes.Type(value = PraesenzSeminar.class, name = "PRAESENZ")
})
@Getter
@Setter
public abstract class Seminar {

    @Id
    @GeneratedValue
    private Long id;

    @NotBlank
    private String titel;

    private String beschreibung;

    @NotNull
    private LocalDate startDatum;

    @NotNull
    private LocalDate endDatum;

    @Min(1)
    private int maxTeilnehmer;

    @PositiveOrZero
    private BigDecimal preis;

    @NotNull
    @ManyToOne
    private Benutzer kursleiter;

    // EAGER, weil Vaadin-Views außerhalb einer offenen Session auf die Teilnehmer zugreifen
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Benutzer> teilnehmer = new ArrayList<>();

    // Reihenfolge = Reihenfolge des Nachrückens. Eigenes Select, damit der Join nicht die Teilnehmerliste vervielfacht.
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "seminar_warteliste")
    @OrderColumn(name = "warteposition")
    @Fetch(FetchMode.SELECT)
    private List<Benutzer> warteliste = new ArrayList<>();

    public abstract String getTyp();

    public int getAnzahlTeilnehmer() {
        return teilnehmer.size();
    }

    public boolean isAusgebucht() {
        return getAnzahlTeilnehmer() >= maxTeilnehmer;
    }

    @JsonIgnore
    @AssertTrue(message = "endDatum darf nicht vor startDatum liegen")
    public boolean isZeitraumGueltig() {
        return startDatum == null || endDatum == null || !endDatum.isBefore(startDatum);
    }
}
