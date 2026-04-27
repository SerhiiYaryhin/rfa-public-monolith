package media.toloka.rfa.author.model;

import jakarta.persistence.*;
import lombok.Data;
import media.toloka.rfa.author.model.enumerate.EAuthorRequestStatus;
import media.toloka.rfa.radio.model.Clientdetail;

import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(name = "author_requests")
public class AuthorRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String uuid = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_uuid", referencedColumnName = "uuid")
    private Clientdetail client;

    private String topic;      // Тематика колонки

    @Column(columnDefinition = "TEXT")
    private String resume;     // Резюме/Портфоліо

    @Column(columnDefinition = "TEXT")
    private String links;      // Посилання на роботи

    @Enumerated(EnumType.STRING)
    private EAuthorRequestStatus status = EAuthorRequestStatus.PENDING;

    private Date createdAt = new Date();
    private Date processedAt;
}
