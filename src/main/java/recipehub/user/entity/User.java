package recipehub.user.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    @Column(nullable = false)
    private String password;
    @Column(name = "full_name", length = 100)
    private String fullName;
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'USER'")
    private String role = "USER";
    @Column(name = "token_version", nullable = false, columnDefinition = "bigint default 0")
    private long tokenVersion;
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;
    @Column(name = "lock_reason", length = 250)
    private String lockReason;
    @Column(name = "locked_by", length = 50)
    private String lockedBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public User() { }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
