package chikagebb.linktracker.scrapper.checker;

import chikagebb.linktracker.scrapper.dto.LinkDto;
import java.net.URI;
import java.util.Optional;

public interface LinkChecker {

    boolean support(URI url);

    Optional<String> check(LinkDto linkDto);
}
