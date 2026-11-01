package de.jakub.seminarverwaltung;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Jeder MockMvc-Request meldet sich wie ein echter Client per HTTP Basic als Kursleiter an. */
@TestConfiguration
class AlsKursleiter {

    @Bean
    MockMvcBuilderCustomizer kursleiterLogin() {
        return builder -> builder.defaultRequest(
                MockMvcRequestBuilders.get("/").with(httpBasic("anna.berger@example.com", "demo1234")));
    }
}
