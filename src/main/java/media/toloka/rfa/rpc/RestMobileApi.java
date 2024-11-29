package media.toloka.rfa.rpc;


import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import media.toloka.rfa.radio.model.Post;
import media.toloka.rfa.radio.model.enumerate.EPostCategory;
import media.toloka.rfa.radio.post.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
//@RequestMapping(path="/mapi",produces="application/json")
public class RestMobileApi {

    @Autowired
    private PostService postService;

    @ToString(includeFieldNames=true)
    @Getter
    @Setter
    private class GroupEnum {
        private Integer count;
        private EPostCategory category;
        private String label;
        private Boolean rootPage;

    }
//
    @RequestMapping ("/mapi/1.0/public/getpostcategory") //, consumes = "application/json", produces = "application/json")
    public Set<GroupEnum> GetGroupsinPosts() {
        Integer key = 0;
        Set<GroupEnum> setEPostCategory = new HashSet<>();
        for (EPostCategory category : EnumSet.allOf(EPostCategory.class)) {
            if (category.rootPage) {
                GroupEnum egrp = new GroupEnum();
                egrp.setCount(key++);
                egrp.setLabel(category.label);
                egrp.setRootPage(category.rootPage);
                egrp.setCategory(category);
                setEPostCategory.add(egrp);
            }
            System.out.println(category);
        }
        return setEPostCategory;
    }

    ///mapi

//    @RequestMapping ("/mapi/1.0/public/getpostbycategory/{category}") //, consumes = "application/json", produces = "application/json")
    @RequestMapping(value = "/mapi/1.0/public/getpostbycategory/{category}", method = RequestMethod.GET, /*produces = MediaType.APPLICATION_JSON_VALUE*/ produces = "application/json;charset=utf-8")
    public @ResponseBody List<Post> GetPostsByGroups(
            HttpServletResponse response,
            @PathVariable String category,
            Model model) {
        Integer key = 0;
        EPostCategory postCategory = null; // = EPostCategory.POST_NEWS;
        for (EPostCategory ccategory : EnumSet.allOf(EPostCategory.class)) {
            if (category.equals(ccategory.toString())) postCategory = ccategory;
        }
        List<Post> setPosts = postService.GetPostsByCategory(postCategory);
        if (setPosts.isEmpty())
            return setPosts;
        List<Post> set1Posts = new ArrayList<>();
        for (Post post : setPosts) {
            post.setClientdetail(null);
            post.setStore(null);
            set1Posts.add(post);
        }
        return set1Posts;
    }


}
