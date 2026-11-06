package org.udaan.solution;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SolutionRepository extends JpaRepository<Solution, UUID> {

    List<Solution> findByProblemIdAndStatusInOrderByCreatedAtAsc(UUID problemId, Collection<SolutionStatus> statuses);

    List<Solution> findByProposerIdOrderByCreatedAtDesc(UUID proposerId);

    List<Solution> findByStatusInOrderByCreatedAtDesc(Collection<SolutionStatus> statuses, Pageable page);

    boolean existsByProblemIdAndStatus(UUID problemId, SolutionStatus status);
}
