package de.jakub.seminarverwaltung.unterlage;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SchulungsunterlageRepository extends JpaRepository<Schulungsunterlage, Long> {

    List<Schulungsunterlage> findByTitelContainingIgnoreCase(String titel);

    List<Schulungsunterlage> findBySeminarId(Long seminarId);
}
