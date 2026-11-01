package de.jakub.seminarverwaltung.seminar;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SeminarRepository extends JpaRepository<Seminar, Long> {

    Optional<Seminar> findByTitel(String titel);

    List<Seminar> findByTitelContainingIgnoreCase(String titel);
}
