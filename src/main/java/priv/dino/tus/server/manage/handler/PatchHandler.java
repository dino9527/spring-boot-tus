package priv.dino.tus.server.manage.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import priv.dino.tus.server.core.configuration.properties.TusServerProperties;
import priv.dino.tus.server.core.error.GlobalException;
import priv.dino.tus.server.core.util.DigestUtils;
import priv.dino.tus.server.core.util.UploadExpiredUtils;
import priv.dino.tus.server.manage.service.UploadService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Optional;

import static priv.dino.tus.server.core.constant.TusConstant.*;
import static priv.dino.tus.server.core.util.Utils.*;

/**
 * The patch method handling that performs sanity checks on the request and delegates to a storage plugin.
 *
 * @author dino
 * @date 2021/11/16 09:06
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PatchHandler {

    private final UploadService uploadService;
    private final UploadExpiredUtils uploadExpiredUtils;
    private final TusServerProperties tusServerProperties;

    public Mono<ServerResponse> handleRequest(ServerRequest serverRequest) {
        Hooks.onOperatorDebug();
        log.info("In to PATCH Handler {}.", serverRequest);
        String uploadId = serverRequest.pathVariable("uploadId");
        Optional<String> contentType = getHeaderAsString("Content-Type", serverRequest);
        Optional<Long> offset = getHeaderAsLong("Upload-Offset", serverRequest);
        Optional<Long> contentLength = getHeaderAsLong("Content-Length", serverRequest);
        Optional<Tuple2<String, String>> checksumInfo = getHeaderAsChecksumInfo("Upload-Checksum", serverRequest);
        Flux<DataBuffer> parts = serverRequest.exchange().getRequest().getBody();

        if (checksumInfo.isPresent()) {
            String algorithms = checksumInfo.get().getT1().toLowerCase();
            boolean error = !tusServerProperties.getTusChecksumAlgorithms().contains(algorithms);
            String bodyStr = serverRequest.exchange().getAttribute("POST_BODY");
            if (!StringUtils.hasLength(bodyStr)) {
                throw new GlobalException(HttpStatus.INTERNAL_SERVER_ERROR, "The Body No Content.");
            }
            String encodeStr = DigestUtils.digestAsHex(algorithms,bodyStr);
            if (!checksumInfo.get().getT2().equals(encodeStr)) {
                error = true;
            }
            if (error) {
                return ServerResponse.status(460).build();
            }
        }

        boolean rogueRequest = !contentType.isPresent() || !CONTENT_TYPE.equals(contentType.get());

        if (!offset.isPresent() || offset.get() < 0) {
            rogueRequest = true;
        }

        if (!contentLength.isPresent()) {
            rogueRequest = true;
        }

        if (rogueRequest) {
            return ServerResponse.badRequest().build();
        }

        return uploadService.uploadChunkAndGetUpdatedOffset(Long.valueOf(uploadId),parts,offset.get(),contentLength.get())
                .log()
                .flatMap(r -> ServerResponse
                    .noContent()
                    .header(ACCESS_CONTROL_EXPOSE_HEADER, ACCESS_CONTROL_EXPOSE_POST_VALUE)
                    .header(UPLOAD_OFFSET_HEADER, Long.toString(r.getContentOffset()))
                    .header(TUS_RESUMABLE_HEADER, TUS_RESUMABLE_VALUE)
                    .header(UPLOAD_EXPIRES_HEADER, uploadExpiredUtils.getUploadExpiresHeaderValue(r))
                    .build()
                )
                .doOnNext(s -> log.info("{}", s.headers()));

    }
}
