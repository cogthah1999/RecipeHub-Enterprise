package recipehub.recipe.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import recipehub.comment.entity.Comment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import recipehub.category.entity.Category;
import recipehub.user.entity.User;



@Entity
@Table(name = "recipes")
@Getter
@Setter
@NoArgsConstructor
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer cookingTime;

    private Integer servings;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    private String imageUrl;

    // Quan hệ Many-to-One: Một danh mục (Category) có thể có nhiều công thức
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Quan hệ Many-to-One: Một người dùng (User) có thể tạo nhiều công thức
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Deleting a recipe also deletes its comments, avoiding orphaned records.
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.REMOVE)
    private List<Comment> comments = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Tự động thiết lập thời gian tạo trước khi lưu vào cơ sở dữ liệu
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}