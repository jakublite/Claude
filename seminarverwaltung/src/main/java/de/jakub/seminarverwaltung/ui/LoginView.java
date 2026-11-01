package de.jakub.seminarverwaltung.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** Das Formular schickt per POST an /login, dort prüft Spring Security E-Mail und Passwort. */
@Route(value = "login", autoLayout = false)
@PageTitle("Anmelden")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm formular = new LoginForm();

    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        LoginI18n texte = LoginI18n.createDefault();
        texte.getForm().setTitle("Anmelden");
        texte.getForm().setUsername("E-Mail");
        texte.getForm().setPassword("Passwort");
        texte.getForm().setSubmit("Anmelden");
        texte.getErrorMessage().setTitle("Anmeldung fehlgeschlagen");
        texte.getErrorMessage().setMessage("E-Mail oder Passwort stimmen nicht.");
        formular.setI18n(texte);
        formular.setAction("login");
        formular.setForgotPasswordButtonVisible(false);

        Div marke = new Div(VaadinIcon.ACADEMY_CAP.create(), new Span("Seminarverwaltung"));
        marke.addClassName("marke");

        add(marke, formular);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        formular.setError(event.getLocation().getQueryParameters().getParameters().containsKey("error"));
    }
}
