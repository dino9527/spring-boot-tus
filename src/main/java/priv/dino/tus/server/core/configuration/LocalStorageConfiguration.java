package priv.dino.tus.server.core.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import priv.dino.tus.server.core.error.GlobalException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.util.function.Function;

import static java.nio.file.StandardOpenOption.WRITE;

/**
 * 本地存储配置
 *
 * @author dino
 * @date 2021/11/11 09:23
 */
@Configuration
public class LocalStorageConfiguration {

    @Bean
    Function<Path, Mono<AsynchronousFileChannel>> channelFunction() {
        return path -> Mono.fromSupplier(() -> {
            try {
                return AsynchronousFileChannel.open(path, WRITE);
            } catch (IOException e) {
                throw new GlobalException(HttpStatus.INTERNAL_SERVER_ERROR, "File open operation fault",e);
            }
        });
    }
}
