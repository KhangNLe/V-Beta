package app.VBeta.api.dto.image;

import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ImageStorageRequest(
        @NotBlank String fileName,
        @NotBlank String contentType,
        @NotNull ImageTargetType imageTargetType,
        Long problemId,
        Long wallSectionId,
        Long userid
){}
