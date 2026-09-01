package app.VBeta.mvc;

import app.VBeta.api.dto.discussions.video.CloudFileStorageResponse;
import app.VBeta.api.dto.image.ImageTargetType;
import app.VBeta.application.AuthorizationService;
import app.VBeta.application.ImageService;
import app.VBeta.controller.SocialMediaController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SocialMediaController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class SocialMediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthorizationService authorizationService;

    @MockitoBean
    private ImageService imageService;

    private static final String FIREBASE_UID = "testFirebaseUid";

    private CloudFileStorageResponse sampleSignedUrlResponse() {
        return new CloudFileStorageResponse(
                "https://storage.googleapis.com/signed",
                "PUT",
                "walls/1/section-image.webp",
                "https://storage.googleapis.com/bucket/walls/1/section-image.webp"
        );
    }

    @Test
    @DisplayName("GET /api/social/image/signed-url returns signed URL for wall section image")
    void returns200_whenRequestingWallSectionSignedUrl() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn(FIREBASE_UID);
        when(imageService.createSignedUrl(eq(FIREBASE_UID), any())).thenReturn(sampleSignedUrlResponse());

        mockMvc.perform(get("/api/social/image/signed-url")
                        .param("fileName", "section-image.webp")
                        .param("contentType", "image/webp")
                        .param("imageTargetType", ImageTargetType.WALL_SECTION.name())
                        .param("wallSectionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("PUT"))
                .andExpect(jsonPath("$.signedURL").value("https://storage.googleapis.com/signed"))
                .andExpect(jsonPath("$.uploadObjectName").value("walls/1/section-image.webp"))
                .andExpect(jsonPath("$.publicURL").value("https://storage.googleapis.com/bucket/walls/1/section-image.webp"));

        verify(imageService, times(1)).createSignedUrl(eq(FIREBASE_UID), any());
    }

    @Test
    @DisplayName("GET /api/social/image/signed-url returns signed URL for climbing problem image")
    void returns200_whenRequestingProblemSignedUrl() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn(FIREBASE_UID);
        when(imageService.createSignedUrl(eq(FIREBASE_UID), any())).thenReturn(
                new CloudFileStorageResponse(
                        "https://storage.googleapis.com/signed-problem",
                        "PUT",
                        "walls/1/problems/22/problem-image.jpg",
                        "https://storage.googleapis.com/bucket/walls/1/problems/22/problem-image.jpg"
                )
        );

        mockMvc.perform(get("/api/social/image/signed-url")
                        .param("fileName", "problem-image.jpg")
                        .param("contentType", "image/jpeg")
                        .param("imageTargetType", ImageTargetType.CLIMBING_PROBLEM.name())
                        .param("problemId", "22")
                        .param("wallSectionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signedURL").value("https://storage.googleapis.com/signed-problem"))
                .andExpect(jsonPath("$.uploadObjectName").value("walls/1/problems/22/problem-image.jpg"));
    }

    @Test
    @DisplayName("GET /api/social/image/signed-url returns signed URL for user profile image")
    void returns200_whenRequestingProfileSignedUrl() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn(FIREBASE_UID);
        when(imageService.createSignedUrl(eq(FIREBASE_UID), any())).thenReturn(
                new CloudFileStorageResponse(
                        "https://storage.googleapis.com/signed-profile",
                        "PUT",
                        "profiles/5/avatar.png",
                        "https://storage.googleapis.com/bucket/profiles/5/avatar.png"
                )
        );

        mockMvc.perform(get("/api/social/image/signed-url")
                        .param("fileName", "avatar.png")
                        .param("contentType", "image/png")
                        .param("imageTargetType", ImageTargetType.USER_ACCOUNT.name())
                        .param("userid", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadObjectName").value("profiles/5/avatar.png"));
    }

    @Test
    @DisplayName("GET /api/social/image/signed-url maps invalid request to 400")
    void returns400_whenSignedUrlRequestInvalid() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn(FIREBASE_UID);
        when(imageService.createSignedUrl(eq(FIREBASE_UID), any()))
                .thenThrow(new IllegalArgumentException("Unsupported image type"));

        mockMvc.perform(get("/api/social/image/signed-url")
                        .param("fileName", "section-image.gif")
                        .param("contentType", "image/gif")
                        .param("imageTargetType", ImageTargetType.WALL_SECTION.name())
                        .param("wallSectionId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Unsupported image type"));
    }

    @Test
    @DisplayName("GET /api/social/image/signed-url maps service failure to 404")
    void returns404_whenSignedUrlServiceFails() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn(FIREBASE_UID);
        when(imageService.createSignedUrl(eq(FIREBASE_UID), any()))
                .thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(get("/api/social/image/signed-url")
                        .param("fileName", "section-image.webp")
                        .param("contentType", "image/webp")
                        .param("imageTargetType", ImageTargetType.WALL_SECTION.name())
                        .param("wallSectionId", "1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found"));
    }

    @Test
    @DisplayName("GET /api/social/image/signed-url maps unexpected failure to 500")
    void returns500_whenSignedUrlUnexpectedFailure() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn(FIREBASE_UID);
        doAnswer(invocation -> {
            throw new Exception("Failed to create signed upload URL");
        }).when(imageService).createSignedUrl(eq(FIREBASE_UID), any());

        mockMvc.perform(get("/api/social/image/signed-url")
                        .param("fileName", "section-image.webp")
                        .param("contentType", "image/webp")
                        .param("imageTargetType", ImageTargetType.WALL_SECTION.name())
                        .param("wallSectionId", "1"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to create signed upload URL"));
    }

    @Test
    @DisplayName("PATCH /api/social/image/upload saves wall section image metadata")
    void returns200_whenUploadingWallSectionImage() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn(FIREBASE_UID);
        doNothing().when(imageService).saveImage(eq(FIREBASE_UID), any());

        mockMvc.perform(patch("/api/social/image/upload")
                        .param("targetType", ImageTargetType.WALL_SECTION.name())
                        .param("objectFileName", "walls/1/section-image.webp")
                        .param("imageUrl", "https://storage.googleapis.com/bucket/walls/1/section-image.webp")
                        .param("wallSectionId", "1"))
                .andExpect(status().isOk());

        verify(imageService, times(1)).saveImage(eq(FIREBASE_UID), any());
    }

    @Test
    @DisplayName("PATCH /api/social/image/upload saves climbing problem image metadata")
    void returns200_whenUploadingProblemImage() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn(FIREBASE_UID);
        doNothing().when(imageService).saveImage(eq(FIREBASE_UID), any());

        mockMvc.perform(patch("/api/social/image/upload")
                        .param("targetType", ImageTargetType.CLIMBING_PROBLEM.name())
                        .param("objectFileName", "walls/1/problems/22/problem-image.jpg")
                        .param("imageUrl", "https://storage.googleapis.com/bucket/walls/1/problems/22/problem-image.jpg")
                        .param("climbingProblemId", "22"))
                .andExpect(status().isOk());

        verify(imageService, times(1)).saveImage(eq(FIREBASE_UID), any());
    }

    @Test
    @DisplayName("PATCH /api/social/image/upload saves user profile image metadata")
    void returns200_whenUploadingProfileImage() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn(FIREBASE_UID);
        doNothing().when(imageService).saveImage(eq(FIREBASE_UID), any());

        mockMvc.perform(patch("/api/social/image/upload")
                        .param("targetType", ImageTargetType.USER_ACCOUNT.name())
                        .param("objectFileName", "profiles/5/avatar.png")
                        .param("imageUrl", "https://storage.googleapis.com/bucket/profiles/5/avatar.png")
                        .param("userId", "5"))
                .andExpect(status().isOk());

        verify(imageService, times(1)).saveImage(eq(FIREBASE_UID), any());
    }

    @Test
    @DisplayName("PATCH /api/social/image/upload maps service failure to 400")
    void returns400_whenUploadImageFails() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn(FIREBASE_UID);
        doThrow(new RuntimeException("Mismatching user ID."))
                .when(imageService).saveImage(eq(FIREBASE_UID), any());

        mockMvc.perform(patch("/api/social/image/upload")
                        .param("targetType", ImageTargetType.USER_ACCOUNT.name())
                        .param("objectFileName", "profiles/5/avatar.png")
                        .param("imageUrl", "https://storage.googleapis.com/bucket/profiles/5/avatar.png")
                        .param("userId", "99"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Mismatching user ID."));
    }

    @Test
    @DisplayName("PATCH /api/social/image/upload maps unexpected failure to 500")
    void returns500_whenUploadImageUnexpectedFailure() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn(FIREBASE_UID);
        doAnswer(invocation -> {
            throw new Exception("Database write failed");
        }).when(imageService).saveImage(eq(FIREBASE_UID), any());

        mockMvc.perform(patch("/api/social/image/upload")
                        .param("targetType", ImageTargetType.WALL_SECTION.name())
                        .param("objectFileName", "walls/1/section-image.webp")
                        .param("imageUrl", "https://storage.googleapis.com/bucket/walls/1/section-image.webp")
                        .param("wallSectionId", "1"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Database write failed"));
    }

    @Test
    @DisplayName("DELETE /api/social/image/problem returns 200")
    void returns200_whenDeletingProblemImage() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn(FIREBASE_UID);
        doNothing().when(imageService).problemImageDeletion(FIREBASE_UID, 22L);

        mockMvc.perform(delete("/api/social/image/problem")
                        .param("climbingProblemId", "22"))
                .andExpect(status().isOk());

        verify(imageService, times(1)).problemImageDeletion(FIREBASE_UID, 22L);
    }

    @Test
    @DisplayName("DELETE /api/social/image/problem maps service failure to 404")
    void returns404_whenDeletingProblemImageFails() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn(FIREBASE_UID);
        doThrow(new RuntimeException("Climbing problem not found"))
                .when(imageService).problemImageDeletion(FIREBASE_UID, 999L);

        mockMvc.perform(delete("/api/social/image/problem")
                        .param("climbingProblemId", "999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Climbing problem not found"));
    }

    @Test
    @DisplayName("DELETE /api/social/image/problem maps unexpected failure to 500")
    void returns500_whenDeletingProblemImageUnexpectedFailure() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn(FIREBASE_UID);
        doAnswer(invocation -> {
            throw new Exception("Storage delete failed");
        }).when(imageService).problemImageDeletion(FIREBASE_UID, 22L);

        mockMvc.perform(delete("/api/social/image/problem")
                        .param("climbingProblemId", "22"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Storage delete failed"));
    }

    @Test
    @DisplayName("DELETE /api/social/image/wall returns 200")
    void returns200_whenDeletingWallImage() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn(FIREBASE_UID);
        doNothing().when(imageService).wallSectionImageDeletion(FIREBASE_UID, 1L);

        mockMvc.perform(delete("/api/social/image/wall")
                        .param("wallSectionId", "1"))
                .andExpect(status().isOk());

        verify(imageService, times(1)).wallSectionImageDeletion(FIREBASE_UID, 1L);
    }

    @Test
    @DisplayName("DELETE /api/social/image/wall maps service failure to 404")
    void returns404_whenDeletingWallImageFails() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn(FIREBASE_UID);
        doThrow(new RuntimeException("Wall section not found"))
                .when(imageService).wallSectionImageDeletion(FIREBASE_UID, 999L);

        mockMvc.perform(delete("/api/social/image/wall")
                        .param("wallSectionId", "999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Wall section not found"));
    }

    @Test
    @DisplayName("DELETE /api/social/image/wall maps unexpected failure to 500")
    void returns500_whenDeletingWallImageUnexpectedFailure() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn(FIREBASE_UID);
        doAnswer(invocation -> {
            throw new Exception("Storage delete failed");
        }).when(imageService).wallSectionImageDeletion(FIREBASE_UID, 1L);

        mockMvc.perform(delete("/api/social/image/wall")
                        .param("wallSectionId", "1"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Storage delete failed"));
    }

    @Test
    @DisplayName("DELETE /api/social/image/problem does not call service when auth fails")
    void doesNotDeleteProblemImage_whenAuthFails() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid())
                .thenThrow(new RuntimeException("Missing or invalid authentication token"));

        mockMvc.perform(delete("/api/social/image/problem")
                        .param("climbingProblemId", "22"))
                .andExpect(status().isNotFound());

        verify(imageService, never()).problemImageDeletion(any(), any());
    }
}
