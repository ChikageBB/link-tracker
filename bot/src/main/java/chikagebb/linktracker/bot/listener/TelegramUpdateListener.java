package chikagebb.linktracker.bot.listener;

import chikagebb.linktracker.bot.dispatcher.TelegramUpdateDispatcher;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramUpdateListener {

    private final TelegramBot bot;
    private final TelegramUpdateDispatcher updateDispatcher;

    @PostConstruct
    public void init() {
        bot.setUpdatesListener(updates -> {
            for (Update update : updates) {

                if (update.message() == null || update.message().text() == null) {
                    log.atWarn()
                            .setMessage("Received update without message")
                            .addKeyValue("update", update.updateId())
                            .log();
                    continue;
                }

                log.atInfo()
                        .setMessage("Received command")
                        .addKeyValue("chat_id", update.message().chat().id())
                        .addKeyValue("text", update.message().text())
                        .log();

                try {
                    updateDispatcher.dispatch(update, bot);
                } catch (Exception e) {
                    log.atError()
                            .setMessage("Error processing update")
                            .addKeyValue("update", update)
                            .addKeyValue("message", e.getMessage())
                            .log();
                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }
}
