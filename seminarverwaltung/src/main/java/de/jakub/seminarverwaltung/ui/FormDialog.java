package de.jakub.seminarverwaltung.ui;

import java.util.function.Consumer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.data.binder.Binder;

/** Dialog zum Anlegen und Bearbeiten: liest die Bean ins Formular und speichert sie, wenn alles gültig ist. */
class FormDialog<T> extends Dialog {

    FormDialog(String titel, Binder<T> binder, T bean, Component formular, Consumer<T> speichern) {
        setHeaderTitle(titel);
        setWidth("min(680px, 95vw)");
        binder.readBean(bean);
        add(formular);

        Button abbrechen = new Button("Abbrechen", e -> close());
        Button sichern = new Button("Speichern", e -> {
            if (!binder.writeBeanIfValid(bean)) {
                return;
            }
            try {
                speichern.accept(bean);
                UiHilfe.erfolg("Gespeichert");
                close();
            } catch (RuntimeException ex) {
                UiHilfe.fehler(UiHilfe.fehlertext(ex));
            }
        });
        sichern.addThemeVariants(ButtonVariant.PRIMARY);
        getFooter().add(abbrechen, sichern);
    }
}
