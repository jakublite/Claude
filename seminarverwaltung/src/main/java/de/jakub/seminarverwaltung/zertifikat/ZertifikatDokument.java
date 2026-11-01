package de.jakub.seminarverwaltung.zertifikat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.web.util.HtmlUtils;

/** Druckbare HTML-Seite eines Zertifikats, im Browser auch als PDF speicherbar. */
final class ZertifikatDokument {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private ZertifikatDokument() {
    }

    static String erzeuge(Zertifikat zertifikat) {
        String name = zertifikat.getTeilnehmer().getVorname() + " " + zertifikat.getTeilnehmer().getNachname();
        return """
                <!doctype html>
                <html lang="de">
                <head>
                  <meta charset="utf-8">
                  <title>Zertifikat %1$s</title>
                  <style>
                    body { font-family: Georgia, serif; color: #222; margin: 0; }
                    main { max-width: 720px; margin: 40px auto; padding: 60px 40px; border: 3px double #4b4fc4; text-align: center; }
                    h1 { font-size: 2.4rem; margin: 0 0 40px; color: #4b4fc4; }
                    .name { font-size: 1.8rem; font-weight: bold; margin: 16px 0; }
                    .seminar { font-size: 1.4rem; font-style: italic; margin: 16px 0 32px; }
                    .klein { font-size: 0.95rem; color: #555; margin: 4px 0; }
                    button { display: block; margin: 20px auto; padding: 8px 20px; font-size: 1rem; }
                    @media print { button { display: none; } main { border-width: 3px; margin: 0 auto; } }
                  </style>
                </head>
                <body>
                  <button onclick="window.print()">Drucken oder als PDF speichern</button>
                  <main>
                    <h1>Zertifikat</h1>
                    <p>Hiermit wird bestätigt, dass</p>
                    <p class="name">%1$s</p>
                    <p>erfolgreich teilgenommen hat an</p>
                    <p class="seminar">%2$s</p>
                    <p>Niveau %3$s</p>
                    <p class="klein">Ausgestellt am %4$s</p>
                    <p class="klein">%5$s</p>
                  </main>
                </body>
                </html>
                """.formatted(html(name), html(zertifikat.getSeminar().getTitel()), zertifikat.getNiveau(),
                DATUM.format(zertifikat.getAusstellungsdatum()), gueltigkeit(zertifikat.getGueltigBis()));
    }

    private static String gueltigkeit(LocalDate gueltigBis) {
        return gueltigBis == null ? "Unbegrenzt gültig" : "Gültig bis " + DATUM.format(gueltigBis);
    }

    private static String html(String text) {
        return HtmlUtils.htmlEscape(text, "UTF-8");
    }
}
