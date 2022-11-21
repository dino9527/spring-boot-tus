package priv.dino.tus.server.manage.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import priv.dino.tus.server.core.configuration.properties.TusServerProperties;
import reactor.core.publisher.Mono;

import static priv.dino.tus.server.core.constant.TusConstant.*;
import static priv.dino.tus.server.core.constant.TusConstant.ACCESS_CONTROL_ALLOW_METHIDS_VALUE;

/**
 * The very simple options call that reveals Tus Server information.
 *
 * @author dino
 * @date 2021/11/15 13:37
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OptionsHandler {
    private final TusServerProperties tusServerProperties;

    public Mono<ServerResponse> handleRequest(ServerRequest serverRequest) {
        return ServerResponse.noContent()
                .header(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, ACCESS_CONTROL_ALLOW_ORIGIN_VALUE)
                .header(ACCESS_CONTROL_EXPOSE_HEADER, ACCESS_CONTROL_EXPOSE_OPTIONS_VALUE)
                .header(TUS_RESUMABLE_HEADER, TUS_RESUMABLE_VALUE)
                .header(TUS_VERSION_HEADER, TUS_VERSION_VALUE)
                .header(TUS_MAX_SIZE_HEADER, tusServerProperties.getMaxSize().toString())
                .header(TUS_EXTENTION_HEADER, TUS_EXTENTION_VALUE)
                .header(TUS_CHECKSUM_ALGORITHM_HEADER, tusServerProperties.getTusChecksumAlgorithms())
                .header(ACCESS_CONTROL_ALLOW_METHIDS_HEADER, ACCESS_CONTROL_ALLOW_METHIDS_VALUE)
                .build();
    }
}
