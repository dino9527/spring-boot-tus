package priv.dino.tus.server.manage.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import priv.dino.tus.server.manage.repository.FileRepository;
import reactor.core.publisher.Mono;

import static priv.dino.tus.server.core.constant.TusConstant.TUS_RESUMABLE_HEADER;
import static priv.dino.tus.server.core.constant.TusConstant.TUS_RESUMABLE_VALUE;

/**
 * The termination extension handler.
 *
 * @author dino
 * @date 2021/11/15 08:55
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DeleteHandler {

    private final FileRepository fileRepository;

    public Mono<ServerResponse> handleRequest(ServerRequest serverRequest) {
        String uploadId = serverRequest.pathVariable("uploadId");

        return ServerResponse.status(HttpStatus.NO_CONTENT)
                .header(TUS_RESUMABLE_HEADER, TUS_RESUMABLE_VALUE)
                .build(fileRepository.deleteById(Long.valueOf(uploadId)).then());

    }
}
