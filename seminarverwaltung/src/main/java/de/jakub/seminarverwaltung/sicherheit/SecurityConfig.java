package de.jakub.seminarverwaltung.sicherheit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;

import de.jakub.seminarverwaltung.ui.LoginView;

/**
 * Oberfläche: Login-Formular mit Session. REST: HTTP Basic ohne Session und nur für Kursleiter.
 * Welche View wer sehen darf, steht als Annotation an den Views.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Ohne WWW-Authenticate-Header zeigt der Browser keinen Basic-Dialog und merkt sich keine Zugangsdaten
    private static final AuthenticationEntryPoint NICHT_ANGEMELDET = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
    // setStatus statt sendError, damit die Antwort nicht über /error in die Kette der Oberfläche läuft
    private static final AccessDeniedHandler KEIN_ZUGRIFF = (request, response, ex) -> response.setStatus(HttpStatus.FORBIDDEN.value());

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Aus der Oberfläche verlinkte Downloads, deshalb gilt hier auch die Session des Browsers. */
    @Bean
    @Order(1)
    SecurityFilterChain downloads(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/zertifikate/*/dokument", "/api/seminare/*/teilnehmerliste.csv")
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/seminare/*/teilnehmerliste.csv").hasRole("KURSLEITER")
                        // Ob das Zertifikat der angemeldeten Person gehört, prüft der ZertifikatController
                        .anyRequest().authenticated())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.NEVER));
        return alsRest(http).build();
    }

    /** STATELESS ignoriert das Session-Cookie. Ohne Cookie gibt es nichts, was CSRF ausnutzen könnte. */
    @Bean
    @Order(2)
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**")
                .authorizeHttpRequests(a -> a.anyRequest().hasRole("KURSLEITER"))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(c -> c.disable());
        return alsRest(http).build();
    }

    /** Die H2-Console nutzt Frames und eigene Formulare ohne CSRF-Token. H2 lässt sie nur von localhost zu. */
    @Bean
    @Order(3)
    SecurityFilterChain h2ConsoleSicherheit(HttpSecurity http) throws Exception {
        return http.securityMatcher("/h2-console/**")
                .authorizeHttpRequests(a -> a.anyRequest().hasRole("KURSLEITER"))
                .csrf(c -> c.disable())
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .exceptionHandling(e -> e.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
                .build();
    }

    @Bean
    @Order(4)
    SecurityFilterChain oberflaeche(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(a -> a.requestMatchers("/styles.css").permitAll())
                .with(VaadinSecurityConfigurer.vaadin(), vaadin -> vaadin.loginView(LoginView.class));
        return http.build();
    }

    private static HttpSecurity alsRest(HttpSecurity http) throws Exception {
        return http.httpBasic(b -> b.authenticationEntryPoint(NICHT_ANGEMELDET))
                .exceptionHandling(e -> e.authenticationEntryPoint(NICHT_ANGEMELDET).accessDeniedHandler(KEIN_ZUGRIFF));
    }
}
