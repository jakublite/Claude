package de.jakub.seminarverwaltung.nachricht;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import de.jakub.seminarverwaltung.benutzer.Benutzer;
import de.jakub.seminarverwaltung.seminar.OnlineSeminar;
import de.jakub.seminarverwaltung.seminar.PraesenzSeminar;
import de.jakub.seminarverwaltung.seminar.Seminar;

/** Erzeugt E-Mails aus Vorlagen. Es gibt keinen Mailserver, sie landen nur im Postausgang. */
@Service
public class NachrichtenService {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final NachrichtRepository nachrichtRepository;
    private final VorlageRepository vorlageRepository;

    public NachrichtenService(NachrichtRepository nachrichtRepository, VorlageRepository vorlageRepository) {
        this.nachrichtRepository = nachrichtRepository;
        this.vorlageRepository = vorlageRepository;
    }

    public void sende(Anlass anlass, Benutzer empfaenger, Seminar seminar) {
        Vorlage vorlage = vorlage(anlass);
        Nachricht nachricht = new Nachricht();
        nachricht.setAnlass(anlass);
        nachricht.setEmpfaenger(empfaenger.getEmail());
        nachricht.setBetreff(fuelle(vorlage.getBetreff(), empfaenger, seminar));
        nachricht.setInhalt(fuelle(vorlage.getInhalt(), empfaenger, seminar));
        nachricht.setVersendetAm(LocalDateTime.now());
        nachrichtRepository.save(nachricht);
    }

    /** Für jeden Anlass die gespeicherte Vorlage oder den Standardtext. */
    public List<Vorlage> alleVorlagen() {
        return Arrays.stream(Anlass.values()).map(this::vorlage).toList();
    }

    private Vorlage vorlage(Anlass anlass) {
        return vorlageRepository.findById(anlass).orElseGet(() -> {
            Vorlage standard = new Vorlage();
            standard.setAnlass(anlass);
            standard.setBetreff(anlass.getStandardBetreff());
            standard.setInhalt(anlass.getStandardInhalt());
            return standard;
        });
    }

    private static String fuelle(String text, Benutzer empfaenger, Seminar seminar) {
        return text.replace("{vorname}", empfaenger.getVorname())
                .replace("{nachname}", empfaenger.getNachname())
                .replace("{seminar}", seminar.getTitel())
                .replace("{zeitraum}", DATUM.format(seminar.getStartDatum()) + " – " + DATUM.format(seminar.getEndDatum()))
                .replace("{ort}", ort(seminar));
    }

    private static String ort(Seminar seminar) {
        return switch (seminar) {
            case PraesenzSeminar p -> p.getRaum() == null ? p.getOrt() : p.getOrt() + ", " + p.getRaum();
            case OnlineSeminar o -> o.getPlattform() == null ? "Online" : "Online (" + o.getPlattform() + ")";
            default -> "";
        };
    }
}
