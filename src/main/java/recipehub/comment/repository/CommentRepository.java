package recipehub.comment.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import recipehub.comment.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByRecipeIdOrderByCreatedAtDescIdDesc(Long recipeId);
    List<Comment> findTop8ByOrderByCreatedAtDescIdDesc();
    long countByRecipeId(Long recipeId);
}
