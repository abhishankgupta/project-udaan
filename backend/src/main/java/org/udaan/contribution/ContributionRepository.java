package org.udaan.contribution;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ContributionRepository extends JpaRepository<Contribution, UUID> {

    @Query("select c from Contribution c where c.problemId = :problemId and c.status = 'INTERESTED' order by c.createdAt")
    List<Contribution> activeForProblem(UUID problemId);

    @Query("select c from Contribution c where c.problemId = :problemId and c.userId = :userId"
            + " and c.status = 'INTERESTED' and c.solutionId is null")
    Optional<Contribution> activeOfferOnProblem(UUID problemId, UUID userId);

    @Query("select c from Contribution c where c.problemId = :problemId and c.userId = :userId"
            + " and c.status = 'INTERESTED' and c.solutionId = :solutionId")
    Optional<Contribution> activeOfferOnSolution(UUID problemId, UUID userId, UUID solutionId);

    @Query("select c from Contribution c where c.userId = :userId and c.status = 'INTERESTED' order by c.createdAt desc")
    List<Contribution> activeForUser(UUID userId);
}
