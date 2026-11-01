package de.jakub.seminarverwaltung.zertifikat;

import java.time.LocalDate;

import de.jakub.seminarverwaltung.benutzer.Benutzer;
import de.jakub.seminarverwaltung.seminar.Seminar;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Zertifikat {

    @Id
    @GeneratedValue
    private Long id;

    /** Online- oder Präsenz-Seminar */
    @NotNull
    @ManyToOne
    private Seminar seminar;

    @NotNull
    @ManyToOne
    private Benutzer teilnehmer;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Niveau niveau;

    @NotNull
    private LocalDate ausstellungsdatum;

    /** leer = unbegrenzt gültig */
    private LocalDate gueltigBis;
}
