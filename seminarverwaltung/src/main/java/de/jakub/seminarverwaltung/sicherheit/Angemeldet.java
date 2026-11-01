package de.jakub.seminarverwaltung.sicherheit;

import org.springframework.stereotype.Component;

import com.vaadin.flow.spring.security.AuthenticationContext;

import de.jakub.seminarverwaltung.benutzer.Benutzer;
import de.jakub.seminarverwaltung.benutzer.BenutzerRepository;
import de.jakub.seminarverwaltung.benutzer.Rolle;

/** Die in der Oberfläche angemeldete Person. */
@Component
public class Angemeldet {

    private final AuthenticationContext authenticationContext;
    private final BenutzerRepository benutzerRepository;

    public Angemeldet(AuthenticationContext authenticationContext, BenutzerRepository benutzerRepository) {
        this.authenticationContext = authenticationContext;
        this.benutzerRepository = benutzerRepository;
    }

    public boolean istKursleiter() {
        return authenticationContext.hasRole(Rolle.KURSLEITER.name());
    }

    public String email() {
        return authenticationContext.getPrincipalName().orElseThrow();
    }

    public Benutzer benutzer() {
        return benutzerRepository.findByEmail(email()).orElseThrow();
    }

    public boolean istSelbst(Benutzer benutzer) {
        return benutzer != null && email().equals(benutzer.getEmail());
    }

    /** Kursleiter sehen alle Daten, Teilnehmer nur die eigenen. */
    public boolean darfSehen(Benutzer besitzer) {
        return istKursleiter() || istSelbst(besitzer);
    }

    public void ausloggen() {
        authenticationContext.logout();
    }
}
