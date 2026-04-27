package media.toloka.rfa.author.model;

import jakarta.persistence.*;
import lombok.Data;
import media.toloka.rfa.radio.model.Clientdetail;

import java.util.UUID;

@Data
@Entity
@Table(name = "author_columns")
public class AuthorColumn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String uuid = UUID.randomUUID().toString();

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_uuid", referencedColumnName = "uuid")
    private Clientdetail author;

    private String title;          // Назва колонки
    
    @Column(columnDefinition = "TEXT")
    private String description;    // Опис/Біографія
    
    private String avatarUuid;     // Посилання на фото у Store
    
    @Column(unique = true)
    private String slug;           // ЧПУ посилання
}
