package priv.dino.tus.server.manage.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import priv.dino.tus.server.core.configuration.properties.TusServerProperties;
import priv.dino.tus.server.manage.domain.File;
import priv.dino.tus.server.manage.service.UploadService;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

import static priv.dino.tus.server.core.constant.TusConstant.*;
import static priv.dino.tus.server.core.constant.TusConstant.TUS_RESUMABLE_VALUE;
import static priv.dino.tus.server.core.util.Utils.*;

/**
 * The creation and concatenation extension implementation.
 *
 * @author dino
 * @date 2021/11/15 14:13
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PostHandler {

    private final TusServerProperties tusServerProperties;
    private final UploadService uploadService;
    private static final String UPLOAD_CONCAT_PARTIAL = "partial";
    private static final String UPLOAD_CONCAT_FINAL = "final;";

    public Mono<ServerResponse> handleRequest(ServerRequest serverRequest) {
        Optional<Long> fileSizeHeader = getHeaderAsLong("Upload-Length", serverRequest);
        Optional<String> mimeTypeHeader = getHeaderAsString("Mime-Type", serverRequest);
        Optional<String> metadataHeader = getHeaderAsString("Upload-Metadata", serverRequest);
        Optional<String> concatHeader = getHeaderAsString("Upload-Concat", serverRequest);
        boolean isPartial = UPLOAD_CONCAT_PARTIAL.equals(concatHeader.orElse(""));
        boolean isPotentiallyFinal = concatHeader.orElse("").startsWith(UPLOAD_CONCAT_FINAL);

        if (isPotentiallyFinal) {
            String uploadConcat = concatHeader.orElse("Upload-Concat No Content ");
            log.info("Final Upload-Concat {}.", uploadConcat);
            String[] parts = uploadConcat.substring(UPLOAD_CONCAT_FINAL.length() + 1).split(" ");
            if (parts.length <= 1) {
                return ServerResponse.badRequest().build();
            }
            return uploadService.mergePartialUploads(extractPartialUploadIds(parts), metadataHeader,mimeTypeHeader.orElse(""))
                    .flatMap(stringStream -> ServerResponse
                            .created(serverRequest.uriBuilder().pathSegment(stringStream.getId().toString()).build())
                            .header(ACCESS_CONTROL_EXPOSE_HEADER, ACCESS_CONTROL_EXPOSE_POST_VALUE)
                            .header(TUS_RESUMABLE_HEADER, TUS_RESUMABLE_VALUE)
                            .build()
                    )
                    .doOnError(throwable -> log.error("Error on file create", throwable));

            
        } else if (fileSizeHeader.isPresent()) {
            if (this.checkServerSizeConstraint(fileSizeHeader.get())) {
                final Map<String, String> parsedMetadata = uploadService.parseMetadata(metadataHeader.orElse(""));
                final File file = File.builder()
                        .mimeType(mimeTypeHeader.orElse(""))
                        .contentLength(fileSizeHeader.get())
                        .originalName(parsedMetadata.getOrDefault("filename", "FILE NAME NOT EXISTS"))
                        .contentOffset(0L)
                        .lastUploadedChunkNumber(0L)
                        .fingerprint(parsedMetadata.getOrDefault("fingerprint", "FINGERPRINT NAME NOT EXISTS"))
                        .partialStatus(isPartial)
                        .build();
                return uploadService
                        .createUpload(file)
                        .flatMap(stringStream -> ServerResponse
                                .created(serverRequest.uriBuilder().pathSegment(stringStream.getId().toString()).build())
                                .header(ACCESS_CONTROL_EXPOSE_HEADER, ACCESS_CONTROL_EXPOSE_POST_VALUE)
                                .header(TUS_RESUMABLE_HEADER, TUS_RESUMABLE_VALUE)
                                .build()
                        )
                        .doOnError(throwable -> log.error("Error on file create", throwable));
            } else {
                return ServerResponse.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
            }
        } else {
            return ServerResponse.badRequest().build();
        }
    }

    public boolean checkServerSizeConstraint(Long totalLength) {
        return totalLength <= tusServerProperties.getMaxSize();
    }
}
