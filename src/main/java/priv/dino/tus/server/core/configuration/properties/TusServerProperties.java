package priv.dino.tus.server.core.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * tus服务配置参数
 *
 * @author dino
 * @date 2021/10/29 14:18
 */
@Data
@Component
@ConfigurationProperties(prefix = "tus-server")
public class TusServerProperties {

    /**
     * 文件目录
     */
    private String fileDirectory;
    /**
     * Tus-Max-Size 响应头必须是非负整数，表示整个上传允许的最大字节大小。如果有已知的硬限制，服务器应该设置此头
     */
    private Long maxSize;

    /**
     * 校验和算法
     */
    private String tusChecksumAlgorithms;

    /**
     * tus 上传路径
     */
    private String contextPath;

    /**
     * 文件过期周期（单位：天）
     */
    private Long duration;
}
