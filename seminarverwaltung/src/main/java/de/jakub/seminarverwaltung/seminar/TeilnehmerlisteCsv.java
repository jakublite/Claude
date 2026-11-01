package de.jakub.seminarverwaltung.seminar;

import java.util.List;

import de.jakub.seminarverwaltung.benutzer.Benutzer;

/** Teilnehmerliste für Excel: UTF-8 mit BOM (sonst falsche Umlaute), Trenner ';'. */
final class TeilnehmerlisteCsv {

    private TeilnehmerlisteCsv() {
    }

    static String erzeuge(Seminar seminar) {
        StringBuilder csv = new StringBuilder("﻿Status;Vorname;Nachname;E-Mail\r\n");
        seminar.getTeilnehmer().forEach(t -> csv.append(zeile("Teilnehmer", t)));
        List<Benutzer> warteliste = seminar.getWarteliste();
        for (int i = 0; i < warteliste.size(); i++) {
            csv.append(zeile("Warteliste " + (i + 1), warteliste.get(i)));
        }
        return csv.toString();
    }

    private static String zeile(String status, Benutzer benutzer) {
        return zelle(status) + ";" + zelle(benutzer.getVorname()) + ";" + zelle(benutzer.getNachname()) + ";"
                + zelle(benutzer.getEmail()) + "\r\n";
    }

    /** Das Apostroph verhindert, dass Excel einen Namen wie =SUMME(...) als Formel ausführt. */
    private static String zelle(String wert) {
        String sicher = wert.matches("^[=+\\-@].*") ? "'" + wert : wert;
        return "\"" + sicher.replace("\"", "\"\"") + "\"";
    }
}
