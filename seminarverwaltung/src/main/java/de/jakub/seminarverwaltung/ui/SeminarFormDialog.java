package de.jakub.seminarverwaltung.ui;

import java.util.function.Consumer;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;

import de.jakub.seminarverwaltung.benutzer.Benutzer;
import de.jakub.seminarverwaltung.benutzer.BenutzerRepository;
import de.jakub.seminarverwaltung.benutzer.Rolle;
import de.jakub.seminarverwaltung.seminar.OnlineSeminar;
import de.jakub.seminarverwaltung.seminar.PraesenzSeminar;
import de.jakub.seminarverwaltung.seminar.Seminar;

/** Ein Formular für beide Seminararten: die typspezifischen Felder kommen je nach Unterklasse dazu. */
class SeminarFormDialog extends FormDialog<Seminar> {

    SeminarFormDialog(Seminar seminar, BenutzerRepository benutzerRepository, Consumer<Seminar> speichern) {
        this(seminar, new BeanValidationBinder<>(Seminar.class), benutzerRepository, speichern);
    }

    private SeminarFormDialog(Seminar seminar, BeanValidationBinder<Seminar> binder,
            BenutzerRepository benutzerRepository, Consumer<Seminar> speichern) {
        super(titel(seminar), binder, seminar, formular(seminar, binder, benutzerRepository), speichern);
    }

    private static String titel(Seminar seminar) {
        String art = seminar instanceof OnlineSeminar ? "Online-Seminar" : "Präsenz-Seminar";
        return (seminar.getId() == null ? "Neues " : "") + art + (seminar.getId() == null ? "" : " bearbeiten");
    }

    private static FormLayout formular(Seminar seminar, BeanValidationBinder<Seminar> binder,
            BenutzerRepository benutzerRepository) {
        TextField titel = new TextField("Titel");
        TextArea beschreibung = new TextArea("Beschreibung");
        DatePicker start = UiHilfe.datumsfeld("Start");
        DatePicker ende = UiHilfe.datumsfeld("Ende");
        IntegerField maxTeilnehmer = new IntegerField("Max. Teilnehmer");
        maxTeilnehmer.setStepButtonsVisible(true);
        maxTeilnehmer.setMin(1);
        BigDecimalField preis = new BigDecimalField("Preis (€)");
        ComboBox<Benutzer> kursleiter = new ComboBox<>("Kursleiter");
        kursleiter.setItems(benutzerRepository.findAll().stream().filter(b -> b.getRolle() == Rolle.KURSLEITER).toList());
        kursleiter.setItemLabelGenerator(b -> b.getVorname() + " " + b.getNachname());

        binder.forField(titel).bind("titel");
        binder.forField(beschreibung).bind("beschreibung");
        binder.forField(start).bind("startDatum");
        binder.forField(ende)
                .withValidator(e -> e == null || start.getValue() == null || !e.isBefore(start.getValue()),
                        "Ende darf nicht vor dem Start liegen")
                .bind("endDatum");
        binder.forField(maxTeilnehmer).asRequired("Pflichtfeld").bind("maxTeilnehmer");
        binder.forField(preis).bind("preis");
        binder.forField(kursleiter).bind("kursleiter");

        FormLayout formular = new FormLayout(titel, kursleiter, start, ende, maxTeilnehmer, preis);
        formular.setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("480px", 2));
        formular.setColspan(titel, 2);

        if (seminar instanceof OnlineSeminar) {
            TextField meetingLink = new TextField("Meeting-Link");
            meetingLink.setPlaceholder("https://…");
            TextField plattform = new TextField("Plattform");
            plattform.setPlaceholder("Teams, Zoom, Webex …");
            binder.forField(meetingLink)
                    .asRequired("Pflichtfeld")
                    .withValidator(l -> l.startsWith("http://") || l.startsWith("https://"), "Muss mit http(s):// beginnen")
                    .bind(s -> ((OnlineSeminar) s).getMeetingLink(), (s, v) -> ((OnlineSeminar) s).setMeetingLink(v));
            binder.forField(plattform)
                    .bind(s -> ((OnlineSeminar) s).getPlattform(), (s, v) -> ((OnlineSeminar) s).setPlattform(v));
            formular.add(meetingLink, plattform);
        } else {
            TextField ort = new TextField("Ort");
            ort.setHelperText("Stadt, wird für das Wetter genutzt");
            TextField raum = new TextField("Raum");
            binder.forField(ort)
                    .asRequired("Pflichtfeld")
                    .bind(s -> ((PraesenzSeminar) s).getOrt(), (s, v) -> ((PraesenzSeminar) s).setOrt(v));
            binder.forField(raum)
                    .bind(s -> ((PraesenzSeminar) s).getRaum(), (s, v) -> ((PraesenzSeminar) s).setRaum(v));
            formular.add(ort, raum);
        }

        formular.add(beschreibung);
        formular.setColspan(beschreibung, 2);
        return formular;
    }
}
