package chikagebb.linktracker.scrapper.properties;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.scheduler")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScrapperProperties {

    @NotNull
    private Integer batchSize;
}
