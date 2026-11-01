package de.jakub.seminarverwaltung.ui;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.progressbar.ProgressBar;

import de.jakub.seminarverwaltung.benutzer.Benutzer;
import de.jakub.seminarverwaltung.benutzer.BenutzerRepository;
import de.jakub.seminarverwaltung.seminar.OnlineSeminar;
import de.jakub.seminarverwaltung.seminar.PraesenzSeminar;
import de.jakub.seminarverwaltung.seminar.Seminar;
import de.jakub.seminarverwaltung.seminar.SeminarService;
import de.jakub.seminarverwaltung.sicherheit.Angemeldet;
import de.jakub.seminarverwaltung.unterlage.Schulungsunterlage;
import de.jakub.seminarverwaltung.unterlage.SchulungsunterlageRepository;
import de.jakub.seminarverwaltung.wetter.Wetter;
import de.jakub.seminarverwaltung.wetter.WetterService;
import de.jakub.seminarverwaltung.zertifikat.Zertifikat;
import de.jakub.seminarverwaltung.zertifikat.ZertifikatRepository;

/**
 * 360°-Sicht auf ein Seminar: Stammdaten, Kursleiter, Wetter, Teilnehmer, Zertifikate und Unterlagen.
 * Teilnehmer sehen keine anderen Personen und ändern nur die eigene Anmeldung.
 */
class SeminarDetailDialog extends Dialog {

    private final SeminareView liste;
    private final SeminarService seminarService;
    private final BenutzerRepository benutzerRepository;
    private final WetterService wetterService;
    private final ZertifikatRepository zertifikatRepository;
    private final SchulungsunterlageRepository unterlageRepository;
    private final Angemeldet angemeldet;

    private final Div inhalt = new Div();

    /** Aktueller Stand, ändert sich nach An- und Abmeldungen im Dialog. */
    private Seminar seminar;

    SeminarDetailDialog(Seminar seminar, SeminareView liste, SeminarService seminarService,
            BenutzerRepository benutzerRepository, WetterService wetterService,
            ZertifikatRepository zertifikatRepository, SchulungsunterlageRepository unterlageRepository,
            Angemeldet angemeldet) {
        this.liste = liste;
        this.seminarService = seminarService;
        this.benutzerRepository = benutzerRepository;
        this.wetterService = wetterService;
        this.zertifikatRepository = zertifikatRepository;
        this.unterlageRepository = unterlageRepository;
        this.angemeldet = angemeldet;

        setWidth("min(980px, 96vw)");
        inhalt.addClassName("detail");
        add(inhalt);

        Button schliessen = new Button("Schließen", e -> close());
        if (angemeldet.istKursleiter()) {
            Button bearbeiten = new Button("Bearbeiten", VaadinIcon.EDIT.create(), e -> {
                close();
                liste.bearbeite(this.seminar);
            });
            bearbeiten.addThemeVariants(ButtonVariant.PRIMARY);
            Button loeschen = new Button("Löschen", VaadinIcon.TRASH.create(), e -> {
                close();
                liste.loesche(this.seminar);
            });
            loeschen.addThemeVariants(ButtonVariant.ERROR, ButtonVariant.TERTIARY);
            getFooter().add(loeschen, schliessen, bearbeiten);
        } else {
            getFooter().add(schliessen);
        }

        zeige(seminar);
    }

    private void zeige(Seminar seminar) {
        this.seminar = seminar;
        setHeaderTitle(seminar.getTitel());
        getHeader().removeAll();
        getHeader().add(SeminareView.typBadge(seminar));

        inhalt.removeAll();
        Div kacheln = new Div(eckdaten(seminar), visitenkarte(seminar.getKursleiter()), dritteKachel(seminar));
        kacheln.addClassName("kacheln");
        inhalt.add(kacheln);
        if (seminar.getBeschreibung() != null) {
            inhalt.add(new Paragraph(seminar.getBeschreibung()));
        }
        Div unten = new Div(angemeldet.istKursleiter() ? teilnehmer(seminar) : eigeneTeilnahme(seminar),
                zertifikateUndUnterlagen(seminar));
        unten.addClassName("zwei-spalten");
        inhalt.add(unten);
    }

    private Card eckdaten(Seminar seminar) {
        Card karte = new Card();
        karte.setTitle("Eckdaten");
        karte.add(
                zeile("Zeitraum", UiHilfe.datum(seminar.getStartDatum()) + " – " + UiHilfe.datum(seminar.getEndDatum())),
                zeile("Preis", UiHilfe.betrag(seminar.getPreis())));
        ProgressBar auslastung = new ProgressBar(0, seminar.getMaxTeilnehmer(), seminar.getAnzahlTeilnehmer());
        karte.add(zeile("Auslastung", seminar.getAnzahlTeilnehmer() + " / " + seminar.getMaxTeilnehmer()), auslastung);
        if (!seminar.getWarteliste().isEmpty()) {
            karte.add(zeile("Warteliste", String.valueOf(seminar.getWarteliste().size())));
        }
        return karte;
    }

