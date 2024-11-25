package media.toloka.rfa.rpc;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import media.toloka.rfa.radio.model.enumerate.EPostCategory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class RestMobileApi {

    @ToString(includeFieldNames=true)
    @Getter
    @Setter
    private class GroupEnum {
        private String count;
        private EPostCategory category;
        private String label;
        private Boolean rootPage;

    }

    @GetMapping ("/mapi/1.0/public/getpostcategory") //, consumes = "application/json", produces = "application/json")
    public Set<String> GetGroupsinPosts() {
        Integer key = 0;
        Set<String> setEPostCategory = new HashSet<>();
        for (EPostCategory category : EnumSet.allOf(EPostCategory.class)) {
            if (category.rootPage) setEPostCategory.add(
                    category.toString());
            System.out.println(category);
        }
        return setEPostCategory;
    }

    @GetMapping ("/mapi/1.0/public/getpostcategorymap") //, consumes = "application/json", produces = "application/json")
    public Map<Integer, GroupEnum> GetGroupsinPostsMap() {
        Integer key = 0;
        Map<Integer, GroupEnum> mapEPostCategory = new HashMap();
        for (EPostCategory category : EnumSet.allOf(EPostCategory.class)) {
            if (category.rootPage) {
                GroupEnum tgrp = new GroupEnum();
                tgrp.setCategory(category);
                tgrp.setCount(key.toString());
                tgrp.setLabel(category.label);
                tgrp.setRootPage(category.rootPage);
                mapEPostCategory.put(key++, tgrp);
            }
//                    category.toString());
            System.out.println(category);
        }
        return mapEPostCategory;
    }
}