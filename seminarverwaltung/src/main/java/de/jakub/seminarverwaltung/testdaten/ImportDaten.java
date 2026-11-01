package de.jakub.seminarverwaltung.testdaten;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import de.jakub.seminarverwaltung.benutzer.Rolle;
import de.jakub.seminarverwaltung.unterlage.Art;
import de.jakub.seminarverwaltung.unterlage.Bezugsart;
import de.jakub.seminarverwaltung.zertifikat.Niveau;

/**
 * Austauschformat für CSV- und JSON-Import. Verweise laufen über fachliche Schlüssel
 * (E-Mail beim Benutzer, Titel beim Seminar), nicht über technische IDs.
 */
public record ImportDaten(
        List<BenutzerDaten> benutzer,
        List<SeminarDaten> seminare,
        List<ZertifikatDaten> zertifikate,
        List<UnterlageDaten> unterlagen) {

    public ImportDaten {
        benutzer = benutzer == null ? List.of() : benutzer;
        seminare = seminare == null ? List.of() : seminare;
        zertifikate = zertifikate == null ? List.of() : zertifikate;
        unterlagen = unterlagen == null ? List.of() : unterlagen;
    }

    public record BenutzerDaten(String vorname, String nachname, String email, Rolle rolle) {
    }

    /** typ ist ONLINE (mit meetingLink/plattform) oder PRAESENZ (mit ort/raum). */
    public record SeminarDaten(String typ, String titel, String beschreibung, LocalDate startDatum,
            LocalDate endDatum, int maxTeilnehmer, BigDecimal preis, String kursleiterEmail,
            List<String> teilnehmerEmails, String meetingLink, String plattform, String ort, String raum,
            List<String> wartelisteEmails) {
    }

    public record ZertifikatDaten(String seminarTitel, String teilnehmerEmail, Niveau niveau,
            LocalDate ausstellungsdatum, LocalDate gueltigBis) {
    }

    public record UnterlageDaten(String titel, Art art, Bezugsart bezugsart, LocalDate verfuegbarBis,
            BigDecimal gebuehr, boolean bezahlt, String seminarTitel, String benutzerEmail) {
    }
}
