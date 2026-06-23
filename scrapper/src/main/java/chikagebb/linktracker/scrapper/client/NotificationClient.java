package chikagebb.linktracker.scrapper.client;

import java.util.List;

public interface NotificationClient {
    void sendUpdate(Long id, String url, String description, List<Long> chatIds);
}
