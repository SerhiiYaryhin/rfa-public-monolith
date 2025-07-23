package media.toloka.rfa.comments.dto;

import com.google.gson.annotations.Expose; // Продовжуємо використовувати @Expose для GSON
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class CommentDTO {
    @Expose
    private String uuid;
    @Expose
    private Long id;
    @Expose
    private String text;
    @Expose
    private Date timestamp;
    @Expose
    private String contentEntityType; // Може бути String, оскільки це лише DTO
    @Expose
    private String contentEntityId;

    // Замість повного об'єкта Comment, передаємо лише UUID батьківського коментаря
    @Expose
    private String parentCommentUuid;

    @Expose
    private int depth;

    // Список дочірніх CommentDTO для рекурсивної ієрархії
    @Expose
    private List<CommentDTO> replies = new ArrayList<>();

    @Expose
    private Boolean apruve;
    @Expose
    private Date apruvedate;

    // DTO для автора замість повного Clientdetail
    @Expose
    private AuthorDTO author;
}