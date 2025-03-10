package media.toloka.rfa.radio.dropfile;

import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;



@Slf4j
@Controller
@Profile("Front")
@RequestMapping("/uploadfile")
public class DropGetFileController {

    @Autowired
    private ClientService clientService;
    final Logger logger = LoggerFactory.getLogger(DropGetFileController.class);
    @GetMapping
    public String GetUploadFile() {
        Users user = clientService.GetCurrentUser();
        // Якщо не залогінені, то переходимо на головну.
//        if (opt.isEmpty()) {
        if (user == null) {
            return "redirect:/";
        }

//        logger.info("Стартуємо завантаження файлу");
        return "/fileupload";
    }
}

