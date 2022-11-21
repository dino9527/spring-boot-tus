package priv.dino.tus.server.core.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import priv.dino.tus.server.core.configuration.properties.TusServerProperties;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 服务配置文件
 *
 * @author dino
 * @date 2021/10/29 14:31
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
public class TusServerConfiguration {

    private final TusServerProperties tusServerProperties;

    @Bean
    public Path fileDirectory() throws IOException {
        String fileDirectory = tusServerProperties.getFileDirectory();
        final Path writeDirectoryPath = Paths.get(fileDirectory);

        log.debug("Files path: {}, created: {}", writeDirectoryPath, Files.exists(writeDirectoryPath));
        if (!Files.exists(writeDirectoryPath)) {
            Files.createDirectories(writeDirectoryPath);
        }

        if (!Files.isWritable(writeDirectoryPath)) {
            throw new AccessDeniedException(fileDirectory);
        }
        return writeDirectoryPath;
    }

}
