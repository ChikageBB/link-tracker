package chikagebb.linktracker.bot.dispatcher;

import chikagebb.linktracker.bot.command.TelegramCommand;
import chikagebb.linktracker.bot.handler.UnknownCommandHandler;
import chikagebb.linktracker.bot.handler.UpdateHandler;
import chikagebb.linktracker.bot.service.UserStateService;
import chikagebb.linktracker.bot.state.TrackState;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramUpdateDispatcher {

    private final List<UpdateHandler> handlers;
    private final UserStateService userStateService;

    public void dispatch(Update update, TelegramBot bot) {

        Long chatId = update.message().chat().id();
        String text = update.message().text();
        TrackState state = userStateService.getState(chatId);

        boolean isDialog = state != TrackState.IDLE;

        if (isDialog
                && text != null
                && text.startsWith("/")
                && !text.equals("/cancel")
                && !text.startsWith(TelegramCommand.TRACK.getValue())
                && !text.startsWith(TelegramCommand.UNTRACK.getValue())) {
            userStateService.clearState(chatId);
        }

        handlers.stream()
                .filter(h -> !(h instanceof UnknownCommandHandler))
                .filter(h -> h.isSupport(update))
                .findFirst()
                .or(() -> handlers.stream()
                        .filter(h -> h instanceof UnknownCommandHandler)
                        .filter(h -> h.isSupport(update))
                        .findFirst())
                .ifPresent(h -> h.handle(update, bot));
    }
}
