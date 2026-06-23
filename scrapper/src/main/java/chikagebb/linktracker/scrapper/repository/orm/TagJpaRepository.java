package chikagebb.linktracker.scrapper.repository.orm;

import chikagebb.linktracker.scrapper.repository.orm.entity.Tag;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagJpaRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);
}
