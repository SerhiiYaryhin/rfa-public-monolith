package media.toloka.rfa.radio.login;

// робота з поштою https://nuwanthafernando95.medium.com/spring-boot-email-template-with-thymeleaf-4f21ca437b52
// реєстрація користувача https://www.baeldung.com/registration-verify-user-by-email

//import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Mail;
import media.toloka.rfa.radio.email.service.EmailSenderService;
import media.toloka.rfa.radio.history.service.HistoryService;
import media.toloka.rfa.radio.model.Token;
import media.toloka.rfa.radio.login.service.TokenService;
import media.toloka.rfa.radio.root.RootController;
import media.toloka.rfa.security.model.ERole;
import media.toloka.rfa.security.model.Roles;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessagingException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.*;

import static media.toloka.rfa.radio.model.enumerate.EHistoryType.History_UserSendMailSetPassword;
import static media.toloka.rfa.radio.model.enumerate.EHistoryType.History_UserSetPassword;
import static media.toloka.rfa.security.model.ERole.*;

//import static media.toloka.rfa.radio.model.EHistoryType.*;
//import static media.toloka.rfa.security.model.ERole.*;
//import static media.toloka.rfa.security.UserDetailsService.model.ERole.ROLE_USER;


@Controller
public class UserLoginController {

//    @Autowired
//    private JavaMailSender emailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;
    @Autowired
    private ClientService clientService;

    @Autowired
    private HistoryService historyService;


    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private EmailSenderService emailSenderService;

    @Autowired
    private TokenService serviceToken;

    Logger logger = LoggerFactory.getLogger(UserLoginController.class);

//    @Data
    @Getter
    @Setter
    public class formUser {
        String email;
        String custname;
        String custsurname;
        String group;
    }

    @Getter
    @Setter
    public class lineChooseRole {
        ERole type;
        String value;
    }
    @Getter
    @Setter
    public class listLineChooseRole {
        List<lineChooseRole> lChooseRole = new ArrayList<>();
        ERole value;
    }



