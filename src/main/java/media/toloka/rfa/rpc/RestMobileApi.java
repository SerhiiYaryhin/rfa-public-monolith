package media.toloka.rfa.rpc;


import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import media.toloka.rfa.radio.model.Post;
import media.toloka.rfa.radio.model.PostCategory;
import media.toloka.rfa.radio.model.enumerate.EPostCategory;
import media.toloka.rfa.radio.post.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@Profile("mapi")
@CrossOrigin
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

    @ToString(includeFieldNames=true)
    @Getter
    @Setter
    private class LGroup {
        private Integer count;
        private PostCategory category;
        private String label;
        private Boolean rootPage;
        private String uuid;
        private List<PostCategory> child;
    }

    //
    @RequestMapping (value = "/mapi/1.0/public/getpostcategory", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
//    @RequestMapping (value = "/mapi/1.0/public/getpostcategory", method = RequestMethod.GET, /*produces = MediaType.APPLICATION_JSON_VALUE*/ produces = "application/json;charset=utf-8")
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
            post.setCoverstoreuuid(null);
//            post.setStore(null);
            post.setPostbody(null);
            set1Posts.add(post);
        }
        return set1Posts;
    }

    @RequestMapping (value = "/mapi/1.1/public/getpostcategory", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
//    @RequestMapping (value = "/mapi/1.0/public/getpostcategory", method = RequestMethod.GET, /*produces = MediaType.APPLICATION_JSON_VALUE*/ produces = "application/json;charset=utf-8")
    public Set<LGroup> GetPostGroup() {
        Integer key = 0;
        Set<LGroup> setPostCategory = new HashSet<>();
        for (PostCategory category : postService.getPostCategory()) {
            if (category.getRootPage()) {
                LGroup egrp = new LGroup();
                egrp.setCount(key++);
                egrp.setLabel(category.getLabel());
                egrp.setRootPage(category.getRootPage());
                egrp.setCategory(null);
//                egrp.setCategory(category);
                List<PostCategory> listparent = postService.getChildPostCategory(category);
                egrp.setChild(null);
//                egrp.setChild(listparent);
                egrp.setUuid(category.getUuid());
                setPostCategory.add(egrp);
            }
            System.out.println(category);
        }
        return setPostCategory;
    }

    @RequestMapping(value = "/mapi/1.1/public/getpostbycategory/{uuid_category}", method = RequestMethod.GET, /*produces = MediaType.APPLICATION_JSON_VALUE*/ produces = "application/json;charset=utf-8")
    public @ResponseBody List<Post> GetPostsByGroupsnew(
            HttpServletResponse response,
            @PathVariable String uuid_category,
            Model model) {
        Integer key = 0;
        PostCategory postCategory = null; // = EPostCategory.POST_NEWS;
        for (PostCategory ccategory : postService.getPostCategory()) {
            if (uuid_category.equals(ccategory.getUuid())) {
                postCategory = postService.getCategoryByUUID(uuid_category);
            }
        }

        List<Post> setPosts = postService.GetPostsByCategory(postCategory);
        if (setPosts.isEmpty())
            return setPosts;
        List<Post> set1Posts = new ArrayList<>();
        for (Post post : setPosts) {
            post.setClientdetail(null);
//            post.setStore(null);
            post.setPostbody(null);
            set1Posts.add(post);
        }
        return set1Posts;
    }

}
