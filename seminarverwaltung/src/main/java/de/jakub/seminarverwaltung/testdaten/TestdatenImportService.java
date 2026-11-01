package de.jakub.seminarverwaltung.testdaten;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import de.jakub.seminarverwaltung.benutzer.Benutzer;
import de.jakub.seminarverwaltung.benutzer.BenutzerRepository;
import de.jakub.seminarverwaltung.seminar.OnlineSeminar;
import de.jakub.seminarverwaltung.seminar.PraesenzSeminar;
import de.jakub.seminarverwaltung.seminar.Seminar;
import de.jakub.seminarverwaltung.seminar.SeminarRepository;
import de.jakub.seminarverwaltung.testdaten.ImportDaten.BenutzerDaten;
import de.jakub.seminarverwaltung.testdaten.ImportDaten.SeminarDaten;
import de.jakub.seminarverwaltung.testdaten.ImportDaten.UnterlageDaten;
import de.jakub.seminarverwaltung.testdaten.ImportDaten.ZertifikatDaten;
import de.jakub.seminarverwaltung.unterlage.Schulungsunterlage;
import de.jakub.seminarverwaltung.unterlage.SchulungsunterlageRepository;
import de.jakub.seminarverwaltung.zertifikat.Zertifikat;
import de.jakub.seminarverwaltung.zertifikat.ZertifikatRepository;

@Service
public class TestdatenImportService {

    private final BenutzerRepository benutzerRepository;
    private final SeminarRepository seminarRepository;
    private final ZertifikatRepository zertifikatRepository;
    private final SchulungsunterlageRepository unterlageRepository;

    public TestdatenImportService(BenutzerRepository benutzerRepository, SeminarRepository seminarRepository,
            ZertifikatRepository zertifikatRepository, SchulungsunterlageRepository unterlageRepository) {
        this.benutzerRepository = benutzerRepository;
        this.seminarRepository = seminarRepository;
        this.zertifikatRepository = zertifikatRepository;
        this.unterlageRepository = unterlageRepository;
    }

    /** Reihenfolge ist wichtig: Seminare verweisen auf Benutzer, Zertifikate und Unterlagen auf Seminare. */
    @Transactional
    public Map<String, Integer> importiere(ImportDaten daten) {
        daten.benutzer().forEach(b -> benutzerRepository.save(zuBenutzer(b)));
        daten.seminare().forEach(s -> seminarRepository.save(zuSeminar(s)));
        daten.zertifikate().forEach(z -> zertifikatRepository.save(zuZertifikat(z)));
        daten.unterlagen().forEach(u -> unterlageRepository.save(zuUnterlage(u)));
        return Map.of(
                "benutzer", daten.benutzer().size(),
                "seminare", daten.seminare().size(),
                "zertifikate", daten.zertifikate().size(),
                "unterlagen", daten.unterlagen().size());
    }

    private Benutzer zuBenutzer(BenutzerDaten daten) {
        Benutzer benutzer = new Benutzer();
        benutzer.setVorname(daten.vorname());
        benutzer.setNachname(daten.nachname());
        benutzer.setEmail(daten.email());
        benutzer.setRolle(daten.rolle());
        return benutzer;
    }

    private Seminar zuSeminar(SeminarDaten daten) {
        Seminar seminar = switch (daten.typ()) {
            case "ONLINE" -> {
                OnlineSeminar online = new OnlineSeminar();
                online.setMeetingLink(daten.meetingLink());
                online.setPlattform(daten.plattform());
                yield online;
            }
            case "PRAESENZ" -> {
                PraesenzSeminar praesenz = new PraesenzSeminar();
                praesenz.setOrt(daten.ort());
                praesenz.setRaum(daten.raum());
                yield praesenz;
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unbekannter Seminartyp '" + daten.typ() + "' bei Seminar '" + daten.titel() + "'");
        };
        seminar.setTitel(daten.titel());
        seminar.setBeschreibung(daten.beschreibung());
        seminar.setStartDatum(daten.startDatum());
        seminar.setEndDatum(daten.endDatum());
        seminar.setMaxTeilnehmer(daten.maxTeilnehmer());
        seminar.setPreis(daten.preis());
        seminar.setKursleiter(benutzerPerEmail(daten.kursleiterEmail()));
        if (daten.teilnehmerEmails() != null) {
            daten.teilnehmerEmails().forEach(email -> seminar.getTeilnehmer().add(benutzerPerEmail(email)));
        }
        if (daten.wartelisteEmails() != null) {
            daten.wartelisteEmails().forEach(email -> seminar.getWarteliste().add(benutzerPerEmail(email)));
        }
        return seminar;
    }

    private Zertifikat zuZertifikat(ZertifikatDaten daten) {
        Zertifikat zertifikat = new Zertifikat();
        zertifikat.setSeminar(seminarPerTitel(daten.seminarTitel()));
        zertifikat.setTeilnehmer(benutzerPerEmail(daten.teilnehmerEmail()));
        zertifikat.setNiveau(daten.niveau());
        zertifikat.setAusstellungsdatum(daten.ausstellungsdatum());
        zertifikat.setGueltigBis(daten.gueltigBis());
        return zertifikat;
    }

    private Schulungsunterlage zuUnterlage(UnterlageDaten daten) {
        Schulungsunterlage unterlage = new Schulungsunterlage();
        unterlage.setTitel(daten.titel());
        unterlage.setArt(daten.art());
        unterlage.setBezugsart(daten.bezugsart());
        unterlage.setVerfuegbarBis(daten.verfuegbarBis());
        unterlage.setGebuehr(daten.gebuehr());
        unterlage.setBezahlt(daten.bezahlt());
        if (daten.seminarTitel() != null) {
            unterlage.setSeminar(seminarPerTitel(daten.seminarTitel()));
        }
        if (daten.benutzerEmail() != null) {
            unterlage.setBenutzer(benutzerPerEmail(daten.benutzerEmail()));
        }
        return unterlage;
    }

    private Benutzer benutzerPerEmail(String email) {
        return benutzerRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Benutzer '" + email + "' unbekannt"));
    }

    private Seminar seminarPerTitel(String titel) {
        return seminarRepository.findByTitel(titel)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seminar '" + titel + "' unbekannt"));
    }
}
