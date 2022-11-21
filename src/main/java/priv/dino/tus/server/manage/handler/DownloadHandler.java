package priv.dino.tus.server.manage.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import priv.dino.tus.server.manage.repository.FileRepository;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * download file
 *
 * @author dino
 * @date 2021/11/23 14:33
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DownloadHandler implements HandlerFunction<ServerResponse> {
    private final FileRepository fileRepository;
    private final Path fileDirectory;

    @Override
    public Mono<ServerResponse> handle(ServerRequest request) {
        String uploadId = request.pathVariable("uploadId");

        return fileRepository.findById(Long.parseLong(uploadId))
                .flatMap(e ->
                        ServerResponse.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + new String(e.getOriginalName().getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1))
                                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                .body((p, a) -> p.writeWith(DataBufferUtils.read(Paths.get(fileDirectory.toString(), uploadId), new DefaultDataBufferFactory(), 4096)))
                                .doOnNext(a -> log.info("Download file ID: {}", uploadId))
                )
                .switchIfEmpty(ServerResponse.notFound().build());


    }

}
