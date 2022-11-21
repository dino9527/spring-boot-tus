package priv.dino.tus.server.manage.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import priv.dino.tus.server.manage.repository.FileRepository;
import reactor.core.publisher.Mono;


import static org.springframework.http.HttpStatus.OK;
import static priv.dino.tus.server.core.constant.TusConstant.*;

/**
 * Return all the information regarding an upload.
 *
 * @author dino
 * @date 2021/11/15 12:47
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class HeadHandler {
    private final FileRepository fileRepository;

    public Mono<ServerResponse> handleRequest(ServerRequest serverRequest) {
        String uploadId = serverRequest.pathVariable("uploadId");


        return fileRepository.findById(Long.valueOf(uploadId)).flatMap(e ->
                        ServerResponse
                                .status(OK)
                                .headers(HttpHeaders::new)
                                .header(LOCATION_HEADER, e.getId().toString())
                                .header(CACHE_CONTROL_HEADER, CACHE_CONTROL_VALUE)
                                .header(UPLOAD_LENGTH_HEADER, e.getContentLength().toString())
                                .header(UPLOAD_OFFSET_HEADER, e.getContentOffset().toString())
                                .header(TUS_RESUMABLE_HEADER, TUS_RESUMABLE_VALUE)
                                .build())
                .switchIfEmpty(ServerResponse.notFound().build());


    }
}
