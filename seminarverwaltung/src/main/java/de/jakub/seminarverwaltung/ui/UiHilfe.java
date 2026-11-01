package de.jakub.seminarverwaltung.ui;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

import jakarta.validation.ConstraintViolationException;

/** Kleine Helfer, die in allen Views gleich gebraucht werden. */
final class UiHilfe {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final NumberFormat EURO = NumberFormat.getCurrencyInstance(Locale.GERMANY);

    private UiHilfe() {
    }

    static String datum(LocalDate datum) {
        return datum == null ? "" : DATUM.format(datum);
    }

    static String betrag(BigDecimal betrag) {
        return betrag == null ? "" : EURO.format(betrag);
    }

    static DatePicker datumsfeld(String label) {
        DatePicker feld = new DatePicker(label);
        feld.setLocale(Locale.GERMANY);
        return feld;
    }

    static TextField suchfeld(String platzhalter, Consumer<String> beiEingabe) {
        TextField feld = new TextField();
        feld.setPlaceholder(platzhalter);
        feld.setPrefixComponent(VaadinIcon.SEARCH.create());
        feld.setClearButtonVisible(true);
        feld.setValueChangeMode(ValueChangeMode.LAZY);
        feld.addValueChangeListener(e -> beiEingabe.accept(e.getValue()));
        feld.setWidth("320px");
        return feld;
    }

    static Button primaer(String text, VaadinIcon icon, Runnable aktion) {
        Button button = new Button(text, icon.create(), e -> aktion.run());
        button.addThemeVariants(ButtonVariant.PRIMARY);
        return button;
    }

    /** Bearbeiten- und Löschen-Symbol für die letzte Grid-Spalte. */
    static HorizontalLayout zeilenAktionen(Runnable bearbeiten, Runnable loeschen) {
        Button edit = new Button(VaadinIcon.EDIT.create(), e -> bearbeiten.run());
        edit.addThemeVariants(ButtonVariant.TERTIARY);
        edit.setTooltipText("Bearbeiten");
        Button delete = new Button(VaadinIcon.TRASH.create(), e -> loeschen.run());
        delete.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.ERROR);
        delete.setTooltipText("Löschen");
        HorizontalLayout aktionen = new HorizontalLayout(edit, delete);
        aktionen.setSpacing(false);
        return aktionen;
    }

    static void loeschenBestaetigen(String was, Runnable loeschen) {
        ConfirmDialog dialog = new ConfirmDialog("Löschen?", "„" + was + "“ wird endgültig gelöscht.", "Löschen", e -> {
            try {
                loeschen.run();
                erfolg("„" + was + "“ gelöscht");
            } catch (DataIntegrityViolationException ex) {
                fehler("„" + was + "“ wird noch verwendet und kann nicht gelöscht werden");
            } catch (RuntimeException ex) {
                fehler(fehlertext(ex));
            }
        });
        dialog.setCancelable(true);
        dialog.setCancelText("Abbrechen");
        dialog.setConfirmButtonTheme("error primary");
        dialog.open();
    }

    static void erfolg(String text) {
        Notification.show(text, 3000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.SUCCESS);
    }

    static void fehler(String text) {
        Notification.show(text, 5000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.ERROR);
    }

    /** Macht aus technischen Exceptions eine lesbare Meldung für den Benutzer. */
    static String fehlertext(Throwable fehler) {
        for (Throwable t = fehler; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException cve) {
                return cve.getConstraintViolations().stream()
                        .map(v -> v.getMessage())
                        .collect(Collectors.joining(", "));
            }
            if (t instanceof ResponseStatusException rse) {
                return rse.getReason();
            }
            if (t instanceof DataIntegrityViolationException) {
                return "Ein Datensatz mit diesen Daten existiert bereits (z. B. gleiche E-Mail)";
            }
        }
        return "Unerwarteter Fehler: " + fehler.getMessage();
    }
}
