package org.udaan.problem;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ProblemRepository extends JpaRepository<Problem, UUID> {

    Optional<Problem> findByPublicCode(String publicCode);

    List<Problem> findBySubmitterIdOrderByCreatedAtDesc(UUID submitterId);

    long countBySubmitterIdAndCreatedAtAfter(UUID submitterId, Instant since);

    @Query(value = "SELECT nextval('problem_code_seq')", nativeQuery = true)
    long nextCodeNumber();
}
