package chikagebb.linktracker.bot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import chikagebb.linktracker.bot.controller.UpdateController;
import chikagebb.linktracker.bot.initializer.TelegramCommandInitializer;
import chikagebb.linktracker.bot.listener.TelegramUpdateListener;
import com.pengrad.telegrambot.TelegramBot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UpdateController.class)
@DisplayName("Bot /updagte endpoint")
public class BotUpdateControllerTest {

    @MockitoBean
    TelegramBot telegramBot;

    @MockitoBean
    TelegramCommandInitializer telegramCommandInitializer;

    @MockitoBean
    TelegramUpdateListener telegramUpdateListener;

    @Autowired
    MockMvc mockMvc;

    @Nested
    @DisplayName("Позитивные сценарии")
    class PositiveScenarios {

        @Test
        @DisplayName("Корректный запрос с полными данными -> 200 OK")
        void validUpdateRequest() throws Exception {
            mockMvc.perform(post("/updates")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                        {
                            "id": 1,
                            "url": "https://github.com/spring-projects/spring-boot",
                            "description": "Новый коммит",
                            "tgChatIds": [123456789]
                        }
                        """))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Запрос с несколькими chatId -> 200 OK")
        void validUpdateRequestWithMultiplyChatIds() throws Exception {
            mockMvc.perform(post("/updates")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                    {
                      "id": 2,
                      "url": "https://stackoverflow.com/questions/12345",
                      "description": "Новый ответ",
                      "tgChatIds": [111, 222, 333]
                    }
                    """))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Негативные сценарии")
    class NegativeScenarios {

        @Test
        @DisplayName("Отсутствует обязательное поле url -> 400 Bad Request")
        void missingUrlField() throws Exception {
            mockMvc.perform(post("/updates")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                    {
                        "id": 1,
                        "description": "без url",
                        "tgChatIds": [123456789]
                    }
                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Отсутствует обязательное поле id -> 400 Bad Request")
        void missingIdField() throws Exception {
            mockMvc.perform(post("/updates")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                            {
                              "url": "https://github.com/spring-projects/spring-boot",
                              "description": "без id",
                              "tgChatIds": [123456789]
                            }
                            """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Пустой JSON объект → 400 Bad Request")
        void emptyJsonBody() throws Exception {
            mockMvc.perform(post("/updates")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Некорректный JSON → 400 Bad Request")
        void malformedJson() throws Exception {
            mockMvc.perform(post("/updates")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("не json вообще"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Пустой список chatIds → 400 Bad Request")
        void emptyChatIds() throws Exception {
            mockMvc.perform(post("/updates")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                            {
                              "id": 1,
                              "url": "https://github.com/spring-projects/spring-boot",
                              "description": "пустые chatIds",
                              "tgChatIds": []
                            }
                            """))
                    .andExpect(status().isBadRequest());
        }
    }
}