    @GetMapping(value = "/login/route")
    public String userRouter (
            HttpServletRequest request,
            Model model
    ) {
        logger.info("============ ROUTE to Group page ");

        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        String remoteAddr = request.getHeader("X-FORWARDED-FOR");
        if (remoteAddr == null || "".equals(remoteAddr)) {
            remoteAddr = request.getRemoteAddr();
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        logger.info("IP={} Користувач {} {} company: {}",remoteAddr,cd.getCustname(),cd.getCustsurname(),cd.getFirmname());
//        String remip = request.getRemoteAddr();

        if (clientService.checkRole(ROLE_ADMIN)) {
            return "redirect:/admin/home";
        } else if (clientService.checkRole(ROLE_MODERATOR)) {
            return "redirect:/moderator/home";
//            return "redirect:/moderator/home";
        } else if (clientService.checkRole(ROLE_EDITOR)) {
            return "redirect:/editor/home";
        } else if (clientService.checkRole (ROLE_CREATER)) {
            return "redirect:/creater/home";
        } else if (clientService.checkRole (ROLE_USER)) {
            return "redirect:/user/user_page";
        } else if (clientService.checkRole(ROLE_UNKNOWN)) {
            return "redirect:/messenger";
        } else if (clientService.checkRole(ROLE_TELEGRAM)) {
            if (user.getRoles().size() == 1) {
                // Тільки одна роль у списку
                // Тож, потрібно зробити вибір між ROLE_CREATER та ROLE_USER
                return "redirect:/login/setrole";
            }
        } ;
        // Йой! Щлсь пішло не так
        logger.info("============ redirect to Logout page");
        return "redirect:/logout";
    }
    // Якщо користуач зареєструвався через телеграм, то він повинен обрати між радіо та музикантом.
    // ROLE_CREATER та ROLE_USER
    @GetMapping("/login/setrole")
    public String registerChooseRoleUser(
            Model model
    ) {
        listLineChooseRole lradio = new listLineChooseRole();
//        List<lineChooseRole> chooserole = new ArrayList<>();
        lineChooseRole lChooseRole1 = new lineChooseRole();
        lChooseRole1.setType(ROLE_CREATER);
        lChooseRole1.setValue("Музикант");
        lradio.getLChooseRole().add(lChooseRole1);
        lineChooseRole lChooseRole2 = new lineChooseRole();
        lChooseRole2.setType(ROLE_USER);
        lChooseRole2.setValue("Радіостанція");
        lradio.getLChooseRole().add(lChooseRole2);
        model.addAttribute("chooserole", lradio);
        return "/login/setrole";
    }

    @PostMapping("/login/setrole")
    public String chooseUserTypeRoleRegisterTelegram(
            @ModelAttribute listLineChooseRole chooserole,
            Model model
    ) {
        logger.info("==========: {}",chooserole.getValue());
        Users user = clientService.GetCurrentUser();
        Roles roles = new Roles();
        roles.setRole(chooserole.getValue());
        user.getRoles().add(roles);
        clientService.SaveUser(user);
        return "redirect:/login/route";
    }

    @GetMapping("/login/registerRadioUser")
    public String registerRadioUser(
            Model model
    ) {
        // створюємо List Для радіобутонів

        model.addAttribute("user", new Users());
        return "/login/registerRadioUser";
    }



    @GetMapping("/login/registerCreater")
    public String registerCreater(
            Model model
    ) {
        // створюємо List Для радіобутонів

        model.addAttribute("user", new Users());
        return "/login/registerCreater";
    }

    @PostMapping("/login/registerRadioUser")
    public String saveRegisterUser(
            @ModelAttribute formUser formuser,
            Model model
    ) {
        // Забираємо з форми email користувача
        String email = formuser.getEmail();
        Users newUser = clientService.GetUserByEmail(email);

        // перевіряємо, чи є цей емайл в базі
        if (newUser == null) {
            newUser = new Users();
            // додали групу для користувача
            // Роль беремо зі форми. Визначається статусом радіобутона.
            Roles role = new Roles();
                    role.setRole(ROLE_USER);
                    newUser.setRoles(new ArrayList<Roles>());
                    newUser.getRoles().add(role);
            // зберігаємо користувача в базу
            newUser.setPassword("*");
            newUser.setEmail(formuser.getEmail());
            clientService.CreateClientsDetail(newUser,formuser.getCustname(),formuser.getCustsurname());
            clientService.SaveUser(newUser);
            String token = UUID.randomUUID().toString();
            serviceToken.createVerificationToken(newUser, token);


            // формуємо повідомлення для відправки поштою
            Mail mail;
            mail = new Mail();
            mail.setTo(newUser.getEmail());
            mail.setFrom("info@toloka.kiev.ua");
            mail.setSubject("Радіо для Всіх! Підтвердження реєстрації Вашої радіостанції на порталі \"Радіо для Всіх!\".");
            Map<String, Object> map1 = new HashMap<String, Object>();
//                map1.put("name",(Object) userDTO.getEmail());
                map1.put("name", (Object) newUser.getClientdetail().getCustname() + " " + newUser.getClientdetail().getCustsurname()); // сформували імʼя та призвище для листа
            map1.put("confirmationUrl", (Object) "https://rfa.toloka.media/login/setUserPassword?token=" + token); // сформували для переходу адресу з токеном
            mail.setHtmlTemplate(new Mail.HtmlTemplate("/mail/registerSetPassword", map1)); // заповнили обʼєкт для відсилання пошти
            // пробуємо надіслати
            // todo потрібно зробити нормальну обробку помилок пошти
            try {
                logger.info("Відправляємо пошту при реєстрації!");
                emailSenderService.sendEmail(mail);
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }

            // Готуємо інформацію для відображення для користувача у формі
            model.addAttribute("success", "Користувача '" + email + "' успішно збережено! Для продовження реєстрації перевірте свою пошту.");
        } else {
            // знайшли таку пошту
            // формуємо повідомлення для форми
            // Готуємо інформацію для відображення для користувача у формі
            model.addAttribute("danger", "Користувача '" + email + "' знайдено в базі.");
            String restorePSW = "Відновити пароль?";
            model.addAttribute("restorepsw", restorePSW);
        }

        return "/login/registerRadioUser";
    }

    @PostMapping("/login/registerCreater")
    public String saveCreater(
            @ModelAttribute formUser formuser,
            Model model
    ) {
        // Забираємо з форми email користувача
        String email = formuser.getEmail();

        Users newUser = clientService.GetUserByEmail(email);

        // перевіряємо, чи є цей емайл в базі
        if (newUser == null) {
            newUser = new Users();
            // додали групу для користувача
            // Роль беремо зі форми. Визначається статусом радіобутона.
            Roles role = new Roles();
                    role.setRole(ROLE_CREATER);
                    newUser.setRoles(new ArrayList<Roles>());
                    newUser.getRoles().add(role);
            // зберігаємо користувача в базу
            newUser.setPassword("*");
            newUser.setEmail(formuser.getEmail());
            clientService.CreateClientsDetail(newUser,formuser.getCustname(),formuser.getCustsurname());
            clientService.SaveUser(newUser);
            String token = UUID.randomUUID().toString();
            serviceToken.createVerificationToken(newUser, token);


            // формуємо повідомлення для відправки поштою
            Mail mail;
            mail = new Mail();
            mail.setTo(newUser.getEmail());
            mail.setFrom("info@toloka.kiev.ua");
            mail.setSubject("Радіо для Всіх! Підтвердження реєстрації Вашої радіостанції на сервісі.");
            Map<String, Object> map1 = new HashMap<String, Object>();
//                map1.put("name",(Object) userDTO.getEmail());
            map1.put("name", (Object) newUser.getClientdetail().getCustname() + " " + newUser.getClientdetail().getCustsurname()); // сформували імʼя та призвище для листа
//            map1.put("name", (Object) "УВАГА!!! Штучно Сформоване імʼя"); // сформували імʼя та призвище для листа
            map1.put("confirmationUrl", (Object) "https://rfa.toloka.media/login/setUserPassword?token=" + token); // сформували для переходу адресу з токеном
            mail.setHtmlTemplate(new Mail.HtmlTemplate("/mail/registerSetPassword", map1)); // заповнили обʼєкт для відсилання пошти
            // пробуємо надіслати
//                logger.info("ЗРОБИТИ ВІДПРАВКУ ПОШТИ!!!");
            // Відправляємо через RabbitMQ

            // потрібно зробити нормальну обробку помилок пошти
            try {
                logger.info("Відправляємо пошту при реєстрації!");
                emailSenderService.sendEmail(mail);
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }

            // Готуємо інформацію для відображення для користувача у формі
            model.addAttribute("success", "Користувача '" + email + "' успішно збережено! Для продовження реєстрації перевірте свою пошту.");
        } else { // знайшли таку пошту
            // формуємо повідомлення для форми
            // Готуємо інформацію для відображення для користувача у формі
            model.addAttribute("danger", "Користувача '" + email + "' знайдено в базі.");
            String restorePSW = "Відновити пароль?";
            model.addAttribute("restorepsw", restorePSW);
        }

        return "/login/registerRadioUser";
    }

    @GetMapping("/login/restorePsw")
    public String restorePswGet(
            Model model
    ) {
        return "/login/restorepsw";
    }

    // Відновлення паролю користувача через електронну пошту
    @PostMapping("/login/restorePsw")
    public String restorePswPost(
            @ModelAttribute Users userDTO,
            Model model
    ) {
        // Забираємо з форми email користувача
        String email = userDTO.getEmail();
        // намагаємося знайти пошту в базі кристувачів
        Users  user = clientService.GetUserByEmail(email);
        // перевіряємо, чи є цей емайл в базі
        if (user != null) {  // користувач є в базі
            // формуємо токен та зберігаємо в базу
//            Users user = opt.get();
            // формуємо повідомлення для відправки поштою
            Mail mail;
            mail = new Mail();
            mail.setTo(userDTO.getEmail());
            mail.setFrom("rfa@toloka.kiev.ua");
            mail.setSubject("Радіо для Всіх! Відновлення паролю користувача.");
            Map<String, Object> map1 = new HashMap<String, Object>();
            Token token; // = tokenRepo.findByUser(user);
            if(serviceToken.findByUser(user) == null) {
                serviceToken.createVerificationToken(user, UUID.randomUUID().toString());
            }
            token = serviceToken.findByUser(user);
            String fname;
//            fname = user.getCustname() + " " + user.getCustsurname();
//            fname = "УВАГА!!! Штучне сформоване Імʼя.";
            fname = user.getClientdetail().getCustname() + " " + user.getClientdetail().getCustsurname();
            model.addAttribute("fname", fname);
            map1.put("fname", (Object) fname); // сформували імʼя та призвище для листа
            map1.put("confirmationUrl", (Object) "https://rfa.toloka.media/login/setUserPassword?token=" + token.getToken()); // сформували для переходу адресу з токеном
            mail.setHtmlTemplate(new Mail.HtmlTemplate("/mail/restorePsw", map1)); // заповнили обʼєкт для відсилання пошти
            // TODO потрібно зробити нормальну обробку помилок пошти
            logger.info("Відправляємо лист для відновлення паролю {} userId={}",fname,user.getId());
            try {
                emailSenderService.sendEmail(mail);
                historyService.saveHistory(History_UserSendMailSetPassword, mail.getTo(), user);

            }
            catch (MessagingException e) {
                System.out.println("========================== mail MessagingException");
                model.addAttribute("msg", "На пошту '" + email + "' не надіслано лист. ");
                return "redirect:/error";
            }
            // формуємо повідомлення для форми реєстрації
            model.addAttribute("success", "На пошту '" + email + "' надіслано лист. Для продовження відновлення паролю перевірте свою пошту.");
        } else { // не знайшли таку пошту
            // формуємо повідомлення для форми
            String restorePSW = "Зареєструвати нового користувача?";
            model.addAttribute("restorepsw", restorePSW);
            model.addAttribute("danger", "Користувача '" + email + "' не знайдено в базі.");
        }
        return "/login/restorepsw";
    }

    @GetMapping("/login/setUserPassword")
    public String RegitrationConfirm(
            @RequestParam("token") String token,
            Model model
    ) {
        // Перевіряємо токен для і встановлюємо пароль для користувача
        model.addAttribute("htoken", token);
        return "/login/setUserPassword";
    }

    @PostMapping("/login/setUserPassword")
    public String RegitrationSavePassword(
            @RequestParam("psw") String psw,
            @RequestParam("htoken") String token,
            Model model
    ) {
        // Перевіряємо термін дії токена і встановлюємо пароль для користувача
        Token myToken = serviceToken.findByToken(token);
        if (myToken != null) {
            //TODO перевірити термін дії токена. якщо протерміновано, то вивести повідомлення і запропонувати повторити
            Users myuser = myToken.getUser();
            // TODO перевірити чи не залочено користувача
            myuser.setPassword(passwordEncoder.encode(psw));
            clientService.SaveUser(myuser);
            historyService.saveHistory(History_UserSetPassword, "Встановили новий пароль", myuser);
            serviceToken.delete(myToken);
        } else {
            // токен не найден
            // формуємо сторінку з рекомендаціями що робити.
            // 1. Ви помилилися при копіювання строки з адресою
            // 2. Посилання застаріле. Спробуйте відновити пароль
            System.out.println("========================== Set User Password: Token not found. ");
            model.addAttribute("msg", "Унікального посилання не знайдено. Спробуйте ще раз, або зверніться до адміністратора сервісу: rfa.toloka.kiev.ua");
            return "redirect:/error";
        }
        // світимо результат запису пароля і якщо все гаразд, то пропонуємо ввійти в систему
        return "redirect:/login";
    }

    @PostMapping("/login/saveUser")
    public String saveUser(
            @ModelAttribute Users user,
            Model model
    ) { // тут напевно робота з поштою
        clientService.SaveUser(user);
        String message = "Користувача '" + user.getEmail() + "' збережено!";
        model.addAttribute("msg", message);
        return "/login/registerUser";
    }

    @GetMapping(value = "/login")
    public String login() {
        return "/login/login";
    }

    @PostMapping("/login")
    public String loginUser(
            @ModelAttribute Users user,
            Model model
    ) {
//        System.out.println("========================== Login");
        return "redirect:/user/user";
    }
}