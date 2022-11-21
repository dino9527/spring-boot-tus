package priv.dino.tus.server.manage.router;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import priv.dino.tus.server.manage.handler.*;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

/**
 * download router
 *
 * @author dino
 * @date 2021/11/23 15:48
 */
@Configuration
@RequiredArgsConstructor
public class DownloadRouter {

    private final DownloadHandler downloadHandler;

    @RouterOperations({
            @RouterOperation(
                    path = "/download/{uploadId}",
                    beanClass = DownloadHandler.class,
                    beanMethod = "handle",
                    method = RequestMethod.GET,
                    operation = @Operation(operationId = "handle", summary = "The download file.", method = "GET",
                            responses = {
                                    @ApiResponse(responseCode = "404", description = "Download unit of work not found."),
                                    @ApiResponse(responseCode = "200", description = "Download unit of work found.")},
                            parameters = {@Parameter(in = ParameterIn.PATH, name = "uploadId",
                                    required = true, description = "The ID of the upload unit of work",
                                    schema = @Schema(type = "string", format = "SnowflakeID"))})
            )
    })
    @Bean
    public RouterFunction<ServerResponse> route() {
        return RouterFunctions
                        .route(GET("/download/{uploadId}").and(accept(MediaType.APPLICATION_JSON)), downloadHandler);
    }
}
