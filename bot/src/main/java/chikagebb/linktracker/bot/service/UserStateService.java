package chikagebb.linktracker.bot.service;

import chikagebb.linktracker.bot.state.TrackState;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class UserStateService {

    private final Map<Long, TrackState> states = new ConcurrentHashMap<>();

    private final Map<Long, String> pendingsUrl = new ConcurrentHashMap<>();

    public TrackState getState(Long chatId) {
        return states.getOrDefault(chatId, TrackState.IDLE);
    }

    public void setStates(Long chatId, TrackState trackState) {
        states.put(chatId, trackState);
    }

    public void setPendingUrl(Long chatId, String url) {
        pendingsUrl.put(chatId, url);
    }

    public String getPendingUrl(Long chatId) {
        return pendingsUrl.get(chatId);
    }

    public void clearState(Long chatId) {
        states.remove(chatId);
        pendingsUrl.remove(chatId);
    }
}
