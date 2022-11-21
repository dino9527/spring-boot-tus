package priv.dino.tus.server.manage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import priv.dino.tus.server.core.configuration.properties.TusServerProperties;
import priv.dino.tus.server.core.util.UploadExpiredUtils;
import priv.dino.tus.server.manage.domain.File;
import priv.dino.tus.server.manage.repository.FileRepository;
import priv.dino.tus.server.manage.service.UploadService;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Stream;

import static org.springframework.http.HttpStatus.*;
import static priv.dino.tus.server.core.constant.TusConstant.*;


/**
 * 上传 控制器
 *
 * @author dino
 * @date 2021/10/29 10:33
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("upload")
@Slf4j
@Tag(name = "上传管理")
public class UploadController {

    private final FileRepository filesRepository;
    private final TusServerProperties tusServerProperties;
    private final UploadService uploadService;
    private final UploadExpiredUtils uploadExpiredUtils;

    @Operation(summary = "通过id获取文件信息。" )
    @GetMapping("/{id}")
    public Mono<ResponseEntity<File>> findById(@NonNull @PathVariable("id") Long id) {
        return filesRepository.findById(id)
                        .map(ResponseEntity::ok)
                        .defaultIfEmpty(ResponseEntity.notFound().build());

    }

    @Operation(summary = "提供有关 Tus.io 协议的服务器实现的信息。", method = "OPTIONS",
            responses = {
                    @ApiResponse(responseCode = "204", description = "服务器信息。",
                            headers = {@Header(name = TUS_VERSION_HEADER, description = "支持的 Tus.io 协议版本。", required = true),
                                    @Header(name = TUS_MAX_SIZE_HEADER, description = "服务器允许上传的最大长度。", required = true),
                                    @Header(name = TUS_EXTENTION_HEADER, description = "目前支持 Tus.io 扩展。", required = true),
                                    @Header(name = TUS_CHECKSUM_ALGORITHM_HEADER, description = "目前支持 Tus.io 校验和算法。", required = true)})})
    @RequestMapping(method = RequestMethod.OPTIONS)
    public Mono<ResponseEntity<Object>> processOptions() {
        log.debug("1 - OPTIONS:");
        log.debug("OPTIONS END");
        return Mono.just(ResponseEntity
                .status(NO_CONTENT)
                .header(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, ACCESS_CONTROL_ALLOW_ORIGIN_VALUE)
                .header(ACCESS_CONTROL_EXPOSE_HEADER, ACCESS_CONTROL_EXPOSE_OPTIONS_VALUE)
                .header(TUS_RESUMABLE_HEADER, TUS_RESUMABLE_VALUE)
                .header(TUS_VERSION_HEADER, TUS_VERSION_VALUE)
                .header(TUS_MAX_SIZE_HEADER, tusServerProperties.getMaxSize().toString())
                .header(TUS_EXTENTION_HEADER, TUS_EXTENTION_VALUE)
                .header(TUS_CHECKSUM_ALGORITHM_HEADER, tusServerProperties.getTusChecksumAlgorithms())
                .header(ACCESS_CONTROL_ALLOW_METHIDS_HEADER, ACCESS_CONTROL_ALLOW_METHIDS_VALUE)
                .build());
    }

    @Operation(summary = "创建一个新的上传工作单元.", method = "POST",
            parameters = {
                    @Parameter(name = "Upload-Length", in = ParameterIn.HEADER, required = true),
                    @Parameter(name = "Upload-Metadata", in = ParameterIn.HEADER, schema = @Schema(type = "string"), required = true),
                    @Parameter(name = "Mime-Type", in = ParameterIn.HEADER, schema = @Schema(type = "string"))},
            responses = {
                    @ApiResponse(responseCode = "413", description = "上传大小过大。"),
                    @ApiResponse(responseCode = "400", description = "错误的请求。"),
                    @ApiResponse(responseCode = "201", description = "上传工作单元已创建。",
                            headers = {@Header(name = "Location", description = "创建的上传工作单元的 uri。", required = true)})})
    @PostMapping
    public Mono<ResponseEntity<Object>> uploadStart(
            @NonNull @RequestHeader(name = "Upload-Length") final Long fileSize,
            @NonNull @RequestHeader(name = "Upload-Metadata") final String metadata,
            @RequestHeader(name = "Mime-Type", defaultValue = "") final String mimeType,
            @NonNull final UriComponentsBuilder uriComponentsBuilder,
            @NonNull final ServerHttpRequest request
    ) {
        request.getHeaders().forEach((k, v) -> log.debug("headers: {} {}", k, v));

        log.debug("2 - POST START");
        log.debug("Final-Length header value: " + fileSize);

        if(fileSize < 1){
            log.error("文件长度错误!");
            return Mono.just(ResponseEntity
                    .badRequest()
                    .build());
        }

        if(fileSize > tusServerProperties.getMaxSize()){
            log.error("上传文件过大!");
            return Mono.just(ResponseEntity
                    .status(PAYLOAD_TOO_LARGE)
                    .build());
        }

        final Map<String, String> parsedMetadata = uploadService.parseMetadata(metadata);
        final File file = File.builder()
                .mimeType(mimeType)
                .contentLength(fileSize)
                .originalName(parsedMetadata.getOrDefault("filename", "FILE NAME NOT EXISTS"))
                .contentOffset(0L)
                .lastUploadedChunkNumber(0L)
                .fingerprint(parsedMetadata.getOrDefault("fingerprint", "FINGERPRINT NAME NOT EXISTS"))
                .build();

        return uploadService
                .createUpload(file)
                .map(f -> Stream.concat(
                        request.getPath().elements().stream().map(PathContainer.Element::value),
                        Stream.of(f.getId().toString())
                ))
                .map(stringStream -> ResponseEntity
                        .created(uriComponentsBuilder.pathSegment(stringStream.filter(s -> !"/".equals(s)).toArray(String[]::new)).build().toUri())
                        .header(ACCESS_CONTROL_EXPOSE_HEADER, ACCESS_CONTROL_EXPOSE_POST_VALUE)
                        .header(TUS_RESUMABLE_HEADER, TUS_RESUMABLE_VALUE)
                        .build()
                )
                .doOnError(throwable -> log.error("Error on file create", throwable))
                .onErrorReturn(ResponseEntity
                        .status(INTERNAL_SERVER_ERROR)
                        .build()
                );
    }

    @Operation(summary = "提供特定上传的状态信息。", method = "HEAD",
            responses = {
                    @ApiResponse(responseCode = "404", description = "未找到上传工作单元。"),
                    @ApiResponse(responseCode = "200", description = "上传找到的工作单元。",
                            headers = {@Header(name = "Upload-Length", description = "上传工作单元的总长度。", required = true),
                                    @Header(name = "Upload-Offset", description = "到目前为止上传了多少字节。", required = true)})},
            parameters = {@Parameter(in = ParameterIn.PATH, name = "id",
                    required = true, description = "上传工作单元的ID", schema = @Schema(type = "string", format = "SnowflakeID"))})
    @RequestMapping(method = RequestMethod.HEAD, value = "/{id}")
    public Mono<ResponseEntity<Object>> header(@NonNull @PathVariable("id") final Long id) {
        log.debug("3 HEAD START");
        log.debug("id value: " + id);

        return filesRepository.findById(id).map(e ->
                        ResponseEntity
                                .status(OK)
                                .headers(new HttpHeaders())
                                .header(LOCATION_HEADER, e.getId().toString())
                                .header(CACHE_CONTROL_HEADER, CACHE_CONTROL_VALUE)
                                .header(UPLOAD_LENGTH_HEADER, e.getContentLength().toString())
                                .header(UPLOAD_OFFSET_HEADER, e.getContentOffset().toString())
                                .header(TUS_RESUMABLE_HEADER, TUS_RESUMABLE_VALUE)
                                .build())
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }


    @Operation(summary = "将字节添加到特定上传。", method = "PATCH",
            responses = {
                    @ApiResponse(responseCode = "400", description = "错误的请求。"),
                    @ApiResponse(responseCode = "423", description = "上传当前正在处理的工作单元。"),
                    @ApiResponse(responseCode = "409", description = "偏移不匹配。"),
                    @ApiResponse(responseCode = "404", description = "未找到上传工作单元。"),
                    @ApiResponse(responseCode = "204", description = "处理的字节数。",
                            headers = {@Header(name = "Upload-Offset", description = "到目前为止上传了多少字节。", required = true)})},
            parameters = {@Parameter(in = ParameterIn.PATH, name = "id",
                    required = true, description = "上传工作单元的ID", schema = @Schema(type = "string", format = "SnowflakeID")),
                    @Parameter(name = "Upload-Offset", in = ParameterIn.HEADER, required = true, schema = @Schema(type = "integer")),
                    @Parameter(name = "Content-Length", in = ParameterIn.HEADER, required = true, schema = @Schema(type = "integer")),
                    @Parameter(name = "Content-Type", example = "application/offset+octet-stream", required = true, schema = @Schema(type = "string"))})
    @RequestMapping(
            method = {RequestMethod.POST, RequestMethod.PATCH,},
            value = {"/{id}"},
            consumes = {"application/offset+octet-stream"}
    )
    public Mono<ResponseEntity<Object>> uploadProcess(
            @NonNull @PathVariable("id") final Long id,
            @NonNull final ServerHttpRequest request,
            @RequestHeader(name = "Upload-Offset") final long offset,
            @RequestHeader(name = "Content-Length") final long length
    ) {
        request.getHeaders().forEach((k, v) -> log.debug("headers: {} {}", k, v));

        log.debug("4 PATCH START");
        log.debug("SnowflakeID value: " + id);

        return uploadService
                .uploadChunkAndGetUpdatedOffset(id, request.getBody(), offset, length)
                .log()
                .map(e -> ResponseEntity
                        .status(NO_CONTENT)
                        .header(ACCESS_CONTROL_EXPOSE_HEADER, ACCESS_CONTROL_EXPOSE_POST_VALUE)
                        .header(UPLOAD_OFFSET_HEADER, Long.toString(e.getContentOffset()))
                        .header(TUS_RESUMABLE_HEADER, TUS_RESUMABLE_VALUE)
                        .header(UPLOAD_EXPIRES_HEADER, uploadExpiredUtils.getUploadExpiresHeaderValue(e))
                        .build()
                )
                .doOnNext(r -> log.info("{}", r.getHeaders()));
    }

    @Operation(summary = "删除特定的上传。", method = "DELETE",
            responses = {@ApiResponse(responseCode = "204", description = "请求已处理")},
            parameters = {@Parameter(in = ParameterIn.PATH, name = "id",
                    required = true, description = "上传工作单元的ID",
                    schema = @Schema(type = "string", format= "SnowflakeID"))})
    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    public Mono<Void> delete(@NonNull @PathVariable("id") final Long id) {
        log.debug("5 DELETE START");
        log.debug("id value: " + id);
        return filesRepository.deleteById(id);
    }


}
