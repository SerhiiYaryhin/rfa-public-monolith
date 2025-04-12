package media.toloka.rfa.blockeditor.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import media.toloka.rfa.blockeditor.model.BlockPost;
import media.toloka.rfa.blockeditor.repository.BEPostRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Optional;

@Controller
public class BEPostController {

    private final BEPostRepository bePostRepository;

    public BEPostController(BEPostRepository postRepository) {
        this.bePostRepository = postRepository;
    }

    @GetMapping("/blockeditor/create")
    public String showForm(@NotNull Model model) {
        model.addAttribute("post", new BlockPost());
        return "/blockeditor/editor-form";
    }

    @PostMapping("/blockeditor/save")
    public String savePost(@ModelAttribute BlockPost fpost) {
        Optional<BlockPost> post = bePostRepository.findById(1L);
        if (post.isPresent()) {
            BlockPost lpost = post.get();
            lpost.setContent(fpost.getContent());
            bePostRepository.save(lpost);
        } else {
            bePostRepository.save(fpost);
        }
        return "redirect:/blockeditor/posts";
    }

    @GetMapping("/blockeditor/posts")
    public String showPosts(@NotNull Model model) {
        model.addAttribute("posts", bePostRepository.findAll());
        return "/blockeditor/post-list";
    }

    @GetMapping("/blockeditor/edit/{id}")
    public String editPost(@PathVariable Long id, Model model) {
        Optional<BlockPost> post = bePostRepository.findById(id);
        if (post.isPresent()) {
            model.addAttribute("post", post.get());
            ObjectMapper objectMapper = new ObjectMapper();
            String contentJson;
            try {
                contentJson = objectMapper.writeValueAsString(post.get().getContent());
            } catch (JsonProcessingException e) {
                System.err.println("Error serializing post content: " + e.getMessage());
                // Можна задати fallback значення для контенту
                contentJson = "{\"error\": \"Failed to serialize content\"}";
            }

            model.addAttribute("postContentJson", contentJson);

            return "/blockeditor/editor-form";
        } else {
            return "redirect:/blockeditor/posts";
        }
    }

    @GetMapping("/blockeditor/view/{id}")
    public String viewPost(@PathVariable Long id, Model model) {
        Optional<BlockPost> post = bePostRepository.findById(id);
        if (post.isPresent()) {
            model.addAttribute("post", post.get());

            ObjectMapper objectMapper = new ObjectMapper();
            String contentJson;
            try {
                contentJson = objectMapper.writeValueAsString(post.get().getContent());
            } catch (JsonProcessingException e) {
                System.err.println("Error serializing post content: " + e.getMessage());
                // Можна задати fallback значення для контенту
                contentJson = "{\"error\": \"Failed to serialize content\"}";
            }

            model.addAttribute("postContentJson", contentJson);

            return "/blockeditor/post-view";
        } else {
            return "redirect:/blockeditor/posts";
        }
    }
}
