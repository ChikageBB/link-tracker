package chikagebb.linktracker.scrapper.repository.orm;

import chikagebb.linktracker.scrapper.repository.orm.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatJpaRepository extends JpaRepository<Chat, Long> {

    void removeById(Long id);

    @Modifying
    @Query(value = "INSERT INTO chats (id) VALUES (:id)", nativeQuery = true)
    void insertChat(@Param("id") Long id);
}
