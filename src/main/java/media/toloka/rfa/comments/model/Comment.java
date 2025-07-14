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
import java.util.Date;
import java.util.UUID;

@Data
@Entity
public class Comment {
    @Id
    @Expose
    private String uuid;
    @Expose
    private Long id;

    @Expose
    @Column(columnDefinition = "TEXT")
    private String content;

    @Expose
//    @DateTimeFormat(pattern = "dd-MM-yy")
    private Date createdate = new Date(); // Дата створення коментаря

    @Expose
    // Вказує на тип об'єкта (наприклад: POST, ARTICLE, TRACK)
    private ECommentSourceType commentSourceType;

    @Expose
    // коментар схвалений
    private Boolean apruve = true;
    @Expose
    // дата схвалення коментаря
    private Date apruvedate = null;

    @Expose
    // ID цього об'єкта
    private String targetuuid;

    @ToString.Exclude
    @ManyToOne()
    @JoinColumn(name = "clientdetail_id")
    private Clientdetail autor;

    @PrePersist
    public void generateUUID() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        if (this.id == null) {
            this.id = System.currentTimeMillis(); // Метод для генерації унікального ID
        }
    }
}

