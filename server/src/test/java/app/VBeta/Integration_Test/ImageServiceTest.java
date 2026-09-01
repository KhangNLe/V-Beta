package app.VBeta.Integration_Test;

import app.VBeta.api.dto.discussions.video.CloudFileStorageResponse;
import app.VBeta.api.dto.image.ImageStorageRequest;
import app.VBeta.api.dto.image.ImageTargetType;
import app.VBeta.api.dto.image.ProfileImageCreationRequest;
import app.VBeta.application.ImageService;
import app.VBeta.application.support.cloud.GcpFileStorageAdapter;
import app.VBeta.application.support.problem.ClimbingProblemManager;
import app.VBeta.application.support.wall.WallSectionManager;
import app.VBeta.config.TestGcpStorageConfig;
import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.climb.WallSection;
import app.VBeta.repository.ClimbingProblemRepository;
import app.VBeta.repository.WallSectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(TestGcpStorageConfig.class)
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource("classpath:application-postgres-it.properties")
public class ImageServiceTest {

    private static final String CLIMBER_UID = "testFirebaseUid";
    private static final String SETTER_UID = "testFirebaseUid2";
    private static final String ADMIN_UID = "testFirebaseUid3";

    private static final Long WALL_SECTION_ID = 1L;
    private static final Long ACTIVE_PROBLEM_ID = 1L;
    private static final Long CLIMBER_USER_ID = 1L;

    private static final String WALL_OBJECT_KEY = "image/wallSection-1/test-wall.webp";
    private static final String WALL_IMAGE_URL = "https://storage.googleapis.com/test-bucket/image/wallSection-1/test-wall.webp";
    private static final String PROBLEM_OBJECT_KEY = "image/problem-1/test-problem.jpg";
    private static final String PROBLEM_IMAGE_URL = "https://storage.googleapis.com/test-bucket/image/problem-1/test-problem.jpg";

    @Autowired
    private ImageService imageService;

    @Autowired
    private WallSectionManager wallSectionManager;

    @Autowired
    private ClimbingProblemManager climbingProblemManager;

    @Autowired
    private WallSectionRepository wallSectionRepository;

    @Autowired
    private ClimbingProblemRepository climbingProblemRepository;

    @MockitoBean
    private GcpFileStorageAdapter gcpFileStorageAdapter;

    @BeforeEach
    void stubCloudStorage() throws Exception {
        when(gcpFileStorageAdapter.getPublicBucketName()).thenReturn("test-bucket");
        when(gcpFileStorageAdapter.generateSignedPutURL(anyString(), anyString()))
                .thenReturn(new URL("https://storage.googleapis.com/signed"));
        when(gcpFileStorageAdapter.generatePublicURL(anyString(), anyString()))
                .thenAnswer(invocation -> String.format(
                        "https://storage.googleapis.com/%s/%s",
                        invocation.getArgument(0),
                        invocation.getArgument(1)
                ));
        doNothing().when(gcpFileStorageAdapter).deleteFile(anyString(), anyString());
    }

    @Test
    @DisplayName("Admin can create signed URL for wall section image")
    void createSignedUrl_allowsAdminForWallSection() {
        ImageStorageRequest request = new ImageStorageRequest(
                "section-image.webp",
                "image/webp",
                ImageTargetType.WALL_SECTION,
                null,
                WALL_SECTION_ID,
                null
        );

        CloudFileStorageResponse response = imageService.createSignedUrl(ADMIN_UID, request);

        assertNotNull(response);
        assertEquals("PUT", response.method());
        assertNotNull(response.signedURL());
        assertTrue(response.uploadObjectName().startsWith("image/wallSection-1/"));
        assertTrue(response.publicURL().startsWith("https://storage.googleapis.com/test-bucket/"));
    }

    @Test
    @DisplayName("Setter can create signed URL for climbing problem image")
    void createSignedUrl_allowsSetterForProblem() {
        ImageStorageRequest request = new ImageStorageRequest(
                "problem-image.jpg",
                "image/jpeg",
                ImageTargetType.CLIMBING_PROBLEM,
                ACTIVE_PROBLEM_ID,
                WALL_SECTION_ID,
                null
        );

        CloudFileStorageResponse response = imageService.createSignedUrl(SETTER_UID, request);

        assertNotNull(response);
        assertTrue(response.uploadObjectName().startsWith("image/problem-1/"));
    }

    @Test
    @DisplayName("Climber can create signed URL for own profile image")
    void createSignedUrl_allowsClimberForOwnProfile() {
        ImageStorageRequest request = new ImageStorageRequest(
                "avatar.png",
                "image/png",
                ImageTargetType.USER_ACCOUNT,
                null,
                null,
                CLIMBER_USER_ID
        );

        CloudFileStorageResponse response = imageService.createSignedUrl(CLIMBER_UID, request);

        assertNotNull(response);
        assertTrue(response.uploadObjectName().startsWith("image/userProfile-1/"));
    }

    @Test
    @DisplayName("Climber cannot create signed URL for wall section image")
    void createSignedUrl_rejectsClimberForWallSection() {
        ImageStorageRequest request = new ImageStorageRequest(
                "section-image.webp",
                "image/webp",
                ImageTargetType.WALL_SECTION,
                null,
                WALL_SECTION_ID,
                null
        );

        assertThrows(RuntimeException.class,
                () -> imageService.createSignedUrl(CLIMBER_UID, request));
    }

    @Test
    @DisplayName("Setter cannot create signed URL for wall section image")
    void createSignedUrl_rejectsSetterForWallSection() {
        ImageStorageRequest request = new ImageStorageRequest(
                "section-image.webp",
                "image/webp",
                ImageTargetType.WALL_SECTION,
                null,
                WALL_SECTION_ID,
                null
        );

        assertThrows(RuntimeException.class,
                () -> imageService.createSignedUrl(SETTER_UID, request));
    }

