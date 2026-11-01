package de.jakub.seminarverwaltung.ui;

import java.time.LocalDate;
import java.util.Comparator;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.jakub.seminarverwaltung.benutzer.Benutzer;
import de.jakub.seminarverwaltung.benutzer.BenutzerRepository;
import de.jakub.seminarverwaltung.seminar.Seminar;
import de.jakub.seminarverwaltung.seminar.SeminarRepository;
import de.jakub.seminarverwaltung.sicherheit.Angemeldet;
import de.jakub.seminarverwaltung.zertifikat.Niveau;
import de.jakub.seminarverwaltung.zertifikat.Zertifikat;
import de.jakub.seminarverwaltung.zertifikat.ZertifikatRepository;
import de.jakub.seminarverwaltung.zertifikat.ZertifikatService;
import jakarta.annotation.security.PermitAll;

/** Teilnehmer sehen nur die eigenen Zertifikate und ändern nichts. */
@Route("zertifikate")
@PageTitle("Zertifikate")
@PermitAll
public class ZertifikateView extends VerticalLayout {

    private final ZertifikatRepository zertifikatRepository;
    private final ZertifikatService zertifikatService;
    private final SeminarRepository seminarRepository;
    private final BenutzerRepository benutzerRepository;
    private final Angemeldet angemeldet;
    private final Grid<Zertifikat> grid = new Grid<>();

    public ZertifikateView(ZertifikatRepository zertifikatRepository, ZertifikatService zertifikatService,
            SeminarRepository seminarRepository, BenutzerRepository benutzerRepository, Angemeldet angemeldet) {
        this.zertifikatRepository = zertifikatRepository;
        this.zertifikatService = zertifikatService;
        this.seminarRepository = seminarRepository;
        this.benutzerRepository = benutzerRepository;
        this.angemeldet = angemeldet;
        addClassName("seite");
        setSizeFull();

        ComboBox<Seminar> filter = new ComboBox<>();
        filter.setPlaceholder("Nach Seminar filtern …");
        filter.setItems(seminarRepository.findAll());
        filter.setItemLabelGenerator(Seminar::getTitel);
        filter.setClearButtonVisible(true);
        filter.setWidth("320px");
        filter.addValueChangeListener(e -> aktualisiere(e.getValue()));

        HorizontalLayout toolbar = new HorizontalLayout(filter);
        if (angemeldet.istKursleiter()) {
            toolbar.add(UiHilfe.primaer("Neues Zertifikat", VaadinIcon.PLUS, () -> bearbeite(neuesZertifikat())));
        }
        toolbar.addClassName("toolbar");

        grid.addColumn(z -> z.getSeminar().getTitel()).setHeader("Seminar")
                .setComparator(z -> z.getSeminar().getTitel()).setWidth("200px").setFlexGrow(2);
        grid.addColumn(z -> z.getTeilnehmer().getVorname() + " " + z.getTeilnehmer().getNachname())
                .setHeader("Teilnehmer").setComparator(z -> z.getTeilnehmer().getNachname()).setAutoWidth(true);
        grid.addComponentColumn(ZertifikateView::niveauBadge).setHeader("Niveau")
                .setComparator(Zertifikat::getNiveau).setAutoWidth(true);
        grid.addColumn(z -> UiHilfe.datum(z.getAusstellungsdatum())).setHeader("Ausgestellt")
                .setComparator(Zertifikat::getAusstellungsdatum).setAutoWidth(true);
        grid.addComponentColumn(ZertifikateView::gueltigkeit).setHeader("Gültig bis")
                .setComparator(Comparator.comparing(Zertifikat::getGueltigBis, Comparator.nullsLast(Comparator.naturalOrder())))
                .setAutoWidth(true);
        grid.addComponentColumn(ZertifikateView::dokumentLink).setWidth("60px").setFlexGrow(0);
        if (angemeldet.istKursleiter()) {
            grid.addComponentColumn(z -> UiHilfe.zeilenAktionen(() -> bearbeite(z), () -> loesche(z)))
                    .setWidth("110px").setFlexGrow(0);
        }
        grid.setSizeFull();

        add(toolbar, grid);
        aktualisiere(null);
    }

