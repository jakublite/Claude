package de.jakub.seminarverwaltung.benutzer;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BenutzerRepository extends JpaRepository<Benutzer, Long> {

    Optional<Benutzer> findByEmail(String email);

    List<Benutzer> findByNachnameContainingIgnoreCase(String nachname);
}
