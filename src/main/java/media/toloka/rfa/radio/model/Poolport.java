package media.toloka.rfa.radio.model;

import jakarta.persistence.*;
import lombok.Data;
import media.toloka.rfa.radio.model.enumerate.EServerPortType;

// Пул портів для станції

@Data
@Entity
@Table
public class Poolport {
    @Id
    @GeneratedValue
    private Long id;
    private Integer port;
    private EServerPortType porttype;

    @ManyToOne
    @JoinColumn(name = "station_id")
    private Station station;
}

