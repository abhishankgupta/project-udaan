package org.udaan.problem;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProblemUpdateRepository extends JpaRepository<ProblemUpdate, UUID> {

    List<ProblemUpdate> findByProblemIdOrderByCreatedAtAsc(UUID problemId);
}
