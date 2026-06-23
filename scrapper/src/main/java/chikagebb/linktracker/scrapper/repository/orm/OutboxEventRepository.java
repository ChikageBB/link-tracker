package chikagebb.linktracker.scrapper.repository.orm;

import chikagebb.linktracker.scrapper.repository.orm.entity.OutboxEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query(value = """
            SELECT * FROM outbox_events
            WHERE publishet_at IS NULL
                AND status = 'PENDING'
                AND attempts < :maxAttempts
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxEvent> findUnpublishedForUpdate(@Param("maxAttempts") int maxAttempts, @Param("limit") int limit);
}
