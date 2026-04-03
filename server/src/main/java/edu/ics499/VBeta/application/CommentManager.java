package edu.ics499.VBeta.application;

import edu.ics499.VBeta.api.dto.DiscussionCommentRequest;
import edu.ics499.VBeta.domain.model.UserComment;
import edu.ics499.VBeta.domain.model.DiscussionComment;
import edu.ics499.VBeta.repository.UserAccountRepository;
import edu.ics499.VBeta.repository.UserCommentRepository;
import edu.ics499.VBeta.repository.DiscussionCommentRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class CommentManager {
    private final DiscussionCommentRepository discussionCommentRepository;
    private final UserCommentRepository userCommentRepository;
    private final UserAccountRepository userAccountRepository;

    public CommentManager(DiscussionCommentRepository discussionCommentRepository,
                          UserCommentRepository userCommentRepository,
                          UserAccountRepository userAccountRepository){
        this.userCommentRepository = userCommentRepository;
        this.discussionCommentRepository = discussionCommentRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public void addComment(String firebaseUid, DiscussionCommentRequest request){

    }
}
