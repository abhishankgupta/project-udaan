package org.udaan.discussion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByTargetTypeAndTargetIdOrderByCreatedAtAsc(Comment.TargetType type, UUID targetId);

    long countByAuthorIdAndCreatedAtAfter(UUID authorId, Instant since);
}
