package chikagebb.linktracker.scrapper.repository.orm;

import chikagebb.linktracker.scrapper.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.access-type", havingValue = "ORM")
public class OrmChatRepository implements ChatRepository {

    private final ChatJpaRepository chatJpaRepository;

    @Override
    @Transactional
    public void save(Long chatId) {
        if (!chatJpaRepository.existsById(chatId)) {
            chatJpaRepository.insertChat(chatId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(Long chatId) {
        return chatJpaRepository.existsById(chatId);
    }

    @Override
    @Transactional
    public void remove(Long chatId) {
        chatJpaRepository.removeById(chatId);
    }
}
