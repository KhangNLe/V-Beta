package app.VBeta.repository;

import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.climb.LifecycleStatus;
import app.VBeta.domain.model.climb.WallSection;
import app.VBeta.domain.model.climb.ClimbingGrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for {@link ClimbingProblem} entities.
 */
public interface ClimbingProblemRepository extends JpaRepository<ClimbingProblem, Long> {
    /**
     * Returns climbing problems associated with a wall section.
     *
     * @param wallSection wall section
     * @return climbing problems for the wall section
     */
    List<ClimbingProblem> findByWallSection(WallSection wallSection);

    /**
     * Returns problems for a wall section matching an exact assigned grade and lifecycle status.
     *
     * @param wallSection wall section
     * @param climbingGrade assigned climbing grade
     * @param status lifecycle status filter
     * @return matching climbing problems
     */
    List<ClimbingProblem> findByWallSectionAndClimbingGradeAndProblemStatus(
            WallSection wallSection, ClimbingGrade climbingGrade, LifecycleStatus status);

    /**
     * Returns problems for a wall section with the given status whose assigned grade is
     * inclusively between {@code minGrade} and {@code maxGrade}, unsorted.
     *
     * @param wallSection wall section
     * @param status lifecycle status filter
     * @param minGrade inclusive lower bound grade
     * @param maxGrade inclusive upper bound grade
     * @return matching climbing problems
     */
    List<ClimbingProblem> findByWallSectionAndProblemStatusAndClimbingGradeBetween(WallSection wallSection,
                                                                   LifecycleStatus status,
                                                                   ClimbingGrade minGrade,
                                                                   ClimbingGrade maxGrade);

    /**
     * Returns problems for a wall section with the given status whose assigned grade is
     * inclusively between {@code minGrade} and {@code maxGrade}, ordered by grade ascending.
     *
     * @param wallSection wall section
     * @param status lifecycle status filter
     * @param minGrade inclusive lower bound grade
     * @param maxGrade inclusive upper bound grade
     * @return matching climbing problems sorted ascending by assigned grade
     */
    List<ClimbingProblem> findByWallSectionAndProblemStatusAndClimbingGradeBetweenOrderByClimbingGradeAsc(
            WallSection wallSection, LifecycleStatus status, ClimbingGrade minGrade, ClimbingGrade maxGrade);

    /**
     * Returns problems for a wall section with the given status whose assigned grade is
     * inclusively between {@code minGrade} and {@code maxGrade}, ordered by grade descending.
     *
     * @param wallSection wall section
     * @param status lifecycle status filter
     * @param minGrade inclusive lower bound grade
     * @param maxGrade inclusive upper bound grade
     * @return matching climbing problems sorted descending by assigned grade
     */
    List<ClimbingProblem> findByWallSectionAndProblemStatusAndClimbingGradeBetweenOrderByClimbingGradeDesc(
            WallSection wallSection, LifecycleStatus status, ClimbingGrade minGrade, ClimbingGrade maxGrade);
}
