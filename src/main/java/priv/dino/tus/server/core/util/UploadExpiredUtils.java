package priv.dino.tus.server.core.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import priv.dino.tus.server.core.configuration.properties.TusServerProperties;
import priv.dino.tus.server.manage.domain.File;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 上传过期时间工具类
 *
 * @author dino
 * @date 2021/11/20 20:57
 */
@Component
@RequiredArgsConstructor
public class UploadExpiredUtils {

    private final TusServerProperties tusServerProperties;

    public static final String PATTERN_DATETIME = "yyyy-MM-dd HH:mm:ss";

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(PATTERN_DATETIME);


    public boolean checkUploadExpired(File file) {
        return LocalDateTime.now().isAfter(this.getUploadExpires(file));
    }



    private LocalDateTime getUploadExpires(File file) {
        return file.getCreatedAt().plusDays(tusServerProperties.getDuration());
    }

    public String getUploadExpiresHeaderValue(File file) {
        return dateTimeFormatter.format(this.getUploadExpires(file));
    }
}
