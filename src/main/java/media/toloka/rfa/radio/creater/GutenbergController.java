package media.toloka.rfa.radio.creater;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Profile("Front")
@Controller
public class GutenbergController {

    private String savedContent = "<p>Це збережений текст</p>"; // Імітація збереженого контенту

    @GetMapping("/creater/getenberg")
    public String showEditor(Model model) {
        model.addAttribute("savedContent", savedContent); // Передаємо контент у шаблон
        return "/creater/getenberg"; // Завантажує editor.html
    }

    @PostMapping("/creater/gsave")
    public String saveContent(@RequestParam String content, Model model) {
        savedContent = content; // Оновлюємо збережений контент
        model.addAttribute("savedContent", savedContent);
        return "redirect:/creater/getenberg";
    }
}