package media.toloka.rfa.comments.config;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import media.toloka.rfa.comments.model.Comment; // Імпортуйте вашу модель Comment

public class CommentExclusionStrategy implements ExclusionStrategy {

    @Override
    public boolean shouldSkipField(FieldAttributes f) {
        // Якщо поле називається "parentComment" і воно знаходиться в класі Comment,
        // то ми пропускаємо його серіалізацію.
        if (f.getDeclaringClass() == Comment.class && f.getName().equals("parentComment")) {
            return true;
        }
        // Можливо, вам також потрібно виключити поле "author", якщо воно викликає цикл у Clientdetail
        // або якщо вам не потрібна повна інформація про автора в коментарі.
        if (f.getDeclaringClass() == Comment.class && f.getName().equals("author")) {
            return true; // Пропускаємо серіалізацію поля "author"
        }
        // Ви також можете вирішити пропускати "replies" у певних сценаріях,
        // але для ієрархії коментарів це зазвичай не потрібно, оскільки цикл розривається на батькові.
        // if (f.getDeclaringClass() == Comment.class && f.getName().equals("replies")) {
        //     return true;
        // }
        return false; // В іншому випадку, серіалізуємо поле
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        return false; // Не пропускаємо жоден клас повністю
    }
}
