package media.toloka.rfa.radio.model;


import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(indexes = @Index(columnList = "uuid"))
public class Album {
    @Id
    @GeneratedValue
    @Expose
    private Long id;
    @Expose
    private String uuid= UUID.randomUUID().toString();

    @Expose
    private String name;

    @Expose
    private String autor;

    @Expose
    private Date createdate = new Date();

    @Expose
    private String albumrelisedate = new Date().toString();

    @Expose
    @DateTimeFormat(pattern = "dd-MM-yy")
    private Date datealbumrelise = new Date();

//    @Expose
//    private AlbumCover albumcover;
    @Expose
    @OneToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "albumcover_id")
    private Albumсover albumcover;

    @Expose
    private String storeuuidalbumcover;

    @Expose
    private String albumcoverdateupload = new Date().toString();

    @Expose
    private String style;

    @Expose
    @Column(columnDefinition = "TEXT")
    private String description;

    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
    @JoinColumn(name = "clientdetail_id")
    private Clientdetail clientdetail;

    @ToString.Exclude
    @OneToMany(mappedBy = "album", fetch=FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Track> track;


}