    private Card visitenkarte(Benutzer kursleiter) {
        String name = kursleiter.getVorname() + " " + kursleiter.getNachname();
        Card karte = new Card();
        karte.setTitle("Kursleitung");
        Span nameSpan = new Span(name);
        nameSpan.addClassName("zeile-titel");
        Anchor mail = new Anchor("mailto:" + kursleiter.getEmail(), kursleiter.getEmail());
        Div text = new Div(nameSpan, mail);
        text.addClassName("visitenkarte-text");
        Div visitenkarte = new Div(new Avatar(name), text);
        visitenkarte.addClassName("visitenkarte");
        karte.add(visitenkarte);
        return karte;
    }

    /** Präsenz: Ort und Wetter, Online: Plattform und Meeting-Link. */
    private Card dritteKachel(Seminar seminar) {
        Card karte = new Card();
        if (seminar instanceof OnlineSeminar online) {
            karte.setTitle("Online-Meeting");
            karte.add(zeile("Plattform", online.getPlattform() == null ? "–" : online.getPlattform()));
            if (angemeldet.istKursleiter() || nimmtTeil(seminar)) {
                karte.add(new Anchor(online.getMeetingLink(), "Meeting öffnen", AnchorTarget.BLANK));
            } else {
                karte.add(new Span("Den Link siehst du nach der Anmeldung."));
            }
            return karte;
        }
        PraesenzSeminar praesenz = (PraesenzSeminar) seminar;
        karte.setTitle("Wetter in " + praesenz.getOrt());
        karte.setHeaderSuffix(VaadinIcon.CLOUD.create());
        try {
            Wetter wetter = wetterService.ermittleWetter(praesenz.getOrt());
            Span temperatur = new Span(wetter.temperaturText());
            temperatur.addClassName("kennzahl-wert");
            karte.add(temperatur,
                    zeile("Niederschlag", wetter.niederschlagswahrscheinlichkeitProzent() + " %"),
                    zeile("Wind", wetter.windText()));
        } catch (RuntimeException ex) {
            karte.add(new Span("Wetter gerade nicht verfügbar"));
        }
        if (praesenz.getRaum() != null) {
            karte.add(zeile("Raum", praesenz.getRaum()));
        }
        return karte;
    }

    private Card teilnehmer(Seminar seminar) {
        Card karte = new Card();
        karte.setTitle("Teilnehmer");

        Set<Long> dabei = Stream.concat(seminar.getTeilnehmer().stream(), seminar.getWarteliste().stream())
                .map(Benutzer::getId)
                .collect(Collectors.toSet());
        ComboBox<Benutzer> auswahl = new ComboBox<>();
        auswahl.setPlaceholder("Benutzer anmelden …");
        auswahl.setItems(benutzerRepository.findAll().stream()
                .filter(b -> !dabei.contains(b.getId()))
                .filter(b -> !b.getId().equals(seminar.getKursleiter().getId()))
                .toList());
        auswahl.setItemLabelGenerator(b -> b.getVorname() + " " + b.getNachname());
        auswahl.addClassName("wachsen");
        Button anmelden = new Button("Anmelden", VaadinIcon.PLUS.create(), e -> {
            if (auswahl.getValue() != null) {
                aendereTeilnahme(() -> seminarService.anmelden(seminar.getId(), auswahl.getValue().getId()));
            }
        });
        anmelden.addThemeVariants(ButtonVariant.PRIMARY);
        anmelden.setEnabled(!seminar.isAusgebucht());
        Button warten = new Button("Auf Warteliste", VaadinIcon.CLOCK.create(), e -> {
            if (auswahl.getValue() != null) {
                aendereTeilnahme(() -> seminarService.aufWarteliste(seminar.getId(), auswahl.getValue().getId()));
            }
        });
        warten.setVisible(seminar.isAusgebucht());
        Div neu = new Div(auswahl, anmelden, warten);
        neu.addClassName("zeile-kopf");
        karte.add(neu);

        if (seminar.getTeilnehmer().isEmpty()) {
            karte.add(new Span("Noch niemand angemeldet."));
        }
        for (Benutzer t : seminar.getTeilnehmer()) {
            karte.add(personZeile(t, "Abmelden", () -> seminarService.abmelden(seminar.getId(), t.getId())));
        }
        if (!seminar.getWarteliste().isEmpty()) {
            Span ueberschrift = new Span("Warteliste");
            ueberschrift.addClassName("zeile-titel");
            karte.add(ueberschrift);
        }
        for (Benutzer w : seminar.getWarteliste()) {
            karte.add(personZeile(w, "Von der Warteliste nehmen", () -> seminarService.vonWarteliste(seminar.getId(), w.getId())));
        }

        Anchor csv = new Anchor("api/seminare/" + seminar.getId() + "/teilnehmerliste.csv", "Teilnehmerliste als CSV");
        csv.setTarget(AnchorTarget.BLANK);
        karte.add(csv);
        return karte;
    }

