package media.toloka.rfa.author.model;

import jakarta.persistence.*;
import lombok.Data;
import media.toloka.rfa.author.model.enumerate.EAuthorArticleStatus;

import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(name = "author_articles",
       uniqueConstraints = @UniqueConstraint(columnNames = "uuid"))
public class AuthorArticle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String uuid = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "column_uuid", referencedColumnName = "uuid")
    private AuthorColumn column;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String postbody;   // JSON дані від Editor.js

    @Column(columnDefinition = "TEXT")
    private String lead;       // короткий опис (лід)

    private String coverUuid;  // Головна ілюстрація

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EAuthorArticleStatus status = EAuthorArticleStatus.DRAFT;

    private Long looked = 0L;
    
    private Date createdAt = new Date();
    private Date publishDate;
    
    @Column(unique = true)
    private String slug;
}
