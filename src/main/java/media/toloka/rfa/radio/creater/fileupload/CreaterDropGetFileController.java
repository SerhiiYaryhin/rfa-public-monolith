package media.toloka.rfa.radio.creater.fileupload;


import media.toloka.rfa.radio.client.service.ClientService;
//import media.toloka.rfa.security.model.Users;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;



//@Slf4j
//@RequestMapping("/creator/trackupload")
@Profile("Front")
@Controller
public class CreaterDropGetFileController {

    @Autowired
    private ClientService clientService;

    @Value("${media.toloka.rfa.upload_directory}")
    private String PATHuploadDirectory;

    final Logger logger = LoggerFactory.getLogger(CreaterDropGetFileController.class);

    @GetMapping("/creator/trackupload")
    public String CreaterDropGetUploadTrack(
            Model model
    ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        return "/creater/trackupload";
    }

    @GetMapping("/creator/picupload")
    public String CreaterDropGetPicontroller (
            Model model
    ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        return "/creater/picupload";
    }

}

