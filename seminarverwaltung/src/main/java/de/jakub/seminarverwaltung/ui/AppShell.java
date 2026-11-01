package de.jakub.seminarverwaltung.ui;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.aura.Aura;

/** Aura ist das moderne Standard-Theme ab Vaadin 25, styles.css liegt unter META-INF/resources. */
@StyleSheet(Aura.STYLESHEET)
@StyleSheet("styles.css")
public class AppShell implements AppShellConfigurator {
}
