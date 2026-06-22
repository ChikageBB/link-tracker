package chikagebb.linktracker.scrapper.repository;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("test")
@TestPropertySource(properties = "app.access-type=JOOQ")
public class JooqLinkRepositoryTest extends AbstractLinkRepositoryTest {}
