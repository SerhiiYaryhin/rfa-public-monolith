package media.toloka.rfa.comments.dto;

import com.google.gson.annotations.Expose;
import lombok.Data;

@Data
public class AuthorDTO {
    @Expose
    private String uuid;
    @Expose
    private String username; // Припустимо, у Clientdetail є поле username
    // Додайте інші поля Clientdetail, які ви хочете виставити через API
    // наприклад, @Expose private String avatarUrl;
}
