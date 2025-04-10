package media.toloka.rfa.blockeditor.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import media.toloka.editor.model.Post;
import media.toloka.editor.repository.PostRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Optional;

@Controller
public class PostController {

    private final PostRepository postRepository;

    public PostController(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @GetMapping("/create")
    public String showForm(Model model) {
        model.addAttribute("post", new Post());
        return "editor-form";
    }

    @PostMapping("/save")
    public String savePost(@ModelAttribute Post fpost) {
        Optional<Post> post = postRepository.findById(1L);
        if (post.isPresent()) {
            Post lpost = post.get();
            lpost.setContent(fpost.getContent());
            postRepository.save(lpost);
        } else {
            postRepository.save(fpost);

        }
        return "redirect:/posts";
    }

    @GetMapping("/posts")
    public String showPosts(Model model) {
        model.addAttribute("posts", postRepository.findAll());
        return "post-list";
    }

    @GetMapping("/edit/{id}")
    public String editPost(@PathVariable Long id, Model model) {
        Optional<Post> post = postRepository.findById(id);
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

            return "editor-form";
        } else {
            return "redirect:/posts";
        }
    }

    @GetMapping("/view/{id}")
    public String viewPost(@PathVariable Long id, Model model) {
        Optional<Post> post = postRepository.findById(id);
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

            return "post-view";
        } else {
            return "redirect:/posts";
        }
    }
}



//
//import media.toloka.editor.model.Post;
//import media.toloka.editor.repository.PostRepository;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.*;
//
//@Controller
//
//
//
//public class PostController {
//
//    private final PostRepository postRepository;
//
//    public PostController(PostRepository postRepository) {
//        this.postRepository = postRepository;
//    }
//
//    @GetMapping("/create")
//    public String showForm(Model model) {
//        model.addAttribute("post", new Post());
//        return "editor-form";
//    }
//
//    @PostMapping("/save")
//    public String savePost(@ModelAttribute Post post) {
//        postRepository.save(post);
//        return "redirect:/posts";
//    }
//
//    @GetMapping("/posts")
//    public String showPosts(Model model) {
//        model.addAttribute("posts", postRepository.findAll());
//        return "post-list";
//    }
//}