    @Test
    @DisplayName("Climber cannot create signed URL for another user's profile image")
    void createSignedUrl_rejectsProfileMismatch() {
        ImageStorageRequest request = new ImageStorageRequest(
                "avatar.png",
                "image/png",
                ImageTargetType.USER_ACCOUNT,
                null,
                null,
                99L
        );

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> imageService.createSignedUrl(CLIMBER_UID, request));
        assertEquals("Mismatching user ID.", error.getMessage());
    }

    @Test
    @DisplayName("Admin can save wall section image metadata")
    void saveImage_persistsWallSectionImage() {
        ProfileImageCreationRequest request = new ProfileImageCreationRequest(
                ImageTargetType.WALL_SECTION,
                WALL_OBJECT_KEY,
                WALL_IMAGE_URL,
                null,
                WALL_SECTION_ID,
                null
        );

        imageService.saveImage(ADMIN_UID, request);

        WallSection wall = wallSectionRepository.findById(WALL_SECTION_ID).orElseThrow();
        assertEquals(WALL_OBJECT_KEY, wall.getImageObjectName());
        assertEquals(WALL_IMAGE_URL, wall.getWallImageUrl());
    }

    @Test
    @DisplayName("Setter can save climbing problem image metadata")
    void saveImage_persistsProblemImage() {
        ProfileImageCreationRequest request = new ProfileImageCreationRequest(
                ImageTargetType.CLIMBING_PROBLEM,
                PROBLEM_OBJECT_KEY,
                PROBLEM_IMAGE_URL,
                null,
                null,
                ACTIVE_PROBLEM_ID
        );

        imageService.saveImage(SETTER_UID, request);

        ClimbingProblem problem = climbingProblemRepository.findById(ACTIVE_PROBLEM_ID).orElseThrow();
        assertEquals(PROBLEM_OBJECT_KEY, problem.getObjectImageName());
        assertEquals(PROBLEM_IMAGE_URL, problem.getProblemImageUrl());
    }

    @Test
    @DisplayName("Climber cannot save wall section image metadata")
    void saveImage_rejectsClimberForWallSection() {
        ProfileImageCreationRequest request = new ProfileImageCreationRequest(
                ImageTargetType.WALL_SECTION,
                WALL_OBJECT_KEY,
                WALL_IMAGE_URL,
                null,
                WALL_SECTION_ID,
                null
        );

        assertThrows(RuntimeException.class,
                () -> imageService.saveImage(CLIMBER_UID, request));
    }

    @Test
    @DisplayName("Setter can delete climbing problem image metadata")
    void problemImageDeletion_clearsProblemImageForSetter() {
        climbingProblemManager.updateProblemImage(ACTIVE_PROBLEM_ID, PROBLEM_OBJECT_KEY, PROBLEM_IMAGE_URL);

        imageService.problemImageDeletion(SETTER_UID, ACTIVE_PROBLEM_ID);

        ClimbingProblem problem = climbingProblemRepository.findById(ACTIVE_PROBLEM_ID).orElseThrow();
        assertNull(problem.getProblemImageUrl());
        verify(gcpFileStorageAdapter).deleteFile("test-bucket", PROBLEM_OBJECT_KEY);
    }

    @Test
    @DisplayName("Climber cannot delete climbing problem image")
    void problemImageDeletion_rejectsClimber() {
        climbingProblemManager.updateProblemImage(ACTIVE_PROBLEM_ID, PROBLEM_OBJECT_KEY, PROBLEM_IMAGE_URL);

        assertThrows(RuntimeException.class,
                () -> imageService.problemImageDeletion(CLIMBER_UID, ACTIVE_PROBLEM_ID));
    }

    @Test
    @DisplayName("Problem image deletion fails when problem does not exist")
    void problemImageDeletion_failsWhenProblemMissing() {
        RuntimeException error = assertThrows(RuntimeException.class,
                () -> imageService.problemImageDeletion(SETTER_UID, 999L));
        assertEquals("Climbing problem not found", error.getMessage());
    }

    @Test
    @DisplayName("Admin can delete wall section image metadata")
    void wallSectionImageDeletion_clearsWallImageForAdmin() {
        wallSectionManager.updateWallImage(WALL_SECTION_ID, WALL_OBJECT_KEY, WALL_IMAGE_URL);

        imageService.wallSectionImageDeletion(ADMIN_UID, WALL_SECTION_ID);

        WallSection wall = wallSectionRepository.findById(WALL_SECTION_ID).orElseThrow();
        assertNull(wall.getWallImageUrl());
        assertNull(wall.getImageObjectName());
        verify(gcpFileStorageAdapter).deleteFile("test-bucket", WALL_OBJECT_KEY);
    }

    @Test
    @DisplayName("Setter cannot delete wall section image")
    void wallSectionImageDeletion_rejectsSetter() {
        wallSectionManager.updateWallImage(WALL_SECTION_ID, WALL_OBJECT_KEY, WALL_IMAGE_URL);

        assertThrows(RuntimeException.class,
                () -> imageService.wallSectionImageDeletion(SETTER_UID, WALL_SECTION_ID));
    }

    @Test
    @DisplayName("Wall section image deletion fails when wall section does not exist")
    void wallSectionImageDeletion_failsWhenWallMissing() {
        RuntimeException error = assertThrows(RuntimeException.class,
                () -> imageService.wallSectionImageDeletion(ADMIN_UID, 999L));
        assertTrue(error.getMessage().contains("Wall Section with id 999 does not exist"));
    }
}
