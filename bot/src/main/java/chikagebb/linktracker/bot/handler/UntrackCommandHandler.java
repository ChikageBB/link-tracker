package chikagebb.linktracker.bot.handler;

import chikagebb.linktracker.bot.client.ScrapperClient;
import chikagebb.linktracker.bot.command.TelegramCommand;
import chikagebb.linktracker.bot.service.UserStateService;
import chikagebb.linktracker.bot.state.TrackState;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Component
@RequiredArgsConstructor
public class UntrackCommandHandler implements UpdateHandler {

    private final ScrapperClient scrapperClient;
    private final UserStateService userStateService;

    private static final String CANCELLED_MESSAGE = """
        Отслеживание отменено ❌
        """;

    private static final String ENTER_URL_MESSAGE = """
        🔗 Введи ссылку, которую хочешь удалить

        Или /cancel — отменить
        """;
    private static final String LINK_NOT_FOUND_MESSAGE = """
        ❌ Ссылка не найдена
        """;

    private static final String LINK_REMOVED_MESSAGE_TEMPLATE = "✅ Ссылка удалена%n%n🔗 %s";

    @Override
    public boolean isSupport(Update update) {
        if (update.message() == null || update.message().text() == null) {
            return false;
        }

        Long chatId = update.message().chat().id();
        String text = update.message().text();
        TrackState state = userStateService.getState(chatId);

        return TelegramCommand.UNTRACK.isEnabled()
                && (TelegramCommand.UNTRACK.getValue().equals(text) || state == TrackState.WAITING_URL);
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
                    .setMessage("User cancelled the link cancellation")
                    .addKeyValue("user_id", update.message().from().id())
                    .log();

            return;
        }

        switch (state) {
            case IDLE -> {
                userStateService.setStates(chatId, TrackState.UNTRACK_WAITING_URL);
                bot.execute(new SendMessage(chatId, ENTER_URL_MESSAGE));
            }
            case UNTRACK_WAITING_URL -> {
                try {
                    scrapperClient.removeLink(chatId, text);
                } catch (HttpClientErrorException.NotFound e) {
                    userStateService.clearState(chatId);
                    bot.execute(new SendMessage(chatId, LINK_NOT_FOUND_MESSAGE));
                    return;
                }

                log.atInfo()
                        .setMessage("User deleted link")
                        .addKeyValue("chat_id", chatId)
                        .addKeyValue("url", text)
                        .log();

                userStateService.clearState(chatId);
                bot.execute(new SendMessage(chatId, LINK_REMOVED_MESSAGE_TEMPLATE.formatted(text)));
            }
            default ->
                log.atWarn()
                        .setMessage("Unknown state")
                        .addKeyValue("state", state)
                        .log();
        }
    }
}
