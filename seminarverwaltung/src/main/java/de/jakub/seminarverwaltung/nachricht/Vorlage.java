package de.jakub.seminarverwaltung.nachricht;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Bearbeiteter E-Mail-Text. Solange es keinen Datensatz gibt, gilt der Standardtext aus {@link Anlass}. */
@Entity
@Getter
@Setter
public class Vorlage {

    @Id
    @Enumerated(EnumType.STRING)
    private Anlass anlass;

    @NotBlank
    @Size(max = 200)
    private String betreff;

    @NotBlank
    @Size(max = 2000)
    @Column(length = 2000)
    private String inhalt;
}
