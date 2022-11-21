package priv.dino.tus.server.core.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static priv.dino.tus.server.core.constant.TusConstant.CONTENT_TYPE;

/**
 * Patch 请求的数据流处理
 *
 * @author dino
 * @date 2021/12/08 14:14
 */
@Component
@Slf4j
public class HttpPatchDataBufferFilter implements WebFilter, Ordered {


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String contentType = request.getHeaders().getFirst("Content-Type");
        if (CONTENT_TYPE.equals(contentType)) {
            return DataBufferUtils.join(exchange.getRequest().getBody())
                    .flatMap(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        String bodyString = new String(bytes, StandardCharsets.UTF_8);
                        log.info(bodyString);
                        exchange.getAttributes().put("POST_BODY", bodyString);
                        DataBufferUtils.release(dataBuffer);
                        /*Flux<DataBuffer> cachedFlux = Flux.defer(() -> {
                            DataBuffer buffer = exchange.getResponse().bufferFactory()
                                    .wrap(bytes);
                            return Mono.just(buffer);
                        });*/

                        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);

                        ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(
                                exchange.getRequest()) {
                            @Override
                            public Flux<DataBuffer> getBody() {
                                return Flux.just(buffer);
                            }
                        };
                        return chain.filter(exchange.mutate().request(mutatedRequest)
                                .build());
                    });
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -200;
    }

}
