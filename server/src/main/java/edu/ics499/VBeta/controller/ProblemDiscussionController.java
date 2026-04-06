package edu.ics499.VBeta.controller;

import edu.ics499.VBeta.api.dto.DiscussionCommentRequest;
import edu.ics499.VBeta.application.ProblemDiscussionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/discussion")
public class ProblemDiscussionController {
    private final ProblemDiscussionService problemDiscussionService;

    public ProblemDiscussionController(ProblemDiscussionService problemDiscussionService){
        this.problemDiscussionService = problemDiscussionService;
    }

    @PostMapping("/add-comments")
    @ResponseStatus(HttpStatus.CREATED)
    public void addUserComment(@Valid @RequestBody DiscussionCommentRequest request){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Missing or invalid authentication");
        }
        problemDiscussionService.addComment(String.valueOf(auth.getPrincipal()), request);
    }
}
