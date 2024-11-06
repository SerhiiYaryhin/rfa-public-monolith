package media.toloka.rfa.tetegrambot.handler.impl.createlogin;

import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.email.service.EmailSenderService;
import media.toloka.rfa.radio.login.service.TokenService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Mail;
import media.toloka.rfa.security.model.Roles;
import media.toloka.rfa.security.model.Users;
import media.toloka.rfa.tetegrambot.enums.ConversationState;
import media.toloka.rfa.tetegrambot.handler.UserRequestHandler;
import media.toloka.rfa.tetegrambot.helper.KeyboardHelper;
import media.toloka.rfa.tetegrambot.model.UserRequest;
import media.toloka.rfa.tetegrambot.model.UserSession;
import media.toloka.rfa.tetegrambot.service.TelegramService;
import media.toloka.rfa.tetegrambot.service.UserSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static media.toloka.rfa.security.model.ERole.ROLE_TELEGRAM;
import static media.toloka.rfa.tetegrambot.constant.Constants.BTN_SEND_YES;

@Slf4j
@Component
public class LoginS7CheckReplyEnteredHandler extends UserRequestHandler {

    @Autowired
    private ClientService clientService;
    @Autowired
    private TokenService serviceToken;
    @Autowired
    private EmailSenderService emailSenderService;

    @Autowired
    private SpringTemplateEngine templateEngine;

    private final TelegramService telegramService;
    private final KeyboardHelper keyboardHelper;
    private final UserSessionService userSessionService;
//    private final

    public LoginS7CheckReplyEnteredHandler(TelegramService telegramService, KeyboardHelper keyboardHelper, UserSessionService userSessionService) {
        this.telegramService = telegramService;
        this.keyboardHelper = keyboardHelper;
        this.userSessionService = userSessionService;
    }
    public boolean isApplicable(UserRequest userRequest) {
//        return isTextMessage(userRequest.getUpdate(), BTN_TO_RFA_REGISTERS);
        return isTextMessage(userRequest.getUpdate())
                && ConversationState.WAITING_LOGIN_CHECK_REPLY.equals(userRequest.getUserSession().getState());
    }

