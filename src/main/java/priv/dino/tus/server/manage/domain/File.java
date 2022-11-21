package priv.dino.tus.server.manage.domain;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件
 *
 * @author dino
 * @date 2021/10/29 10:09
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@With
@Table("file")
public class File implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @Size(max = 255)
    private String mimeType;

    @NotNull
    private Long contentLength;

    @NotNull
    private Long contentOffset;

    @NotNull
    private Long lastUploadedChunkNumber;

    @NotBlank
    @Size(max = 500)
    private String originalName;

    @NotNull
    @Column(value = "is_partial")
    private boolean partialStatus;

    @NotBlank
    @Size(max = 500)
    private String fingerprint;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
