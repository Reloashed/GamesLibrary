package ch.diethelm.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class GameApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAllGames_returnsNonEmptyList() throws Exception {
        mockMvc.perform(get("/api/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    void searchByTitle_existingTitle_returnsSingleMatch() throws Exception {
        mockMvc.perform(get("/api/games/search").param("title", "witcher 3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void searchByTitle_unknownTitle_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/games/search").param("title", "NichtExistierendesSpielXYZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getGameById_unknownId_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/games/999999"))
                .andExpect(status().isNotFound());
    }
}
