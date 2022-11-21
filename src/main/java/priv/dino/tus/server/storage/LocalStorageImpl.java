package priv.dino.tus.server.storage;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import priv.dino.tus.server.manage.domain.File;
import priv.dino.tus.server.core.error.GlobalException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

/**
 * 本地存储 实现类
 *
 * @author dino
 * @date 2021/10/29 15:24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalStorageImpl implements FileStorage {

    private final Path fileDirectory;
    private final Function<Path, Mono<AsynchronousFileChannel>> channelFunction;

    /**
     * 创建文件
     *
     * @param file 文件信息
     * @return reactor.core.publisher.Mono<priv.dino.tus.server.manage.domain.File>
     * @author dino
     * @date 2021/10/29 15:24
     */
    @Override
    public Mono<File> createFile(@NonNull File file) {
        return Mono.fromSupplier(() -> {
            try {
                Files.createFile(Paths.get(fileDirectory.toString(), file.getId().toString()));
                return file;
            } catch (IOException e) {
                throw new GlobalException(HttpStatus.INTERNAL_SERVER_ERROR, "File creation failed: " + file,e);
            }
        });
    }

    /**
     * 写文件
     *
     * @param id     文件id
     * @param parts  数据缓冲区
     * @param offset 偏移量
     * @return reactor.core.publisher.Mono<java.lang.Integer>
     * @author dino
     * @date 2021/11/9 17:15
     */
    @Override
    public Mono<Integer> writeChunk(@NonNull Long id, @NonNull Flux<DataBuffer> parts, long offset) {
        final Path file = Paths.get(fileDirectory.toString(), id.toString());
        final Mono<AsynchronousFileChannel> channel = channelFunction.apply(file);

        return channel
                .flatMapMany(asynchronousFileChannel -> DataBufferUtils.write(parts, asynchronousFileChannel, offset))
                .map(dataBuffer -> {
                    final int capacity = dataBuffer.capacity();
                    DataBufferUtils.release(dataBuffer);
                    return capacity;
                })
                .reduce(0, Integer::sum)
                 /*2021/12/14 Schedulers.boundedElastic() 调用一个有界的弹性线程池阻塞工作来解决警告：不建议在非阻塞上下文中调用“订阅”?????????*/
                .publishOn(Schedulers.boundedElastic())
                .doOnTerminate(() -> channel.subscribe(this::closeChannel));
    }

    void closeChannel( @NonNull final AsynchronousFileChannel asynchronousFileChannel){
        try {
            asynchronousFileChannel.close();
        } catch (IOException e) {
            throw new GlobalException(HttpStatus.INTERNAL_SERVER_ERROR, "Channel close error",e);
        }
    }

    /**
     * 合并文件
     *
     * @param id                      文件id
     * @param extractPartialUploadIds 合并请求的ID
     * @param offset                  偏移量
     * @return reactor.core.publisher.Mono<java.lang.Integer>
     * @author dino
     * @date 2021/11/24 10:52
     */
    @Override
    public Mono<Integer> mergeChunk(Long id, Long[] extractPartialUploadIds, long offset) {
        Flux<DataBuffer> parts = Flux.just(extractPartialUploadIds)
                .flatMapSequential(extractPartialUploadId -> DataBufferUtils.read(Paths.get(fileDirectory.toString(), extractPartialUploadId.toString()), new DefaultDataBufferFactory(), 4096));


        /*parts.subscribe(buffer -> {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            DataBufferUtils.release(buffer);
            String body = new String(bytes, UTF_8);
            System.out.println(body);

        });*/

        return writeChunk(id, parts, offset);
    }
}
