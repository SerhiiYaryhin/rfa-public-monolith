package media.toloka.rfa.podcast;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;

@Profile("Front")
@Controller
public class PodcastStreamController {
    // Стрім подкастів у випадковій послідовності як потік аудіо
    // Це на майбутне.
    // Тобто, це окрема радіостанція, яка транслює епізоди подкастів
    // через icecast2 або через nginx (ICECAST2, HLS, MPEG-DASH)

}
