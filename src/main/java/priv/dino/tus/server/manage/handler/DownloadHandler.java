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
                .flatMap(e -> {
                    String originalFileName = e.getOriginalName();
                    String utf8EscapedFileName = escapeUtf8FileName(originalFileName);
                    String attachment = String.format("attachment; filename=\"%s\"; filename*=utf-8''\"%s\"", utf8EscapedFileName, utf8EscapedFileName);
                    return ServerResponse.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, attachment)
                                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                .body((p, a) -> p.writeWith(DataBufferUtils.read(Paths.get(fileDirectory.toString(), uploadId), new DefaultDataBufferFactory(), 4096)))
                                .doOnNext(a -> log.info("Download file ID: {}", uploadId));
                        }
                )
                .switchIfEmpty(ServerResponse.notFound().build());


    }

    private static String escapeUtf8FileName(String originalFileName) {
        byte[] utf8 = originalFileName.getBytes(StandardCharsets.UTF_8);
        StringBuilder strBuilder = new StringBuilder();
        for (byte b: utf8) {
            char ch = (char)b;
            if ((ch >= '0' && ch <='9') ||
                    (ch >= 'a' && ch <='z') ||
                    (ch >= 'A' && ch <='Z') ||
                    ch == '-' ||
                    ch == '_' ||
                    ch == '.'
            ) {
                strBuilder.append(ch);
                continue;
            } else if ((b >= 0x00 && b <= 0x1F) || b == 0x7F) {
                strBuilder.append('_');
                continue;
            }
            // RFC5987 规范: 对 URL 中的 UTF-8 文本进行转义编码
            strBuilder.append(String.format("%%%02X", b));
        }
        return strBuilder.toString();
    }
}
