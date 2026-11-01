package de.jakub.seminarverwaltung.seminar;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class PraesenzSeminar extends Seminar {

    /** Stadtname, wird auch für die Wetterabfrage genutzt. */
    @NotBlank
    private String ort;

    private String raum;

    @Override
    public String getTyp() {
        return "PRAESENZ";
    }
}
