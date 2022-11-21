package priv.dino.tus.server.storage;

import org.springframework.core.io.buffer.DataBuffer;
import priv.dino.tus.server.manage.domain.File;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 文件存储
 *
 * @author dino
 * @date 2021/10/29 15:23
 */
public interface FileStorage {

    /**
     * 创建文件
     *
     * @param file 文件信息
     * @return reactor.core.publisher.Mono<priv.dino.tus.server.manage.domain.File>
     * @author dino
     * @date 2021/10/29 15:24
     */
    Mono<File> createFile(final File file);

    /**
     * 写文件
     *
     * @param id 文件id
     * @param parts 数据缓冲区
     * @param offset 偏移量
     * @return reactor.core.publisher.Mono<java.lang.Integer>
     * @author dino
     * @date 2021/11/9 17:15
     */
    Mono<Integer> writeChunk(final Long id, final Flux<DataBuffer> parts, final long offset);

    /**
     * 合并文件
     *
     * @param id 文件id
     * @param extractPartialUploadIds 合并请求的ID
     * @param offset 偏移量
     * @return reactor.core.publisher.Mono<java.lang.Integer>
     * @author dino
     * @date 2021/11/24 10:52
     */
    Mono<Integer> mergeChunk(final Long id, final Long[] extractPartialUploadIds, final long offset);
}
