package chikagebb.linktracker.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import chikagebb.linktracker.bot.client.ScrapperClient;
import chikagebb.linktracker.bot.dispatcher.TelegramUpdateDispatcher;
import chikagebb.linktracker.bot.handler.HelpCommandHandler;
import chikagebb.linktracker.bot.handler.ListCommandHandler;
import chikagebb.linktracker.bot.handler.StartCommandHandler;
import chikagebb.linktracker.bot.handler.TrackCommandHandler;
import chikagebb.linktracker.bot.handler.UnknownCommandHandler;
import chikagebb.linktracker.bot.handler.UntrackCommandHandler;
import chikagebb.linktracker.bot.listener.TelegramUpdateListener;
import chikagebb.linktracker.bot.service.UserStateService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TelegramUpdateListenerBotTest {

    private static final long CHAT_ID = 987_654_321L;
    private static final long USER_ID = 123456L;

    @Mock
    private TelegramBot telegramBot;

    @Mock
    private ScrapperClient scrapperClient;

    private TelegramUpdateListener listener;

    @BeforeEach
    void setUp() {
        var userStateService = new UserStateService();
        var handlers = List.of(
                new StartCommandHandler(scrapperClient),
                new HelpCommandHandler(),
                new TrackCommandHandler(scrapperClient, userStateService),
                new UntrackCommandHandler(scrapperClient, userStateService),
                new ListCommandHandler(scrapperClient),
                new UnknownCommandHandler());
        var dispatcher = new TelegramUpdateDispatcher(handlers, userStateService);
        listener = new TelegramUpdateListener(telegramBot, dispatcher);
    }

    /**
     * Захватывает SendMessage и возвращает сообщения
     */
    private String captureResponseText() {
        var captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot).execute(captor.capture());
        var params = captor.getValue().getParameters();
        return (String) params.get("text");
    }

    /**
     * Строим мок Update с заданным текстом
     */
    private Update buildUpdate(String text) {
        var chat = mock(Chat.class);
        when(chat.id()).thenReturn(CHAT_ID);

        var user = mock(User.class);
        when(user.id()).thenReturn(USER_ID);

        var message = mock(Message.class);
        when(message.text()).thenReturn(text);
        when(message.chat()).thenReturn(chat);
        when(message.from()).thenReturn(user);

        var update = mock(Update.class);
        when(update.message()).thenReturn(message);
        return update;
    }

    /**
     * Строим мок Update без message
     */
    private Update buildEmptyUpdate() {
        var update = mock(Update.class);
        when(update.message()).thenReturn(null);
        when(update.updateId()).thenReturn(52);
        return update;
    }

    private Update buildUpdateWithoutText() {
        var message = mock(Message.class);
        when(message.text()).thenReturn(null);

        var update = mock(Update.class);
        when(update.message()).thenReturn(message);
        when(update.updateId()).thenReturn(99);
        return update;
    }

    private UpdatesListener captureUpdatesListener() {
        listener.init();
        var captor = ArgumentCaptor.forClass(UpdatesListener.class);
        verify(telegramBot).setUpdatesListener(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("Позитивные сценарии")
    class PositiveScenarios {

        @Test
        @DisplayName("/start -> бот отвечает приветственным сообщением")
        void startCommandReturnGreeting() {

            var updateListener = captureUpdatesListener();

            updateListener.process(List.of(buildUpdate("/start")));

            String responseText = captureResponseText();
            assertThat(responseText)
                    .as("Ответ на /start должен содержать приветствие")
                    .isNotBlank()
                    .contains("Привет");
        }

        @Test
        @DisplayName("/help -> бот отвечает списком доступных команд")
        void helpCommandReturnCommandList() {
            var updateListener = captureUpdatesListener();

            updateListener.process(List.of(buildUpdate("/help")));

            String responseText = captureResponseText();

            assertThat(responseText)
                    .as("Ответ на /help должен содержать список команд")
                    .isNotBlank()
                    .contains("/start", "/help");
        }

        @Test
        @DisplayName("/start и /help отправляют ответ пользователю")
        void startAndHelpCommandAlwaysSendResponse() {
            var updateListener = captureUpdatesListener();

            updateListener.process(List.of(buildUpdate("/start"), buildUpdate("/help")));

            verify(telegramBot, times(2)).execute(any(SendMessage.class));
        }

        @Test
        @DisplayName("Ответы отправляются в тот же чат, из которого пришла команда")
        void responseSendToCorrectChat() {

            var updateListener = captureUpdatesListener();
            updateListener.process(List.of(buildUpdate("/start")));

            var captor = ArgumentCaptor.forClass(SendMessage.class);
            verify(telegramBot).execute(captor.capture());

            var params = captor.getValue().getParameters();

            assertThat(params.get("chat_id"))
                    .as("chat_id в ответе должен совпадать с chat_id отправителя")
                    .isEqualTo(CHAT_ID);
        }
    }

    @Nested
    @DisplayName("Негативные сценарии")
    class NegativeScenarios {

        @Test
        @DisplayName("Неизвестная команда -> бот отвечает сообщением об ошибке")
        void unknownCommandReturnErrorMessage() {

            var updateListener = captureUpdatesListener();

            updateListener.process(List.of(buildUpdate("/unknown")));

            String responseText = captureResponseText();
            assertThat(responseText)
                    .as("Ответ на неизвестную команду должен содержать подсказку /help")
                    .isNotBlank()
                    .containsIgnoringCase("help")
                    .contains("/help");
        }

        @Test
        @DisplayName("Произвольный текст -> бот отвечает сообщением об ошибке")
        void arbitraryTextReturnErrorMessage() {

            var updateListener = captureUpdatesListener();

            updateListener.process(List.of(buildUpdate("Привет")));

            String responseText = captureResponseText();
            AssertionsForClassTypes.assertThat(responseText)
                    .as("На произвольный текст бот должен сообщить о неизвестной команде")
                    .isNotBlank();
        }

        @Test
        @DisplayName("Update без message -> бот не отправляет ответ")
        void updateWithoutMessageNoResponseSent() {

            var updateListener = captureUpdatesListener();

            updateListener.process(java.util.List.of(buildEmptyUpdate()));

            verify(telegramBot, never()).execute(any(SendMessage.class));
        }

        @Test
        @DisplayName("Update с message без text -> бот не отправляет ответ")
        void updateWithoutTextNoResponseSent() {

            var updateListener = captureUpdatesListener();

            updateListener.process(java.util.List.of(buildUpdateWithoutText()));

            verify(telegramBot, never()).execute(any(SendMessage.class));
        }
    }
}
