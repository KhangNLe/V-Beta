package app.VBeta.application;

import app.VBeta.api.dto.discussions.video.CloudFileStorageResponse;
import app.VBeta.api.dto.image.ImageStorageRequest;
import app.VBeta.api.dto.image.ImageTargetType;
import app.VBeta.api.dto.image.ProfileImageCreationRequest;
import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.application.support.cloud.CloudStorageManager;
import app.VBeta.application.support.problem.ClimbingProblemManager;
import app.VBeta.application.support.wall.WallSectionManager;
import app.VBeta.domain.model.actions.ActionDefinition;
import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.climb.WallSection;
import app.VBeta.domain.model.user.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ImageService {
    private final CloudStorageManager cloudStorageManager;
    private final UserAccountManager userAccountManager;
    private final ClimbingProblemManager climbingProblemManager;
    private final WallSectionManager wallSectionManager;
    private final AuthorizationService authorizationService;

    public ImageService(CloudStorageManager cloudStorageManager,
                        UserAccountManager userAccountManager,
                        ClimbingProblemManager climbingProblemManager,
                        WallSectionManager wallSectionManager,
                        AuthorizationService authorizationService) {
        this.cloudStorageManager = cloudStorageManager;
        this.userAccountManager = userAccountManager;
        this.climbingProblemManager = climbingProblemManager;
        this.wallSectionManager = wallSectionManager;
        this.authorizationService = authorizationService;
    }

    public CloudFileStorageResponse createSignedUrl(String firebaseUid, ImageStorageRequest request){
        validateImageRequest(firebaseUid, request.imageTargetType(), request.userid());
        return cloudStorageManager.createImageSignedUrl(request);
    }

    public void saveImage(String firebaseUid, ProfileImageCreationRequest request){
        validateImageRequest(firebaseUid, request.targetType(), request.userId());
        switch (request.targetType()){
            case WALL_SECTION -> wallSectionManager.updateWallImage(
                    request.wallSectionId(),
                    request.objectFileName(),
                    request.imageUrl()
            );
            case CLIMBING_PROBLEM -> climbingProblemManager.updateProblemImage(
                    request.climbingProblemId(),
                    request.objectFileName(),
                    request.imageUrl()
            );
            case USER_ACCOUNT -> userAccountManager.updateUserProfile(
                    request.userId(),
                    request.objectFileName(),
                    request.imageUrl()
            );
        }
    }

    public void problemImageDeletion(String firebaseUid, Long problemId){
        findAndValidateUserAccount(firebaseUid, ActionDefinition.UPLOAD_PROBLEM_IMAGE);

        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);
        if (problem == null){
            throw new RuntimeException("Climbing problem not found");
        }

        cloudStorageManager.deleteImageObject(problem.getObjectImageName());
        climbingProblemManager.removeProblemImage(problem);
    }

    public void wallSectionImageDeletion(String firebaseUid, Long wallSectionId){
        findAndValidateUserAccount(firebaseUid, ActionDefinition.UPLOAD_WALL_IMAGE);

        WallSection wall = wallSectionManager.findWallSection(wallSectionId);
        cloudStorageManager.deleteImageObject(wall.getImageObjectName());
        wallSectionManager.removeWallImage(wall);
    }

    private void findAndValidateUserAccount(String firebaseUid, ActionDefinition action){
        UserAccount user = userAccountManager.findUserAccount(firebaseUid);
        if (user == null){
            throw new RuntimeException("User not found");
        }
        if (action != null){
            authorizationService.authorize(user, action);
        }
    }

    private void validateImageRequest(String firebaseUid, ImageTargetType targetType, Long userId){
        UserAccount user = userAccountManager.findUserAccount(firebaseUid);
        if (user == null){
            throw new  RuntimeException("User not found");
        }
        if (targetType.equals(ImageTargetType.WALL_SECTION)){
            authorizationService.authorize(user, ActionDefinition.UPLOAD_WALL_IMAGE);
        } else if (targetType.equals(ImageTargetType.CLIMBING_PROBLEM)){
            authorizationService.authorize(user, ActionDefinition.UPLOAD_PROBLEM_IMAGE);
        } else if (!user.getId().equals(userId)){
            throw new RuntimeException("Mismatching user ID.");
        }
    }
}
