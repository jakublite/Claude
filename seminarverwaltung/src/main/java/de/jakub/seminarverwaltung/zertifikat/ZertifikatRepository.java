package de.jakub.seminarverwaltung.zertifikat;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ZertifikatRepository extends JpaRepository<Zertifikat, Long> {

    List<Zertifikat> findBySeminarId(Long seminarId);
}
