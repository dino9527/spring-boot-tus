package priv.dino.tus.server.manage.controllers;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import priv.dino.tus.server.core.configuration.properties.TusServerProperties;
import priv.dino.tus.server.core.util.UploadExpiredUtils;
import priv.dino.tus.server.manage.controller.UploadController;
import priv.dino.tus.server.manage.domain.File;
import priv.dino.tus.server.manage.repository.FileRepository;
import priv.dino.tus.server.manage.service.UploadService;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.NO_CONTENT;
@WebFluxTest(value = {UploadController.class,TusServerProperties.class})
class UploadControllerTest {

    @Autowired
    private WebTestClient webClient;
    @MockBean
    UploadService uploadService;
    @MockBean
    FileRepository filesRepository;
    @InjectMocks
    @Autowired
    TusServerProperties tusServerProperties;
    @MockBean
    ServerHttpRequest request;
    @MockBean
    UploadExpiredUtils uploadExpiredUtils;


    @Test
    void getFileInfo() {
        Mockito
            .when(filesRepository.findById(1L))
            .thenReturn(Mono.just(File.builder().id(1L).build()));
        webClient.get().uri("/upload/1").exchange()
            .expectStatus()
            .isOk()
            .expectBody(File.class);
    }


    @Test
    void uploadStart() {
        Mockito
            .when(uploadService.parseMetadata("testMetadata"))
            .thenReturn(new HashMap<String, String>(){{
                put("filename", "metadata");
                put("fingerprint", "test-fing");
            }});

        final File build = File.builder()
                .id(1L)
                .mimeType("plain/text")
                .contentLength(100L)
                .originalName("metadata")
                .contentOffset(0L)
                .lastUploadedChunkNumber(0L)
                .fingerprint("test-fing")
                .build();

        Mockito
            .when(uploadService.createUpload(build))
            .thenReturn(Mono.just(File.builder()
                .id(1L)
                .mimeType("plain/text")
                .contentLength(100L)
                .originalName("metadata")
                .contentOffset(0L)
                .lastUploadedChunkNumber(0L)
                .fingerprint("test-fing")
                .build()));

        webClient
            .post()
            .uri("/upload")
            .header("Upload-Length", "100")
            .header("Upload-Metadata", "testMetadata")
            .header("Mime-Type", "plain/text")
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().exists("Location");
    }

    @Test
    void uploadStart_doOnError() {
        Mockito
            .when(uploadService.parseMetadata("testMetadata"))
            .thenReturn(new HashMap<String, String>(){{
                put("filename", "metadata");
            }});

        final File metadata = File.builder()
                .id(1L)
                .mimeType("plain/text")
                .contentLength(100L)
                .originalName("metadata")
                .contentOffset(0L)
                .lastUploadedChunkNumber(0L)
                .build();
        Mockito
            .when(uploadService.createUpload(metadata))
            .thenReturn(Mono.error(new Exception()));

        webClient
            .post()
            .uri("/upload")
            .header("Upload-Length", "100")
            .header("Upload-Metadata", "testMetadata")
            .header("Mime-Type", "plain/text")
            .exchange()
            .expectStatus().is5xxServerError();
    }




    @Test
    void uploadProcess() {
        DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
        DefaultDataBuffer dataBuffer =
            factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
        Flux<DataBuffer> body = Flux.just(dataBuffer);

        Mockito.when(request.getBody())
            .thenReturn(body);
        Mockito.when(request.getHeaders())
            .thenReturn(new HttpHeaders(){{
                put("test", Collections.singletonList("test"));
            }});
        Mockito
            .when(uploadService.uploadChunkAndGetUpdatedOffset(1L, body, 0, 3))
            .thenReturn(Mono.just(File.builder().contentOffset(3L).build()));


        final UploadController uploadController = new UploadController(filesRepository, tusServerProperties, uploadService, uploadExpiredUtils);
        uploadController.uploadProcess(1L, request, 0, 3)
            .subscribe(v -> {
                assertEquals(NO_CONTENT, v.getStatusCode());
                assertEquals("3", Objects.requireNonNull(v.getHeaders().get("Upload-Offset")).get(0));
            });
    }


    @Test
    void header() {
        Mockito
            .when(filesRepository.findById(1L))
            .thenReturn(Mono.just(File.builder().id(1L)
                .contentLength(100L)
                .contentOffset(0L).build()));
        webClient
            .head()
            .uri("/upload/1")
            .exchange()
            .expectStatus().isNoContent()
            .expectHeader().exists("Upload-Length")
            .expectHeader().exists("Upload-Offset")
            .expectHeader().exists("Cache-Control")
            .expectHeader().exists("Location")
        ;
    }

    @Test
    void header_notFound() {
        Mockito
            .when(filesRepository.findById(1L))
            .thenReturn(Mono.empty());
        webClient
            .head()
            .uri("/upload/1")
            .exchange()
            .expectStatus().isNotFound()
        ;
    }

    @Test
    void processOptions() {
        webClient
            .options()
            .uri("/upload")
            .exchange()
            .expectStatus().isNoContent()
            .expectHeader().exists("Tus-Version")
            .expectHeader().exists("Tus-Resumable")
            .expectHeader().exists("Access-Control-Expose-Headers")
            .expectHeader().exists("Tus-Extension")
            .expectHeader().exists("Access-Control-Allow-Methods")
        ;

    }
}