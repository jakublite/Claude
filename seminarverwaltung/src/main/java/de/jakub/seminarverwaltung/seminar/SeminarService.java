package de.jakub.seminarverwaltung.seminar;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import de.jakub.seminarverwaltung.benutzer.Benutzer;
import de.jakub.seminarverwaltung.benutzer.BenutzerRepository;
import de.jakub.seminarverwaltung.nachricht.Anlass;
import de.jakub.seminarverwaltung.nachricht.NachrichtenService;

@Service
public class SeminarService {

    private final SeminarRepository seminarRepository;
    private final BenutzerRepository benutzerRepository;
    private final NachrichtenService nachrichtenService;

    public SeminarService(SeminarRepository seminarRepository, BenutzerRepository benutzerRepository,
            NachrichtenService nachrichtenService) {
        this.seminarRepository = seminarRepository;
        this.benutzerRepository = benutzerRepository;
        this.nachrichtenService = nachrichtenService;
    }

    /** Nach dem Erhöhen von maxTeilnehmer können Plätze frei sein, die der Warteliste zustehen. */
    @Transactional
    public Seminar speichern(Seminar seminar) {
        Seminar gespeichert = seminarRepository.save(seminar);
        rueckeNach(gespeichert);
        return gespeichert;
    }

    @Transactional
    public Seminar anmelden(Long seminarId, Long benutzerId) {
        Seminar seminar = ladeSeminar(seminarId);
        Benutzer benutzer = ladeBenutzer(benutzerId);
        pruefeNochNichtDabei(seminar, benutzer);
        if (seminar.isAusgebucht()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seminar ist ausgebucht");
        }
        seminar.getTeilnehmer().add(benutzer);
        nachrichtenService.sende(Anlass.ANMELDUNG, benutzer, seminar);
        return seminar;
    }

    @Transactional
    public Seminar abmelden(Long seminarId, Long benutzerId) {
        Seminar seminar = ladeSeminar(seminarId);
        Benutzer benutzer = seminar.getTeilnehmer().stream()
                .filter(t -> t.getId().equals(benutzerId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benutzer ist nicht angemeldet"));
        seminar.getTeilnehmer().remove(benutzer);
        nachrichtenService.sende(Anlass.ABMELDUNG, benutzer, seminar);
        rueckeNach(seminar);
        return seminar;
    }

    @Transactional
    public Seminar aufWarteliste(Long seminarId, Long benutzerId) {
        Seminar seminar = ladeSeminar(seminarId);
        Benutzer benutzer = ladeBenutzer(benutzerId);
        pruefeNochNichtDabei(seminar, benutzer);
        if (!seminar.isAusgebucht()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Es sind noch Plätze frei, bitte direkt anmelden");
        }
        seminar.getWarteliste().add(benutzer);
        nachrichtenService.sende(Anlass.WARTELISTE, benutzer, seminar);
        return seminar;
    }

    @Transactional
    public Seminar vonWarteliste(Long seminarId, Long benutzerId) {
        Seminar seminar = ladeSeminar(seminarId);
        if (!seminar.getWarteliste().removeIf(w -> w.getId().equals(benutzerId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Benutzer steht nicht auf der Warteliste");
        }
        return seminar;
    }

    private void rueckeNach(Seminar seminar) {
        while (!seminar.isAusgebucht() && !seminar.getWarteliste().isEmpty()) {
            Benutzer naechster = seminar.getWarteliste().removeFirst();
            seminar.getTeilnehmer().add(naechster);
            nachrichtenService.sende(Anlass.NACHRUECKEN, naechster, seminar);
        }
    }

    private void pruefeNochNichtDabei(Seminar seminar, Benutzer benutzer) {
        if (seminar.getKursleiter().getId().equals(benutzer.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Der Kursleiter kann nicht Teilnehmer sein");
        }
        if (seminar.getTeilnehmer().contains(benutzer)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Benutzer ist bereits angemeldet");
        }
        if (seminar.getWarteliste().contains(benutzer)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Benutzer steht bereits auf der Warteliste");
        }
    }

    private Seminar ladeSeminar(Long seminarId) {
        return seminarRepository.findById(seminarId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seminar " + seminarId + " nicht gefunden"));
    }

    private Benutzer ladeBenutzer(Long benutzerId) {
        return benutzerRepository.findById(benutzerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benutzer " + benutzerId + " nicht gefunden"));
    }
}
