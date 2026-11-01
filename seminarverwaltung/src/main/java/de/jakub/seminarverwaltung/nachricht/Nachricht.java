package de.jakub.seminarverwaltung.nachricht;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

/** Simulierte E-Mail im Postausgang. Der Empfänger ist bewusst Text, damit sie das Löschen des Benutzers überlebt. */
@Entity
@Getter
@Setter
public class Nachricht {

    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    private Anlass anlass;

    private String empfaenger;

    // Platz für den Betreff der Vorlage (200) inklusive eingesetztem Seminartitel
    @Column(length = 1000)
    private String betreff;

    // Platz für den Inhalt der Vorlage (2000) inklusive eingesetzter Werte
    @Column(length = 8000)
    private String inhalt;

    private LocalDateTime versendetAm;
}
