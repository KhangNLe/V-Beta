package edu.ics499.VBeta.controller;

import edu.ics499.VBeta.api.dto.DiscussionCommentRequest;
import edu.ics499.VBeta.api.dto.UserCommentData;
import edu.ics499.VBeta.application.CommentManager;
import edu.ics499.VBeta.domain.model.LifecycleStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/discussion")
public class ProblemDiscussionController {
    private final CommentManager commentManager;

    public ProblemDiscussionController(CommentManager commentManager){
        this.commentManager = commentManager;
    }

    @PostMapping("/add-comments")
    public void addUserComment(@Valid @RequestBody DiscussionCommentRequest request){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Missing or invalid authentication");
        }
        try {
            commentManager.addComment(String.valueOf(auth.getPrincipal()), request);

        } catch (IllegalStateException e){
            throw new ResponseStatusException(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS, e.getMessage());
        }
    }
}
