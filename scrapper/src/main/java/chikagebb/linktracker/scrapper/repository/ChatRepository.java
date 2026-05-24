package chikagebb.linktracker.scrapper.repository;

public interface ChatRepository {

    void save(Long chatId);

    boolean exists(Long chatId);

    void remove(Long chatId);
}
