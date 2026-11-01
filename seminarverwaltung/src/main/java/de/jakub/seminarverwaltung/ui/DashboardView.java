package de.jakub.seminarverwaltung.ui;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.jakub.seminarverwaltung.benutzer.BenutzerRepository;
import de.jakub.seminarverwaltung.seminar.Seminar;
import de.jakub.seminarverwaltung.seminar.SeminarRepository;
import de.jakub.seminarverwaltung.unterlage.Schulungsunterlage;
import de.jakub.seminarverwaltung.unterlage.SchulungsunterlageRepository;

@Route("")
@PageTitle("Übersicht")
public class DashboardView extends VerticalLayout {

    public DashboardView(SeminarRepository seminarRepository, BenutzerRepository benutzerRepository,
            SchulungsunterlageRepository unterlageRepository) {
        addClassName("seite");

        List<Seminar> seminare = seminarRepository.findAll();
        List<Schulungsunterlage> ueberfaellig = unterlageRepository.findAll().stream()
                .filter(Schulungsunterlage::isUeberfaellig)
                .toList();
        int belegt = seminare.stream().mapToInt(Seminar::getAnzahlTeilnehmer).sum();
        int plaetze = seminare.stream().mapToInt(Seminar::getMaxTeilnehmer).sum();
        int wartend = seminare.stream().mapToInt(s -> s.getWarteliste().size()).sum();

        Div kennzahlen = new Div(
                kennzahl("Seminare", String.valueOf(seminare.size()), VaadinIcon.CALENDAR),
                kennzahl("Belegte Plätze", belegt + " / " + plaetze, VaadinIcon.USER_CHECK),
                kennzahl("Auf Warteliste", String.valueOf(wartend), VaadinIcon.CLOCK),
                kennzahl("Benutzer", String.valueOf(benutzerRepository.count()), VaadinIcon.USERS),
                kennzahl("Überfällige Ausleihen", String.valueOf(ueberfaellig.size()), VaadinIcon.WARNING));
        kennzahlen.addClassName("kennzahlen");

        Div spalten = new Div(naechsteSeminare(seminare), ueberfaelligeAusleihen(ueberfaellig));
        spalten.addClassName("zwei-spalten");

        add(kennzahlen, spalten);
    }

    private Card kennzahl(String titel, String wert, VaadinIcon icon) {
        Card karte = new Card();
        karte.addClassName("kennzahl");
        karte.setTitle(titel);
        karte.setHeaderSuffix(icon.create());
        Span zahl = new Span(wert);
        zahl.addClassName("kennzahl-wert");
        karte.add(zahl);
        return karte;
    }

    private Card naechsteSeminare(List<Seminar> seminare) {
        Card karte = new Card();
        karte.setTitle("Nächste Seminare");
        seminare.stream()
                .filter(s -> !s.getEndDatum().isBefore(LocalDate.now()))
                .sorted(Comparator.comparing(Seminar::getStartDatum))
                .limit(5)
                .map(this::seminarZeile)
                .forEach(karte::add);
        return karte;
    }

    private Component seminarZeile(Seminar seminar) {
        Span titel = new Span(seminar.getTitel());
        titel.addClassName("zeile-titel");
        Span zeitraum = new Span(UiHilfe.datum(seminar.getStartDatum()) + " – " + UiHilfe.datum(seminar.getEndDatum()));
        zeitraum.addClassName("gedimmt");
        Div kopf = new Div(titel, SeminareView.typBadge(seminar));
        kopf.addClassName("zeile-kopf");

        ProgressBar auslastung = new ProgressBar(0, seminar.getMaxTeilnehmer(), seminar.getAnzahlTeilnehmer());
        Span plaetze = new Span(seminar.getAnzahlTeilnehmer() + " / " + seminar.getMaxTeilnehmer() + " Plätze");
        plaetze.addClassName("gedimmt");

        Div zeile = new Div(kopf, zeitraum, auslastung, plaetze);
        zeile.addClassName("liste-zeile");
        return zeile;
    }

    private Card ueberfaelligeAusleihen(List<Schulungsunterlage> ueberfaellig) {
        Card karte = new Card();
        karte.setTitle("Überfällige Ausleihen");
        if (ueberfaellig.isEmpty()) {
            karte.add(new Span("Keine – alles zurückgegeben."));
        }
        for (Schulungsunterlage unterlage : ueberfaellig) {
            Span titel = new Span(unterlage.getTitel());
            titel.addClassName("zeile-titel");
            Badge faellig = new Badge("seit " + UiHilfe.datum(unterlage.getVerfuegbarBis()));
            faellig.addThemeVariants(BadgeVariant.ERROR);
            Div kopf = new Div(titel, faellig);
            kopf.addClassName("zeile-kopf");
            String wer = unterlage.getBenutzer() == null ? "ohne Benutzer"
                    : unterlage.getBenutzer().getVorname() + " " + unterlage.getBenutzer().getNachname();
            Span benutzer = new Span(wer);
            benutzer.addClassName("gedimmt");
            Div zeile = new Div(kopf, benutzer);
            zeile.addClassName("liste-zeile");
            karte.add(zeile);
        }
        return karte;
    }
}
