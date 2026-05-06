package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.api.dto.UserCommentData;
import edu.ics499.VBeta.domain.model.*;
import edu.ics499.VBeta.repository.DiscussionRootRepository;
import org.aspectj.apache.bcel.generic.InstructionConstants;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DiscussionRootManager {
    private final DiscussionRootRepository discussionRootRepository;
    private final DiscussionCommentManager discussionCommentManager;
    private final SolutionBetaManager solutionBetaManager;

    public DiscussionRootManager(DiscussionRootRepository discussionRootRepository,
                                 DiscussionCommentManager discussionCommentManager,
                                 SolutionBetaManager solutionBetaManager) {
        this.discussionRootRepository = discussionRootRepository;
        this.discussionCommentManager = discussionCommentManager;
        this.solutionBetaManager = solutionBetaManager;
    }

    public DiscussionRoot createNewDiscussion(UserAccount author, ClimbingProblem problem,
                                              DiscussionType discussionType){
        DiscussionRoot discussionRoot = new DiscussionRoot();
        discussionRoot.setParent(null);
        discussionRoot.setProblem(problem);
        discussionRoot.setDiscussionType(discussionType);
        discussionRoot.setUserAccount(author);
        discussionRoot.setCreatedAt(LocalDateTime.now());

        return discussionRootRepository.save(discussionRoot);
    }

    public DiscussionRoot createSubDiscussionThread(UserAccount account, ClimbingProblem problem,
                                                    DiscussionType discussionType, DiscussionRoot parent){
        DiscussionRoot discussionRoot = createNewDiscussion(account, problem, discussionType);
        discussionRoot.setParent(parent);

        return discussionRootRepository.save(discussionRoot);
    }

    public List<DiscussionRoot> getDiscussionForProblem(ClimbingProblem problem){
        return discussionRootRepository.findByClimbingProblem(problem);
    }

    public DiscussionRoot createReplyDiscussionRoot(UserAccount userAccount, ClimbingProblem problem,
                                                  DiscussionType discussionType, DiscussionRoot parent){
        DiscussionRoot discussionRoot = new DiscussionRoot();
        discussionRoot.setParent(parent);
        discussionRoot.setUserAccount(userAccount);
        discussionRoot.setCreatedAt(LocalDateTime.now());
        discussionRoot.setProblem(problem);
        discussionRoot.setDiscussionType(discussionType);
        return discussionRootRepository.save(discussionRoot);
    }

    public DiscussionRoot findDiscussionRootById(Long discussionRootId){
        Optional<DiscussionRoot> discussionRoot = discussionRootRepository.findById(discussionRootId);
        return discussionRoot.orElse(null);
    }
}
