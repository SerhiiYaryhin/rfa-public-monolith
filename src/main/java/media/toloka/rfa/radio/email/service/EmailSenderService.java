package media.toloka.rfa.radio.email.service;


//import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import media.toloka.rfa.radio.model.Mail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
//@RequiredArgsConstructor
public class EmailSenderService {
//    https://www.baeldung.com/spring-email

    Logger logger = LoggerFactory.getLogger(EmailSenderService.class);
    @Autowired
    private JavaMailSenderImpl emailSender;
//
    @Autowired
    private SpringTemplateEngine templateEngine;

    @Value("${media.toloka.rfa..mail.smtphost}")
    private String mailhost;
    @Value("${media.toloka.rfa.mail.smtpport}")
    private int mailport;
    @Value("${media.toloka.rfa.mail.protocol}")
    private String  mailprotocol;
    @Value("${media.toloka.rfa.mail.defaultencoding}")
    private String maildefaultencoding;

    /**
     * Отримує список імен усіх HTML шаблонів у папці templates/mail/
     */
    public List<String> getAvailableTemplates() {
        try (Stream<Path> paths = Files.walk(Paths.get("src/main/resources/templates/mail"))) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString().replace(".html", ""))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Помилка читання каталогу шаблонів: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Читає сирий HTML вміст шаблону для завантаження в редактор
     */
    public String getTemplateRawContent(String templateName) {
        try {
            Path path = Paths.get("src/main/resources/templates/mail/" + templateName + ".html");
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Помилка читання файлу шаблону {}: {}", templateName, e.getMessage());
            return "";
        }
    }

    /**
     * Відправка листа на основі вже відредагованого HTML контенту (без обробки Thymeleaf)
     */
    public void sendRawHtmlEmail(String to, String subject, String htmlBody) {
        JavaMailSenderImpl emailSender = new JavaMailSenderImpl();
        emailSender.setHost(mailhost);
        emailSender.setPort(mailport);
        emailSender.setProtocol(mailprotocol);
        emailSender.setDefaultEncoding(maildefaultencoding);

        jakarta.mail.internet.MimeMessage message = emailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setFrom(mailhost.contains("iw") ? "info@toloka.media" : "info@toloka.media"); // todo: use config
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            emailSender.send(message);
        } catch (Exception e) {
            logger.error("Помилка відправки Raw Email до {}: {}", to, e.getMessage());
        }
    }

    public void sendEmail(Mail mail) throws MessagingException //, IOException
    {
        JavaMailSenderImpl emailSender = new JavaMailSenderImpl();
        emailSender.setHost(mailhost);
        emailSender.setPort(mailport);
        emailSender.setProtocol(mailprotocol);
        emailSender.setDefaultEncoding(maildefaultencoding);

//        emailSender.setJavaMailProperties();
        jakarta.mail.internet.MimeMessage message = emailSender.createMimeMessage();

        MimeMessageHelper helper;
        try {
            helper = new MimeMessageHelper(message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());
            String html = getHtmlContent(mail); // тут отримуємо текст після обробки THYMELEAF
            helper.setTo(mail.getTo());
            helper.setFrom(mail.getFrom());
            helper.setSubject(mail.getSubject());
            helper.setText(html, true);
        } catch (MessagingException e) {
            logger.info("Щось пішло не так при створенні хелпера. :(");
        } catch (jakarta.mail.MessagingException e) {
            logger.info("Щось пішло не так при створенні хелпера. :(");
//            throw new RuntimeException(e);
        }

        emailSender.send(message);
    }

    private String getHtmlContent(Mail mail) {
        Context context = new Context();
        context.setVariables(mail.getHtmlTemplate().getProps());
        return templateEngine.process(mail.getHtmlTemplate().getTemplate(), context);
    }

    public String getTextContent(String template, Map<String, Object> props) {
        Context context = new Context();
        context.setVariables(props); //
//        templateEngine.
        return templateEngine.process(template, context);
    }

}