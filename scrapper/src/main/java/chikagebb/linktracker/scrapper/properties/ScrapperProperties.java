package chikagebb.linktracker.scrapper.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    @NotNull(message = "Размер пакета (batchSize) должен быть указан")
    @Positive(message = "Размер пакета должен быть больше 0")
    private Integer batchSize;

    @NotEmpty(message = "Интервал запуска (interval) не должен быть пустым")
    private String interval;

    @NotNull(message = "Количество потов должно быть указано")
    @Min(value = 1, message = "Количество потоков должно быть не меньше 1")
    @Max(value = 200, message = "Количество потоков не должно превышать 200")
    private Integer threadCount;
}