    /** Öffnet das druckbare Zertifikat in einem neuen Tab. */
    static Anchor dokumentLink(Zertifikat zertifikat) {
        Anchor link = new Anchor("api/zertifikate/" + zertifikat.getId() + "/dokument", VaadinIcon.PRINT.create());
        link.setTarget(AnchorTarget.BLANK);
        link.setTitle("Zertifikat drucken");
        return link;
    }

    private static Badge niveauBadge(Zertifikat zertifikat) {
        Badge badge = new Badge("Niveau " + zertifikat.getNiveau());
        badge.addThemeVariants(switch (zertifikat.getNiveau()) {
            case A -> BadgeVariant.SUCCESS;
            case B -> BadgeVariant.CONTRAST;
            case C -> BadgeVariant.WARNING;
        });
        return badge;
    }

    private static Component gueltigkeit(Zertifikat zertifikat) {
        if (zertifikat.getGueltigBis() == null) {
            return new Span("unbegrenzt");
        }
        if (zertifikat.getGueltigBis().isBefore(LocalDate.now())) {
            Badge abgelaufen = new Badge("abgelaufen " + UiHilfe.datum(zertifikat.getGueltigBis()));
            abgelaufen.addThemeVariants(BadgeVariant.ERROR);
            return abgelaufen;
        }
        return new Span(UiHilfe.datum(zertifikat.getGueltigBis()));
    }

    private void aktualisiere(Seminar seminar) {
        grid.setItems((seminar == null ? zertifikatRepository.findAll()
                : zertifikatRepository.findBySeminarId(seminar.getId())).stream()
                .filter(z -> angemeldet.darfSehen(z.getTeilnehmer()))
                .toList());
    }

    private Zertifikat neuesZertifikat() {
        Zertifikat zertifikat = new Zertifikat();
        zertifikat.setAusstellungsdatum(LocalDate.now());
        return zertifikat;
    }

    private void bearbeite(Zertifikat zertifikat) {
        ComboBox<Seminar> seminar = new ComboBox<>("Seminar");
        seminar.setItems(seminarRepository.findAll());
        seminar.setItemLabelGenerator(Seminar::getTitel);
        ComboBox<Benutzer> teilnehmer = new ComboBox<>("Teilnehmer");
        teilnehmer.setItems(benutzerRepository.findAll());
        teilnehmer.setItemLabelGenerator(b -> b.getVorname() + " " + b.getNachname());
        RadioButtonGroup<Niveau> niveau = new RadioButtonGroup<>("Niveau");
        niveau.setItems(Niveau.values());
        DatePicker ausgestellt = UiHilfe.datumsfeld("Ausstellungsdatum");
        DatePicker gueltigBis = UiHilfe.datumsfeld("Gültig bis");
        gueltigBis.setHelperText("leer = unbegrenzt");

        BeanValidationBinder<Zertifikat> binder = new BeanValidationBinder<>(Zertifikat.class);
        binder.forField(seminar).bind("seminar");
        binder.forField(teilnehmer).bind("teilnehmer");
        binder.forField(niveau).bind("niveau");
        binder.forField(ausgestellt).bind("ausstellungsdatum");
        binder.forField(gueltigBis).bind("gueltigBis");

        FormLayout formular = new FormLayout(seminar, teilnehmer, niveau, ausgestellt, gueltigBis);
        formular.setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("480px", 2));
        formular.setColspan(seminar, 2);

        String titel = zertifikat.getId() == null ? "Neues Zertifikat" : "Zertifikat bearbeiten";
        new FormDialog<>(titel, binder, zertifikat, formular, z -> {
            if (z.getId() == null) {
                zertifikatService.ausstellen(z);
            } else {
                zertifikatRepository.save(z);
            }
            aktualisiere(null);
        }).open();
    }

    private void loesche(Zertifikat zertifikat) {
        UiHilfe.loeschenBestaetigen("Zertifikat " + zertifikat.getSeminar().getTitel(), () -> {
            zertifikatRepository.deleteById(zertifikat.getId());
            aktualisiere(null);
        });
    }
}
