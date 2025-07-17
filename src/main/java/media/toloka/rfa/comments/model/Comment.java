package media.toloka.rfa.comments.model;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import media.toloka.rfa.comments.model.enumerate.ECommentSourceType;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Post;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@Entity
public class Comment {
    @Id
    @Expose
    @GeneratedValue(strategy = GenerationType.UUID)
    private String uuid;
    @Expose
    private Long id;

    @Expose
    @Column(columnDefinition = "TEXT")
    private String text;

    @Expose
    private Date timestamp = new Date(); // Дата створення коментаря

    @Expose
    @Column(nullable = false)
    private String contentEntityType; //Вказує на тип об'єкта. Наприклад: "POST", "COLUMN", "TRACK"

    @Expose
    @Column(nullable = false)
    private String contentEntityId;   // ID конкретного поста, колонки, треку тощо

    @Expose
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_uuid")
    private Comment parentComment;

    @Expose
    private int depth;

//    @Expose
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("timestamp ASC")
    private List<Comment> replies = new ArrayList<>();

    @Expose
    // коментар схвалений
    private Boolean apruve = true;
    @Expose
    // дата схвалення коментаря
    private Date apruvedate = null;

    @ToString.Exclude
    @ManyToOne()
    @JoinColumn(name = "clientdetail_id")
    private Clientdetail author;

    public Comment() {
        this.timestamp = new Date();
    }

    public Comment(Clientdetail author, String authorId, String text, String contentEntityType, String contentEntityId) {
        this();
        this.author = author;
        this.text = text;
        this.contentEntityType = contentEntityType;
        this.contentEntityId = contentEntityId;
        this.depth = 0;
    }

    public Comment(Clientdetail author, String authorId, String text, Comment parentComment, int depth, String contentEntityType, String contentEntityId) {
        this();
        this.author = author;
        this.text = text;
        this.parentComment = parentComment;
        this.depth = depth;
        this.contentEntityType = contentEntityType;
        this.contentEntityId = contentEntityId;
    }

    /**
     * Додає відповідь до списку відповідей поточного коментаря та встановлює поточний коментар як батьківський для відповіді.
     * @param reply Коментар, який додається як відповідь.
     */
    public void addReply(Comment reply) {
        this.replies.add(reply);
        reply.setParentComment(this); // Встановлюємо батьківський коментар для нової відповіді
    }

    /**
     * Видаляє відповідь зі списку відповідей поточного коментаря та розриває зв'язок з батьківським коментарем для відповіді.
     * @param reply Коментар, який видаляється зі списку відповідей.
     */
    public void removeReply(Comment reply) {
        this.replies.remove(reply);
        reply.setParentComment(null); // Розриваємо зв'язок
    }

    @PrePersist
    public void generateUUID() {
//        if (uuid == null) {
//            uuid = UUID.randomUUID().toString();
//        }
        if (this.id == null) {
            this.id = System.currentTimeMillis(); // Метод для генерації унікального ID
        }
    }
}