    @Override
    public void handle(UserRequest userRequest) {

        UserSession session = userRequest.getUserSession();
        session.setUserId(userRequest.getUserSession().getUserId());
        if (userRequest.getUpdate().getMessage().getText().equals(BTN_SEND_YES)) {

            telegramService.sendMessage(userRequest.getChatId(),
                    "Все правильно. \nВідправляємо Вам листа для встановлення паролю.");
            // Вся Інформація корректна
            // шукаємо користувача серед зареєстрованих
            Users potencialUser = clientService.GetUserByEmail(session.getUserEmail());
            if (potencialUser == null) {
                // не знайшли цей e-mail у базі
                potencialUser = new Users();
                Roles role = new Roles();
                role.setRole(ROLE_TELEGRAM);
                potencialUser.setRoles(new ArrayList<Roles>());
                potencialUser.getRoles().add(role);
                potencialUser.setPassword("*");
                potencialUser.setEmail(session.getUserEmail());
                clientService.CreateClientsDetail(potencialUser,session.getUserFName(),session.getUserSName());
                potencialUser.getClientdetail().setTelegramuser(userRequest.getUpdate().getMessage().getFrom().getId().toString());
                potencialUser.getClientdetail().setTelegramuserchatid(userRequest.getChatId().toString());;
                clientService.SaveUser(potencialUser);
                String token = UUID.randomUUID().toString();
                serviceToken.createVerificationToken(potencialUser, token);

                // формуємо повідомлення для встановлення паролю
                Mail mail;
                mail = new Mail();
                mail.setTo(potencialUser.getEmail());
                mail.setFrom("info@toloka.kiev.ua");
                mail.setSubject("Радіо для Всіх! Підтвердження реєстрації на порталі \"Радіо для Всіх!\".");
                Map<String, Object> map1 = new HashMap<String, Object>();
//                map1.put("name",(Object) userDTO.getEmail());
                map1.put("name", (Object) potencialUser.getClientdetail().getCustname() + " " + potencialUser.getClientdetail().getCustsurname()); // сформували імʼя та призвище для листа
//            map1.put("name", (Object) "УВАГА!!! Штучно Сформоване імʼя"); // сформували імʼя та призвище для листа
                map1.put("confirmationUrl", (Object) "https://rfa.toloka.media/login/setUserPassword?token=" + token); // сформували для переходу адресу з токеном
                mail.setHtmlTemplate(new Mail.HtmlTemplate("/mail/registerTelegramSetPassword", map1)); // заповнили обʼєкт для відсилання пошти
                try {
                    emailSenderService.sendEmail(mail);
                    telegramService.sendMessage(userRequest.getChatId(),
                            "Лист з інструкціями для встановлення паролю надіслано на Вашу пошту."
                                    +"\nБудь ласка, виконайте необхідні дії і встановіть пароль для можливості повноцінного користування порталом.");
                } catch (MessagingException e) {
                    telegramService.sendMessage(userRequest.getChatId(),
                            "Щось пішло не так :(\nПеревірте введену поштову адресу або зверніться до нас."
                                    +"\nПомилка.\nПочинаємо з початку.");
                    throw new RuntimeException(e);
                }
            } else {
            // користувач з такою поштою вже зареєстрований на порталі
                telegramService.sendMessage(userRequest.getChatId(),
                        " Будь ласка привʼяжіть свій обликовий запис на порталі до свого телеграму.");

                telegramService.sendMessage(userRequest.getChatId(),
                        "На порталі вже є користувач з такою поштою."
                                +"\nЗайдіть на портал. На сторінці Вашого профайлу Ви знайдете унікальний ключ для привʼязки до телеграму."
                                +"\nСкопіюйте його і привʼяжіть свій аккаунт на порталі до телеграму."
                                +"Якщо Ви втратили пароль, то відновіть його - це дуже просто \uD83D\uDE0A"
                                +"\nПочинаємо з початку.");
            }
        } else {
            // помилка в інформації
            telegramService.sendMessage(userRequest.getChatId(),
                    "Починаємо з початку.");
        }

        // Встановлюємо стан для наступного кроку
        session.setState(ConversationState.CONVERSATION_STARTED);
        // Зберігаємо в статусі користувача сеанс та користувача
        userSessionService.saveSession(userRequest.getChatId(), session);
        // Повідомлення про наступний крок
        if (clientService.GetUserFromTelegram(userRequest.getUpdate().getMessage().getFrom().getId().toString()) != null) {
            ReplyKeyboard replyKeyboard = keyboardHelper.buildMainMenu();
            telegramService.sendMessage(userRequest.getChatId(),
                    "\uD83D\uDC4BПривіт! За допомогою цього чат-бота ви зможете взаємодіяти з порталом \"Радіо для всіх!\""
                            +" надіслати свої треки, додати опис треку, надіслати повідомлення в прямий ефір Студії Толока і багато всього іншого.",
                    replyKeyboard);
        }
        else {
            ReplyKeyboard replyKeyboard = keyboardHelper.buildRegisterMenu();
            telegramService.sendMessage(userRequest.getChatId(),
                    "\uD83D\uDC4BПривіт! За допомогою цього чат-бота ви зможете взаємодіяти з порталом \"Радіо для всіх!\""
                            +" надіслати свої треки, додати опис треку, надіслати повідомлення в прямий ефір Студії Толока і багато всього іншого.",
                    replyKeyboard);
            telegramService.sendMessage(userRequest.getChatId(),
                    "Для цього Ви повинні бути зареєстровані на порталі https://rfa.toloka.media/ "
                            +"та привʼязати свій Телеграм до облікового запису на порталі.");
        }
        telegramService.sendMessage(userRequest.getChatId(),
                "Обирайте з меню нижче ⤵️");
    }

    @Override
    public boolean isGlobal() {
        return true;
    }
}
