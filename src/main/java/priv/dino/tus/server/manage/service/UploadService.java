package priv.dino.tus.server.manage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import priv.dino.tus.server.core.util.UploadExpiredUtils;
import priv.dino.tus.server.manage.domain.File;
import priv.dino.tus.server.core.error.GlobalException;
import priv.dino.tus.server.manage.repository.FileRepository;
import priv.dino.tus.server.storage.FileStorage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 上传 业务层
 *
 * @author dino
 * @date 2021/10/29 15:18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {

    private final FileRepository fileRepository;
    private final FileStorage fileStorage;
    private final UploadExpiredUtils uploadExpiredUtils;

    public Map<String, String> parseMetadata(final String metadata) {

        return Arrays.stream(Optional.ofNullable(metadata).filter(StringUtils::isNotEmpty).orElseThrow(()-> new GlobalException(HttpStatus.INTERNAL_SERVER_ERROR, "Metadata No Content")).split(","))
                .map(v -> v.split(" "))
                .collect(Collectors.toMap(e -> e[0], e -> this.b64DecodeUnicode(e[1])));
    }

    private String b64DecodeUnicode(final String str) {
        final byte[] value;
        final String result;
        try {
            value = Base64.getDecoder().decode(str);
            result = new String(value, UTF_8);
        } catch (final IllegalArgumentException iae) {
            log.warn("Invalid encoding :'{}'", str);
            throw new GlobalException(HttpStatus.INTERNAL_SERVER_ERROR, String.format("Invalid encoding :'%s'", str));
        }
        return result;
    }

    public Mono<File> createUpload(final File file) {
       return fileRepository.save(file)
               .flatMap(fileStorage::createFile);
    }

    public Mono<File> mergePartialUploads(Long[] extractPartialUploadIds, Optional<String> metadataHeader, String mimeType) {
        final Map<String, String> parsedMetadata = this.parseMetadata(metadataHeader.orElse(""));
        return fileRepository.findAllById(Arrays.asList(extractPartialUploadIds))
                .switchIfEmpty(Mono.error(new GlobalException(HttpStatus.INTERNAL_SERVER_ERROR, "File record not found.")))
                .map(File::getContentLength)
                .reduce(0L, Long::sum)
                .flatMap(e-> {
                    final File file = File.builder()
                            .mimeType(mimeType)
                            .contentLength(e)
                            .originalName(parsedMetadata.getOrDefault("filename", "FILE NAME NOT EXISTS"))
                            .contentOffset(e)
                            .lastUploadedChunkNumber(0L)
                            .fingerprint(parsedMetadata.getOrDefault("fingerprint", "FINGERPRINT NAME NOT EXISTS"))
                            .partialStatus(false)
                            .build();
                    return this.createUpload(file);
                }).flatMap(file-> fileStorage.mergeChunk(file.getId(),extractPartialUploadIds,0L).map(number -> {
                    file.setContentOffset(number.longValue());
                    return file;
                }));

    }

    public Mono<File> uploadChunkAndGetUpdatedOffset(
            final Long id,
            final Flux<DataBuffer> parts,
            final long offset,
            final long length
    ) {

        Mono<File> fileOne = fileRepository.findById(id)
                .switchIfEmpty(Mono.error(new GlobalException(HttpStatus.INTERNAL_SERVER_ERROR, "File record not found.")))
                .map(e -> this.isValid(e, offset, length));
        return Mono
                .zip(fileOne,fileStorage.writeChunk(id, parts, offset))
                .flatMap((Tuple2<File, Integer> data) -> this.save(data.getT1(),data.getT2()));
    }

    public Mono<File> save(File file,Integer offset) {
        log.info("[OLD OFFSET] {}", file.getContentOffset());
        log.info("[OFFSET] {}", file.getContentOffset() + offset);
        file.setContentOffset(file.getContentOffset() + offset);
        file.setLastUploadedChunkNumber(file.getLastUploadedChunkNumber() + 1);
        log.debug("File patching: {}", file);
        return fileRepository.save(file);
    }

    private File isValid(File file, long offset, long length) {
        if (offset != file.getContentOffset() && checkContentLengthWithCurrentOffset(length, offset, file.getContentLength())) {
            throw new GlobalException(HttpStatus.INTERNAL_SERVER_ERROR, "Offset mismatch.");
        }
        if (uploadExpiredUtils.checkUploadExpired(file)) {
            throw new GlobalException(HttpStatus.INTERNAL_SERVER_ERROR, "Upload Expires.");
        }
        return file;
    }

    private boolean checkContentLengthWithCurrentOffset(long contentLength, long offset, long entityLength) {
        return contentLength + offset <= entityLength;
    }

}
