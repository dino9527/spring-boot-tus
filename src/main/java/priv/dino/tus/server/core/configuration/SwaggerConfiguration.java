package priv.dino.tus.server.core.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * swagger 配置
 *
 * @author dino
 * @date 2021/10/29 14:39
 */
@Configuration
public class SwaggerConfiguration {

    @Bean
    public OpenAPI customOpenApi(@Value("${springdoc.version}") String appVersion) {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("basicScheme",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
                .info(new Info().title("Tus Server API").version(appVersion)
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")));
    }

    @Bean
    public GroupedOpenApi storeOpenApi() {
        String[] paths = { "/upload/**" };
        return GroupedOpenApi.builder().group("Upload Annotated controller").pathsToMatch(paths)
                .build();
    }

    @Bean
    public GroupedOpenApi storeTusOpenApi() {
        String[] paths = { "/tus/upload/**" };
        return GroupedOpenApi.builder().group("Upload Functional endpoint").pathsToMatch(paths)
                .build();
    }

    @Bean
    public GroupedOpenApi storeDownloadOpenApi() {
        String[] paths = { "/download/**" };
        return GroupedOpenApi.builder().group("Download Functional endpoint").pathsToMatch(paths)
                .build();
    }

    @Bean
    public GroupedOpenApi userOpenApi() {
        String[] paths = { "/stream/**" };
        String[] packagedToMatch = { "priv.dino.tus.server" };
        return GroupedOpenApi.builder().group("x-stream").pathsToMatch(paths).packagesToScan(packagedToMatch)
                .build();
    }
}
