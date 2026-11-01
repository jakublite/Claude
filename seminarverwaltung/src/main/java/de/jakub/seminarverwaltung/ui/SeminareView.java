package de.jakub.seminarverwaltung.ui;

import java.util.Comparator;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.jakub.seminarverwaltung.benutzer.BenutzerRepository;
import de.jakub.seminarverwaltung.seminar.OnlineSeminar;
import de.jakub.seminarverwaltung.seminar.PraesenzSeminar;
import de.jakub.seminarverwaltung.seminar.Seminar;
import de.jakub.seminarverwaltung.seminar.SeminarRepository;
import de.jakub.seminarverwaltung.seminar.SeminarService;
import de.jakub.seminarverwaltung.unterlage.SchulungsunterlageRepository;
import de.jakub.seminarverwaltung.wetter.WetterService;
import de.jakub.seminarverwaltung.zertifikat.ZertifikatRepository;

@Route("seminare")
@PageTitle("Seminare")
public class SeminareView extends VerticalLayout {

    private final SeminarRepository seminarRepository;
    private final BenutzerRepository benutzerRepository;
    private final SeminarService seminarService;
    private final WetterService wetterService;
    private final ZertifikatRepository zertifikatRepository;
    private final SchulungsunterlageRepository unterlageRepository;

    private final Grid<Seminar> grid = new Grid<>();
    private final TextField suche;

    public SeminareView(SeminarRepository seminarRepository, BenutzerRepository benutzerRepository,
            SeminarService seminarService, WetterService wetterService, ZertifikatRepository zertifikatRepository,
            SchulungsunterlageRepository unterlageRepository) {
        this.seminarRepository = seminarRepository;
        this.benutzerRepository = benutzerRepository;
        this.seminarService = seminarService;
        this.wetterService = wetterService;
        this.zertifikatRepository = zertifikatRepository;
        this.unterlageRepository = unterlageRepository;
        addClassName("seite");
        setSizeFull();

        suche = UiHilfe.suchfeld("Titel suchen …", t -> aktualisiere());
        HorizontalLayout toolbar = new HorizontalLayout(suche,
                UiHilfe.primaer("Online-Seminar", VaadinIcon.LAPTOP, () -> bearbeite(mitStandardwerten(new OnlineSeminar()))),
                UiHilfe.primaer("Präsenz-Seminar", VaadinIcon.BUILDING, () -> bearbeite(mitStandardwerten(new PraesenzSeminar()))));
        toolbar.addClassName("toolbar");

        grid.addComponentColumn(SeminareView::titelZelle).setHeader("Seminar")
                .setComparator(Comparator.comparing(Seminar::getTitel, String.CASE_INSENSITIVE_ORDER))
                .setWidth("240px").setFlexGrow(2);
        grid.addColumn(s -> s.getKursleiter().getVorname() + " " + s.getKursleiter().getNachname())
                .setHeader("Kursleiter").setComparator(s -> s.getKursleiter().getNachname()).setAutoWidth(true);
        Grid.Column<Seminar> zeitraum = grid
                .addColumn(s -> UiHilfe.datum(s.getStartDatum()) + " – " + UiHilfe.datum(s.getEndDatum()))
                .setHeader("Zeitraum").setComparator(Seminar::getStartDatum).setAutoWidth(true);
        grid.addColumn(SeminareView::ortOderPlattform).setHeader("Ort / Plattform")
                .setComparator(SeminareView::ortOderPlattform).setAutoWidth(true);
        grid.addComponentColumn(SeminareView::auslastung).setHeader("Auslastung")
                .setComparator(s -> (double) s.getAnzahlTeilnehmer() / s.getMaxTeilnehmer())
                .setWidth("170px").setFlexGrow(0);
        grid.addColumn(s -> UiHilfe.betrag(s.getPreis())).setHeader("Preis")
                .setComparator(Comparator.comparing(Seminar::getPreis, Comparator.nullsFirst(Comparator.naturalOrder())))
                .setAutoWidth(true);
        grid.addComponentColumn(s -> UiHilfe.zeilenAktionen(() -> bearbeite(s), () -> loesche(s)))
                .setWidth("110px").setFlexGrow(0);
        grid.addItemClickListener(e -> zeigeDetails(e.getItem()));
        grid.sort(GridSortOrder.asc(zeitraum).build());
        grid.setSizeFull();

        add(toolbar, grid);
        aktualisiere();
    }

    static Badge typBadge(Seminar seminar) {
        boolean online = seminar instanceof OnlineSeminar;
        Badge badge = new Badge(online ? "Online" : "Präsenz",
                (online ? VaadinIcon.LAPTOP : VaadinIcon.BUILDING).create());
        if (!online) {
            badge.addThemeVariants(BadgeVariant.SUCCESS);
        }
        return badge;
    }

    private static Component titelZelle(Seminar seminar) {
        Span titel = new Span(seminar.getTitel());
        titel.addClassName("zeile-titel");
        Div zelle = new Div(titel, typBadge(seminar));
        zelle.addClassName("zeile-kopf");
        if (seminar.isAusgebucht()) {
            Badge voll = new Badge("Ausgebucht");
            voll.addThemeVariants(BadgeVariant.ERROR);
            zelle.add(voll);
        }
        if (!seminar.getWarteliste().isEmpty()) {
            Badge warteliste = new Badge("Warteliste: " + seminar.getWarteliste().size());
            warteliste.addThemeVariants(BadgeVariant.WARNING);
            zelle.add(warteliste);
        }
        return zelle;
    }

    private static String ortOderPlattform(Seminar seminar) {
        return switch (seminar) {
            case PraesenzSeminar p -> p.getOrt() + (p.getRaum() == null ? "" : ", " + p.getRaum());
            case OnlineSeminar o -> o.getPlattform() == null ? "Online" : o.getPlattform();
            default -> "";
        };
    }

    private static Component auslastung(Seminar seminar) {
        ProgressBar balken = new ProgressBar(0, seminar.getMaxTeilnehmer(), seminar.getAnzahlTeilnehmer());
        Span text = new Span(seminar.getAnzahlTeilnehmer() + " / " + seminar.getMaxTeilnehmer());
        text.addClassName("gedimmt");
        Div zelle = new Div(balken, text);
        zelle.addClassName("auslastung");
        return zelle;
    }

    private static Seminar mitStandardwerten(Seminar seminar) {
        seminar.setMaxTeilnehmer(10);
        return seminar;
    }

    void aktualisiere() {
        String titel = suche.getValue();
        grid.setItems(titel.isBlank() ? seminarRepository.findAll()
                : seminarRepository.findByTitelContainingIgnoreCase(titel));
    }

    void bearbeite(Seminar seminar) {
        new SeminarFormDialog(seminar, benutzerRepository, s -> {
            seminarService.speichern(s);
            aktualisiere();
        }).open();
    }

    void loesche(Seminar seminar) {
        UiHilfe.loeschenBestaetigen(seminar.getTitel(), () -> {
            seminarRepository.deleteById(seminar.getId());
            aktualisiere();
        });
    }

    private void zeigeDetails(Seminar seminar) {
        new SeminarDetailDialog(seminar, this, seminarService, benutzerRepository, wetterService,
                zertifikatRepository, unterlageRepository).open();
    }
}
