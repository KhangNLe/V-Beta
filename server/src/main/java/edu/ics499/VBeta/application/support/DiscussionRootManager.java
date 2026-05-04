package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.domain.model.DiscussionRoot;
import edu.ics499.VBeta.domain.model.DiscussionType;
import edu.ics499.VBeta.domain.model.UserAccount;
import edu.ics499.VBeta.repository.DiscussionRootRepository;
import org.aspectj.apache.bcel.generic.InstructionConstants;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Transactional
public class DiscussionRootManager {
    private final DiscussionRootRepository discussionRootRepository;

    public DiscussionRootManager(DiscussionRootRepository discussionRootRepository) {
        this.discussionRootRepository = discussionRootRepository;
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
}
