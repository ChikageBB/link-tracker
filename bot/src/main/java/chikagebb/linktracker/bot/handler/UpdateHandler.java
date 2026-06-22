package chikagebb.linktracker.bot.handler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;

public interface UpdateHandler {

    boolean isSupport(Update update);

    void handle(Update update, TelegramBot bot);
}
