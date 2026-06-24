package chikagebb.linktracker.bot.service;

import chikagebb.linktracker.bot.entity.ProcessedEvent;
import chikagebb.linktracker.bot.repository.ProcessedEventRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public boolean tryMarkProcessed(UUID eventId) {
        try {
            processedEventRepository.save(new ProcessedEvent(eventId, OffsetDateTime.now()));
            return true;
        } catch (DataIntegrityViolationException dup) {
            return false;
        }
    }
}
