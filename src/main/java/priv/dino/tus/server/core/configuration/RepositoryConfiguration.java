package priv.dino.tus.server.core.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import reactor.core.publisher.Mono;

/**
 * R2DBC 审计
 *
 * @author dino
 * @date 2021/11/17 09:58
 */
@Configuration
@EnableR2dbcAuditing
public class RepositoryConfiguration {

    @Bean
    ReactiveAuditorAware<String> auditorAware() {
        return () -> Mono.just("dino");
    }

}
