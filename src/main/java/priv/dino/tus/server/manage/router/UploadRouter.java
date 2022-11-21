package priv.dino.tus.server.manage.router;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
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
import priv.dino.tus.server.core.configuration.properties.TusServerProperties;
import priv.dino.tus.server.manage.handler.*;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

/**
 * The Tus Server with the route definitions.
 *
 * @author dino
 * @date 2021/11/15 08:54
 */
@Configuration
@RequiredArgsConstructor
public class UploadRouter {

    private final TusServerProperties tusServerProperties;

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/tus/upload/{uploadId}",
                    beanClass = DeleteHandler.class,
                    beanMethod = "handleRequest",
                    method = RequestMethod.DELETE,
                    operation = @Operation(operationId = "handleRequest", summary = "Deletes a specific upload.", method = "DELETE",
                            responses = {@ApiResponse(responseCode = "204", description = "Request processed")},
                            parameters = {@Parameter(in = ParameterIn.PATH, name = "uploadId",
                                    required = true, description = "The ID of the upload unit of work",
                                    schema = @Schema(type = "string", format= "SnowflakeID"))})
            ),
            @RouterOperation(
                    path = "/tus/upload/{uploadId}",
                    beanClass = HeadHandler.class,
                    beanMethod = "handleRequest",
                    method = RequestMethod.HEAD,
                    operation = @Operation(operationId = "handleRequest", summary = "Provides status information for a specific upload.", method = "HEAD",
                            responses = {
                                    @ApiResponse(responseCode = "404", description = "Upload unit of work not found."),
                                    @ApiResponse(responseCode = "200", description = "Upload unit of work found.",
                                            headers = {@Header(name = "Upload-Length", description = "The total length of the upload unit of work.", required = true),
                                                    @Header(name = "Upload-Offset", description = "How many bytes have been uploaded so far.", required = true)})},
                            parameters = {@Parameter(in = ParameterIn.PATH, name = "uploadId",
                                    required = true, description = "The ID of the upload unit of work", schema = @Schema(type = "string", format = "SnowflakeID"))})
            ),
            @RouterOperation(
                    path = "/tus/upload/",
                    beanClass = OptionsHandler.class,
                    beanMethod = "handleRequest",
                    method = RequestMethod.OPTIONS,
                    operation = @Operation(operationId = "handleRequest", summary = "Provides information about the server implementation of the Tus.io protocol.", method = "OPTIONS",
                            responses = {
                                    @ApiResponse(responseCode = "204", description = "Server Information.",
                                            headers = {@Header(name = "Tus-Version", description = "The versions of Tus.io protocol supported.", required = true),
                                                    @Header(name = "Tus-Max-Size", description = "The maximum length server allows to be uploaded.", required = true),
                                                    @Header(name = "Tus-Extension", description = "Tus.io extensions currently supported.", required = true),
                                                    @Header(name = "Tus-Checksum-Algorithm", description = "Tus.io checksum algorithms currently supported.", required = true)})})
            ),
            @RouterOperation(
                    path = "/tus/upload/",
                    beanClass = PostHandler.class,
                    beanMethod = "handleRequest",
                    method = RequestMethod.POST,
                    operation = @Operation(operationId = "handleRequest", summary = "Creates a new upload unit-of-work.", method = "POST",
                            parameters = {@Parameter(name = "Upload-Length", in = ParameterIn.HEADER, required = true),
                                    @Parameter(name = "Upload-Metadata", in = ParameterIn.HEADER, schema = @Schema(type = "string"), required = true),
                                    @Parameter(name = "Mime-Type", in = ParameterIn.HEADER, schema = @Schema(type = "string"))},
                            responses = {
                                    @ApiResponse(responseCode = "413", description = "Upload size too large."),
                                    @ApiResponse(responseCode = "400", description = "Bad Request."),
                                    @ApiResponse(responseCode = "201", description = "Upload unit of work Created.",
                                            headers = {@Header(name = "Location", description = "The uri of the created upload unit of work.", required = true)})})
            ),
            @RouterOperation(
                    path = "/tus/upload/{uploadId}",
                    beanClass = PatchHandler.class,
                    beanMethod = "handleRequest",
                    method = RequestMethod.PATCH,
                    operation = @Operation(operationId = "handleRequestForPatch", summary = "Adds bytes to a specific upload.", method = "PATCH",
                            responses = {
                                    @ApiResponse(responseCode = "400", description = "Bad Request."),
                                    @ApiResponse(responseCode = "423", description = "Upload unit of work currently in process."),
                                    @ApiResponse(responseCode = "409", description = "Offset mismatch."),
                                    @ApiResponse(responseCode = "404", description = "Upload unit of work not found."),
                                    @ApiResponse(responseCode = "460", description = "Checksum Mismatch."),
                                    @ApiResponse(responseCode = "204", description = "Bytes processed.",
                                            headers = {@Header(name = "Upload-Offset", description = "How many bytes have been uploaded so far.", required = true)})},
                            parameters = {@Parameter(in = ParameterIn.PATH, name = "uploadId",
                                    required = true, description = "The ID of the upload unit of work", schema = @Schema(type = "string", format = "SnowflakeID")),
                                    @Parameter(name = "Upload-Offset", in = ParameterIn.HEADER, required = true, schema = @Schema(type = "integer")),
                                    @Parameter(name = "Content-Length", in = ParameterIn.HEADER, required = true, schema = @Schema(type = "integer")),
                                    @Parameter(name = "Content-Type", example = "application/offset+octet-stream", required = true, schema = @Schema(type = "string"))})
            ),
            @RouterOperation(
                    path = "/tus/upload/{uploadId}",
                    beanClass = PatchHandler.class,
                    beanMethod = "handleRequest",
                    method = RequestMethod.POST,
                    operation = @Operation(operationId = "handleRequestForPost", summary = "Adds bytes to a specific upload.", method = "POST",
                            responses = {
                                    @ApiResponse(responseCode = "400", description = "Bad Request."),
                                    @ApiResponse(responseCode = "423", description = "Upload unit of work currently in process."),
                                    @ApiResponse(responseCode = "409", description = "Offset mismatch."),
                                    @ApiResponse(responseCode = "404", description = "Upload unit of work not found."),
                                    @ApiResponse(responseCode = "460", description = "Checksum Mismatch."),
                                    @ApiResponse(responseCode = "204", description = "Bytes processed.",
                                            headers = {@Header(name = "Upload-Offset", description = "How many bytes have been uploaded so far.", required = true)})},
                            parameters = {@Parameter(in = ParameterIn.PATH, name = "uploadId",
                                    required = true, description = "The ID of the upload unit of work", schema = @Schema(type = "string", format = "SnowflakeID")),
                                    @Parameter(name = "Upload-Offset", in = ParameterIn.HEADER, required = true, schema = @Schema(type = "integer")),
                                    @Parameter(name = "Content-Length", in = ParameterIn.HEADER, required = true, schema = @Schema(type = "integer")),
                                    @Parameter(name = "Content-Type", example = "application/offset+octet-stream", required = true, schema = @Schema(type = "string"))})
            )
    })
    public RouterFunction<ServerResponse> routeUpload(DeleteHandler deleteHandler,
                                                      HeadHandler handleRequest,
                                                      OptionsHandler optionsHandler,
                                                      PostHandler postHandler,
                                                      PatchHandler patchHandler) {
        return RouterFunctions.nest(
                path(tusServerProperties.getContextPath()),
                RouterFunctions
                        .route(DELETE("{uploadId}").and(accept(MediaType.APPLICATION_JSON)),deleteHandler::handleRequest)
                        .andRoute(HEAD("{uploadId}").and(accept(MediaType.APPLICATION_JSON)),handleRequest::handleRequest)
                        .andRoute(OPTIONS("").and(accept(MediaType.APPLICATION_JSON)),optionsHandler::handleRequest)
                        .andRoute(POST("").and(accept(MediaType.APPLICATION_JSON)),postHandler::handleRequest)
                        .andRoute(PATCH("{uploadId}").and(accept(MediaType.APPLICATION_JSON)),patchHandler::handleRequest)
                        //POST can replace PATCH because of buggy jre...
                        .andRoute(POST("{uploadId}").and(accept(MediaType.APPLICATION_JSON)),patchHandler::handleRequest));
    }
}