    private Card eigeneTeilnahme(Seminar seminar) {
        Long ich = angemeldet.benutzer().getId();
        Card karte = new Card();
        karte.setTitle("Meine Teilnahme");

        int warteplatz = seminar.getWarteliste().stream().map(Benutzer::getId).toList().indexOf(ich);
        if (nimmtTeil(seminar)) {
            karte.add(teilnahmeZeile("Du bist angemeldet.",
                    "Abmelden", () -> seminarService.abmelden(seminar.getId(), ich)));
        } else if (warteplatz >= 0) {
            karte.add(teilnahmeZeile("Du stehst auf Platz " + (warteplatz + 1) + " der Warteliste.",
                    "Von der Warteliste nehmen", () -> seminarService.vonWarteliste(seminar.getId(), ich)));
        } else if (seminar.isAusgebucht()) {
            karte.add(teilnahmeZeile("Das Seminar ist ausgebucht.",
                    "Auf Warteliste", () -> seminarService.aufWarteliste(seminar.getId(), ich)));
        } else {
            karte.add(teilnahmeZeile("Es sind noch Plätze frei.",
                    "Anmelden", () -> seminarService.anmelden(seminar.getId(), ich)));
        }
        return karte;
    }

    private boolean nimmtTeil(Seminar seminar) {
        return seminar.getTeilnehmer().stream().anyMatch(angemeldet::istSelbst);
    }

    private Component teilnahmeZeile(String text, String buttonText, Supplier<Seminar> aktion) {
        Button button = new Button(buttonText, e -> aendereTeilnahme(aktion));
        button.addThemeVariants(ButtonVariant.PRIMARY);
        Span textSpan = new Span(text);
        textSpan.addClassName("wachsen");
        Div zeile = new Div(textSpan, button);
        zeile.addClassName("zeile-kopf");
        return zeile;
    }

    private Component personZeile(Benutzer person, String entfernenText, Supplier<Seminar> entfernen) {
        String name = person.getVorname() + " " + person.getNachname();
        Avatar avatar = new Avatar(name);
        avatar.addClassName("klein");
        Span nameSpan = new Span(name);
        nameSpan.addClassName("wachsen");
        Button button = new Button(VaadinIcon.CLOSE_SMALL.create(), e -> aendereTeilnahme(entfernen));
        button.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.ERROR);
        button.setTooltipText(entfernenText);
        Div zeile = new Div(avatar, nameSpan, button);
        zeile.addClassName("zeile-kopf");
        return zeile;
    }

    private void aendereTeilnahme(Supplier<Seminar> aktion) {
        try {
            Seminar aktualisiert = aktion.get();
            zeige(aktualisiert);
            liste.aktualisiere();
        } catch (RuntimeException ex) {
            UiHilfe.fehler(UiHilfe.fehlertext(ex));
        }
    }

    private Card zertifikateUndUnterlagen(Seminar seminar) {
        Card karte = new Card();
        karte.setTitle("Zertifikate & Unterlagen");

        List<Zertifikat> zertifikate = zertifikatRepository.findBySeminarId(seminar.getId()).stream()
                .filter(z -> angemeldet.darfSehen(z.getTeilnehmer()))
                .toList();
        for (Zertifikat z : zertifikate) {
            Badge niveau = new Badge("Niveau " + z.getNiveau());
            niveau.addThemeVariants(BadgeVariant.SUCCESS);
            Div rechts = new Div(niveau, ZertifikateView.dokumentLink(z));
            rechts.addClassName("zeile-kopf");
            karte.add(eintrag(VaadinIcon.DIPLOMA, z.getTeilnehmer().getVorname() + " " + z.getTeilnehmer().getNachname(), rechts));
        }
        List<Schulungsunterlage> unterlagen = unterlageRepository.findBySeminarId(seminar.getId()).stream()
                .filter(u -> u.getBenutzer() == null || angemeldet.darfSehen(u.getBenutzer()))
                .toList();
        for (Schulungsunterlage u : unterlagen) {
            karte.add(eintrag(VaadinIcon.BOOK, u.getTitel(), UnterlagenView.statusBadge(u)));
        }
        if (zertifikate.isEmpty() && unterlagen.isEmpty()) {
            karte.add(new Span("Noch keine vorhanden."));
        }
        return karte;
    }

    private static Component eintrag(VaadinIcon icon, String text, Component badge) {
        Span textSpan = new Span(text);
        textSpan.addClassName("wachsen");
        Div zeile = new Div(icon.create(), textSpan, badge);
        zeile.addClassName("zeile-kopf");
        return zeile;
    }

    private static Component zeile(String label, String wert) {
        Span l = new Span(label);
        l.addClassName("gedimmt");
        Div zeile = new Div(l, new Span(wert));
        zeile.addClassName("label-wert");
        return zeile;
    }
}
