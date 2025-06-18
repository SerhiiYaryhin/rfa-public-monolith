package media.toloka.rfa.podcast.model;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table
public class PodcastItunesCategory {
//    @GeneratedValue
    @Expose
    private Long id;

    @Id
    @Expose
    private String uuid;

    @Expose
    private String firstlevel;

    @Expose
    private String secondlevel;

//    @ElementCollection
//    @ManyToOne(fetch=FetchType.EAGER, cascade = {CascadeType.PERSIST,CascadeType.ALL})
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name = "podcast_channel_uuid")
    private PodcastChannel chanel;

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
