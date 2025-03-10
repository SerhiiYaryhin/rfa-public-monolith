package media.toloka.rfa.radio.admin;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import media.toloka.rfa.radio.admin.service.AdminService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.history.service.HistoryService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Post;
import media.toloka.rfa.rpc.service.RPCService;
import media.toloka.rfa.security.model.ERole;
import media.toloka.rfa.security.model.Roles;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static media.toloka.rfa.radio.model.enumerate.EHistoryType.History_UserSendMailSetPassword;
import static media.toloka.rfa.radio.model.enumerate.EHistoryType.History_UserSuspend;
import static media.toloka.rfa.security.model.ERole.*;

@Profile("Front")
@Controller
public class AdminUser {

    final Logger logger = LoggerFactory.getLogger(AdminUser.class);


    @Autowired
    private AdminService adminService;
    @Autowired
    private ClientService clientService;
    @Autowired
    private HistoryService historyService;

    @Data
    public class SS {
        private String searchString;
//        public SS (String ggg) {
//            this.searchString = ggg;
//        }
    }

    // Пошук по користувачам. Ключем виступає значення текстового поля

    @PostMapping(value = "/admin/users")
    public String GetSearchUserList(
            @ModelAttribute("searchString") SS ss,
            HttpServletRequest request,
            HttpServletResponse response,
//            @RequestParam(value = "searchString") String searchString,
            Model model
    ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        // витягуємо рядок для пошуку
        // https://www.geeksforgeeks.org/spring-boot-build-a-dynamic-full-text-search-api-using-jpa-queries/
        logger.info("Рядок для пошуку ="+ss.getSearchString());

        model.addAttribute("ss", ss );
        List<Users> usersList = adminService.GetSearchUsers(ss.getSearchString());
        // todo тут додати сортування для роботи адміна з переліком клієнтів
        model.addAttribute("usersList", usersList );
//        return "redirect:/admin/users";
        return "/admin/users";
    }

    // витягуємо перелік всіх користувачів.
    @GetMapping(value = "/admin/users")
    public String getAdmiUser(
//            @ModelAttribute("ss") SS ss,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
//        Objects ma = model.getAttribute("ss");
        SS ss = (SS) model.asMap().get("ss");
        List<Users> usersList = (List<Users>) model.asMap().get("ss");
        if (ss == null) {
            ss = new SS();
            ss.setSearchString("");
            usersList = adminService.GetAllUsers();
        } else {
            usersList = adminService.GetSearchUsers(ss.getSearchString());
        }
        String searchString = new String();
        // todo тут додати сортування для роботи адміна з переліком клієнтів

        model.addAttribute("usersList", usersList );
        model.addAttribute("ss", ss );
        return "/admin/users";
    }

    // намагаємося видалити користувача
    @GetMapping(value = "/admin/userdel/{iduser}")
    public String getAdminDelUser(
            @PathVariable Long iduser,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        // todo перевірити на приналежність до групи адмінів.

        Users curuser = adminService.GetUsersById(iduser);
        List< Roles> rolesList = curuser.getRoles();
        rolesList.clear();
        // Обробка ClientDetail. Видаляємо всі списки

        //
//        clientService.SaveUser(curuser);
//        clientService.DeleteUser(curuser);



        List<Users> usersList = adminService.GetAllUsers();
        model.addAttribute("usersList", usersList );
        model.addAttribute("searchString", new String() );

        return "redirect:/admin/users";
    }

//    http://localhost:8080/admin/enableuser/2
    // витягуємо користувача для редагування
    @GetMapping(value = "/admin/enableuser/{iduser}")
    public String getAdminEnableUser(
            @PathVariable Long iduser,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Users curuser = adminService.GetUsersById(iduser);
        List< Roles> rolesList = curuser.getRoles();
//        rolesList.clear();
        // Обробка ClientDetail. Видаляємо всі списки

        //
        // clientService.SaveUser(curuser);
//        clientService.DeleteUser(curuser);



        List<Users> usersList = adminService.GetAllUsers();
        model.addAttribute("usersList", usersList );
        model.addAttribute("searchString", new String() );

        return "redirect:/admin/users";
    }

    // Заготовка для адміністрування груп користувачів
    @GetMapping(value = "/admin/useraddgroup/{iduser}/{idgroup}")
    public String getAdminEnableUser(
            @PathVariable Long iduser,
            @PathVariable Integer idgroup,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Users curuser = adminService.GetUsersById(iduser);
        if (clientService.checkRole (ROLE_ADMIN)) {
            List<Roles> rolesList = curuser.getRoles();
            Roles role = new Roles();
            switch (idgroup) {
                case 0:
                    role.setRole(ROLE_UNKNOWN);
                    curuser.getRoles().add(role);
                    break;
                case 1:
                    role.setRole(ROLE_USER);
                    curuser.getRoles().add(role);
                    break;
                case 2:
                    role.setRole(ROLE_CREATER);
                    curuser.getRoles().add(role);
                    break;
                case 3:
                    role.setRole(ROLE_MODERATOR);
                    curuser.getRoles().add(role);
                    break;
                case 4:
                    role.setRole(ROLE_EDITOR);
                    curuser.getRoles().add(role);
                    break;
                case 5:
                    role.setRole(ROLE_ADMIN);
                    curuser.getRoles().add(role);
                    break;
                default:
                    curuser.getRoles().clear();
            }
        } else {
            historyService.saveHistory(History_UserSuspend, "Спроба змінити групи користувача під лівим аккаунтом " , user);
            // todo заморозити користувача, який спробував змінити групу іншому користувачу
            //user.getClientdetail().
            return "redirect:/logout";
        }
        clientService.SaveUser(curuser);

        List<Users> usersList = adminService.GetAllUsers();
        model.addAttribute("usersList", usersList );
        model.addAttribute("searchString", new String() );

        return "redirect:/admin/users";
    }

}
