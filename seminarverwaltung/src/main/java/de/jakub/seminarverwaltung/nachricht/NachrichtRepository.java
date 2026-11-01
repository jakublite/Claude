package de.jakub.seminarverwaltung.nachricht;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NachrichtRepository extends JpaRepository<Nachricht, Long> {

    List<Nachricht> findAllByOrderByIdDesc();

    List<Nachricht> findByEmpfaengerContainingIgnoreCaseOrderByIdDesc(String empfaenger);
}
