package media.toloka.rfa.tetegrambot.helper;

import media.toloka.rfa.radio.client.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.List;

import static media.toloka.rfa.tetegrambot.constant.Constants.*;


//import static org.vladyka.constant.Constants.BTN_CANCEL;

/**
 * Helper class, allows to build keyboards for users
 */
@Component
@Profile("Telegram")
public class KeyboardHelper {

    public ReplyKeyboardMarkup buildCitiesMenu(List<String> cities) {
        List<KeyboardButton> buttons = List.of(
                new KeyboardButton("Київ"),
                new KeyboardButton("Львів"));
        KeyboardRow row1 = new KeyboardRow(buttons);

        KeyboardRow row2 = new KeyboardRow(List.of(new KeyboardButton(BTN_CANCEL)));

        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2))
                .selective(true)
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .build();
    }

    public ReplyKeyboardMarkup buildMainMenu() {
        List<KeyboardButton> buttons = List.of(
                new KeyboardButton("Надіслати трек"),
                new KeyboardButton("Надіслати опис до треку"));
        KeyboardRow row1 = new KeyboardRow(buttons);
        KeyboardRow row5 = new KeyboardRow(List.of(
                new KeyboardButton(BTN_SEND_TO_CHAT),
                new KeyboardButton(BTN_LISTEN_RADIO))
        );
        KeyboardRow row2 = new KeyboardRow(List.of(new KeyboardButton("Перелік треків")));
        KeyboardRow row3 = new KeyboardRow(List.of(new KeyboardButton(BTN_CANCEL)));
        KeyboardRow row4 = new KeyboardRow(List.of(new KeyboardButton("❗️Потрібна допомога")));

        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(row1, row5,row2, row3, row4))
                .selective(true)
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .build();

//        KeyboardRow keyboardRow = new KeyboardRow();
//        keyboardRow.add(" Надіслати трек");
//        keyboardRow.add(" Надіслати опис до треку");
//        keyboardRow.add("❗️Потрібна допомога");
//
//        return ReplyKeyboardMarkup.builder()
//                .keyboard(List.of(keyboardRow))
//                .selective(true)
//                .resizeKeyboard(true)
//                .oneTimeKeyboard(false)
//                .build();
    }

    public ReplyKeyboardMarkup buildMenuWithCancel() {
        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add(BTN_CANCEL);

        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(keyboardRow))
                .selective(true)
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .build();
    }

    public ReplyKeyboard buildRegisterMenu() {
        List<KeyboardButton> buttons = List.of(
                new KeyboardButton(BTN_TO_RFA_REGISTERS),
                new KeyboardButton(BTN_TELEGRAM_LINK));
        KeyboardRow row1 = new KeyboardRow(buttons);

        KeyboardRow row3 = new KeyboardRow(List.of(new KeyboardButton(BTN_CANCEL)));
        KeyboardRow row4 = new KeyboardRow(List.of(new KeyboardButton("❗️Потрібна допомога")));

        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(row1, row3, row4))
                .selective(true)
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .build();
    }

    public ReplyKeyboardMarkup buildYesNoCancelMenu() {
        List<KeyboardButton> buttons = List.of(
                new KeyboardButton("Так"),
                new KeyboardButton("Ні"));
        KeyboardRow row1 = new KeyboardRow(buttons);
        KeyboardRow row2 = new KeyboardRow(List.of(new KeyboardButton(BTN_CANCEL)));

        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2))
                .selective(true)
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .build();
    }

}
