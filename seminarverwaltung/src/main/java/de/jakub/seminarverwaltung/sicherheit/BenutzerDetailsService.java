package de.jakub.seminarverwaltung.sicherheit;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import de.jakub.seminarverwaltung.benutzer.BenutzerRepository;

/** Login mit der E-Mail. Benutzer ohne Passwort, z. B. aus dem JSON-Import, können sich nicht anmelden. */
@Service
public class BenutzerDetailsService implements UserDetailsService {

    private final BenutzerRepository benutzerRepository;

    public BenutzerDetailsService(BenutzerRepository benutzerRepository) {
        this.benutzerRepository = benutzerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return benutzerRepository.findByEmail(email)
                .filter(b -> b.getPasswortHash() != null)
                .map(b -> User.withUsername(b.getEmail()).password(b.getPasswortHash()).roles(b.getRolle().name()).build())
                .orElseThrow(() -> new UsernameNotFoundException("Kein Login für " + email));
    }
}
