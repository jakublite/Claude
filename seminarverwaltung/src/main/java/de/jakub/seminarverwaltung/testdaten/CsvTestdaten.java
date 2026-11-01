package de.jakub.seminarverwaltung.testdaten;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.springframework.core.io.ClassPathResource;

import de.jakub.seminarverwaltung.benutzer.Rolle;
import de.jakub.seminarverwaltung.testdaten.ImportDaten.BenutzerDaten;
import de.jakub.seminarverwaltung.testdaten.ImportDaten.SeminarDaten;
import de.jakub.seminarverwaltung.testdaten.ImportDaten.UnterlageDaten;
import de.jakub.seminarverwaltung.testdaten.ImportDaten.ZertifikatDaten;
import de.jakub.seminarverwaltung.unterlage.Art;
import de.jakub.seminarverwaltung.unterlage.Bezugsart;
import de.jakub.seminarverwaltung.zertifikat.Niveau;

/**
 * Liest die CSV-Dateien eines Ordners unter src/main/resources (demodaten für die App, testdaten für die Tests).
 * Format: erste Zeile Spaltenköpfe, Trenner ';', Listen innerhalb einer Spalte mit ',' getrennt, leere Zelle = null.
 */
public final class CsvTestdaten {

    private CsvTestdaten() {
    }

    public static ImportDaten lese(String ordner) {
        return new ImportDaten(
                lese(ordner + "/benutzer.csv", z -> new BenutzerDaten(z[0], z[1], z[2], Rolle.valueOf(z[3]))),
                lese(ordner + "/seminare.csv", z -> new SeminarDaten(z[0], z[1], z[2], datum(z[3]), datum(z[4]),
                        Integer.parseInt(z[5]), betrag(z[6]), z[7], liste(z[8]), z[9], z[10], z[11], z[12], liste(z[13]))),
                lese(ordner + "/zertifikate.csv", z -> new ZertifikatDaten(z[0], z[1], Niveau.valueOf(z[2]),
                        datum(z[3]), datum(z[4]))),
                lese(ordner + "/unterlagen.csv", z -> new UnterlageDaten(z[0], Art.valueOf(z[1]),
                        Bezugsart.valueOf(z[2]), datum(z[3]), betrag(z[4]), Boolean.parseBoolean(z[5]), z[6], z[7])));
    }

    private static <T> List<T> lese(String pfad, Function<String[], T> zuDatensatz) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource(pfad).getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .skip(1)
                    .filter(zeile -> !zeile.isBlank())
                    .map(CsvTestdaten::zellen)
                    .map(zuDatensatz)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("CSV-Datei " + pfad + " nicht lesbar", e);
        }
    }

    /** -1 behält leere Zellen am Zeilenende, leere Zellen werden zu null. */
    private static String[] zellen(String zeile) {
        return Arrays.stream(zeile.split(";", -1))
                .map(String::trim)
                .map(zelle -> zelle.isEmpty() ? null : zelle)
                .toArray(String[]::new);
    }

    private static LocalDate datum(String wert) {
        return wert == null ? null : LocalDate.parse(wert);
    }

    private static BigDecimal betrag(String wert) {
        return wert == null ? null : new BigDecimal(wert);
    }

    private static List<String> liste(String wert) {
        return wert == null ? List.of() : Arrays.stream(wert.split(",")).map(String::trim).toList();
    }
}
