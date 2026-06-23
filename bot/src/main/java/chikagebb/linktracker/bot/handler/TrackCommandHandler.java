package chikagebb.linktracker.bot.handler;

import chikagebb.linktracker.bot.client.ScrapperClient;
import chikagebb.linktracker.bot.command.TelegramCommand;
import chikagebb.linktracker.bot.service.UserStateService;
import chikagebb.linktracker.bot.state.TrackState;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrackCommandHandler implements UpdateHandler {

    private final ScrapperClient scrapperClient;
    private final UserStateService userStateService;

    private static final String CANCELLED_MESSAGE = """
        Отслеживание отменено ❌
        """;

    private static final String WAITING_URL_MESSAGE = """
            🔗 Отправь ссылку, которую хочешь отслеживать

            например: https://github.com
            """;

    private static final String INCORRECT_LINK_MESSAGE = """
            ❌ Некорректная ссылка

            Убедись, что ссылка начинается с http:// или https://
            Попробуй снова или нажми /cancel
            """;

    private static final String WAITING_TAGS_MESSAGE = """
            🏷 Введи теги через запятую
            например: работа, баг

            Или нажми /skip, чтобы пропустить
            """;

    private static final String LINK_ADDED_MESSAGE = "✅ Ссылка добавлена%n%n🔗 %s%n%s";

    @Override
    public boolean isSupport(Update update) {
        if (update.message() == null || update.message().text() == null) {
            return false;
        }

        Long chatId = update.message().chat().id();
        String text = update.message().text();
        TrackState state = userStateService.getState(chatId);

        if (state == TrackState.WAITING_TAG || state == TrackState.WAITING_URL) {
            return false;
        }

        return TelegramCommand.TRACK.isEnabled()
                && TelegramCommand.TRACK.getValue().equals(text);
    }

    @Override
    public void handle(Update update, TelegramBot bot) {
        Long chatId = update.message().chat().id();
        String text = update.message().text();
        TrackState state = userStateService.getState(chatId);

        if ("/cancel".equals(text)) {
            userStateService.clearState(chatId);
            bot.execute(new SendMessage(chatId, CANCELLED_MESSAGE));
            log.atInfo()
                    .setMessage("User cancelled the link tracking")
                    .addKeyValue("user_id", update.message().from().id())
                    .log();
        }

        switch (state) {
            case IDLE -> {
                userStateService.setStates(chatId, TrackState.WAITING_URL);
                bot.execute(new SendMessage(chatId, WAITING_URL_MESSAGE));
            }
            case WAITING_URL -> {
                if (!isValidUrl(text)) {
                    bot.execute(new SendMessage(chatId, INCORRECT_LINK_MESSAGE));
                    return;
                }
                userStateService.setPendingUrl(chatId, text);
                userStateService.setStates(chatId, TrackState.WAITING_TAG);
                bot.execute(new SendMessage(chatId, WAITING_TAGS_MESSAGE));
            }
            case WAITING_TAG -> {
                String url = userStateService.getPendingUrl(chatId);
                List<String> tags = "/skip".equals(text)
                        ? List.of()
                        : Arrays.stream(text.split(","))
                                .map(String::trim)
                                .filter(f -> !f.isEmpty())
                                .toList();

                try {
                    scrapperClient.addLink(chatId, url, tags);
                } catch (HttpClientErrorException.Conflict e) {
                    userStateService.clearState(chatId);
                    bot.execute(new SendMessage(chatId, "Ссылка уже отслеживается"));
                    return;
                }

                log.atInfo()
                        .setMessage("User link added")
                        .addKeyValue("chat_id", chatId)
                        .addKeyValue("url", url)
                        .addKeyValue("tags", tags)
                        .log();

                userStateService.clearState(chatId);
                bot.execute(new SendMessage(
                        chatId,
                        LINK_ADDED_MESSAGE.formatted(
                                url, tags.isEmpty() ? "" : "\n🏷 Теги: " + String.join(", ", tags))));
            }
            default -> {
                log.atWarn()
                        .setMessage("Unknown state")
                        .addKeyValue("state", state)
                        .log();
            }
        }
    }

    private boolean isValidUrl(String text) {
        if (text.startsWith("/")) return false;

        try {
            URL url = new URL(text);
            return url.getProtocol().equals("https") || url.getProtocol().equals("http");
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
