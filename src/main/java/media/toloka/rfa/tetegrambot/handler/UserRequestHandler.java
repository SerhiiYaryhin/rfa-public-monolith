package media.toloka.rfa.tetegrambot.handler;


import media.toloka.rfa.tetegrambot.model.UserRequest;
import org.springframework.context.annotation.Profile;
import org.telegram.telegrambots.meta.api.objects.Update;
@Profile("Telegram")
public abstract class UserRequestHandler {

    public abstract boolean isApplicable(UserRequest request);
    public abstract void handle(UserRequest dispatchRequest);
    public abstract boolean isGlobal();

    public boolean isCommand(Update update, String command) {
        return update.hasMessage() && update.getMessage().isCommand()
                && update.getMessage().getText().equals(command);
    }

    public boolean isTextMessage(Update update) {
        return update.hasMessage() && update.getMessage().hasText();
    }

    public boolean isAudioMessage(Update update) {
        return update.hasMessage() && update.getMessage().hasAudio();
    }

    public boolean isDocumentMessage(Update update) {
        return update.hasMessage() && update.getMessage().hasDocument();
    }


    public boolean isTextMessage(Update update, String text) {
        return update.hasMessage() && update.getMessage().hasText() && update.getMessage().getText().equals(text);
    }
}