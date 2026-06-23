package chikagebb.linktracker.scrapper.service;

import chikagebb.linktracker.scrapper.repository.ChatRepository;
import chikagebb.linktracker.scrapper.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final LinkRepository linkRepository;

    @Transactional
    public void registry(Long chatId) {
        if (chatRepository.exists(chatId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Чат уже существует");
        }
        chatRepository.save(chatId);
    }

    @Transactional
    public void delete(Long chatId) {
        if (!chatRepository.exists(chatId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Чат не найден: " + chatId);
        }

        linkRepository.deleteAllForChat(chatId);
        chatRepository.remove(chatId);
    }
}
