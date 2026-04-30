package media.toloka.rfa.radio.admin;

import jakarta.servlet.http.HttpServletRequest;
import media.toloka.rfa.radio.admin.service.AdminService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.history.service.HistoryService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.security.model.ERole;
import media.toloka.rfa.security.model.Roles;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static media.toloka.rfa.radio.model.enumerate.EHistoryType.History_DocumentChange;

@Profile("Front")
@Controller
@RequestMapping("/admin/users")
public class AdminUser {

    final Logger logger = LoggerFactory.getLogger(AdminUser.class);

    @Autowired
    private AdminService adminService;
    @Autowired
    private ClientService clientService;
    @Autowired
    private HistoryService historyService;

    @GetMapping
    public String listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String filter,
            Model model) {
        
        Users admin = clientService.GetCurrentUser();
        if (admin == null) return "redirect:/login";

        Page<Users> usersPage = adminService.getUsersPage(page, 20, q, filter);

        model.addAttribute("usersList", usersPage.getContent());
        model.addAttribute("totalPages", usersPage.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("query", q);
        model.addAttribute("filter", filter);
        model.addAttribute("allRoles", ERole.values());

        return "/admin/users";
    }

    @PostMapping("/update")
    public String updateUser(
            @RequestParam Long userId,
            @RequestParam String custname,
            @RequestParam String custsurname,
            @RequestParam String firmname,
            @RequestParam(required = false) List<ERole> roles,
            HttpServletRequest request) {
        
        Users admin = clientService.GetCurrentUser();
        if (admin == null) return "redirect:/login";

        Users user = adminService.GetUsersById(userId);
        if (user != null) {
            Clientdetail cd = user.getClientdetail();
            cd.setCustname(custname);
            cd.setCustsurname(custsurname);
            cd.setFirmname(firmname);
            
            // Оновлення ролей
            if (roles != null && !roles.isEmpty()) {
                // Забороняємо знімати роль адміна самому собі через цей інтерфейс для безпеки
                if (user.getId().equals(admin.getId()) && !roles.contains(ERole.ROLE_ADMIN)) {
                    roles.add(ERole.ROLE_ADMIN);
                }
                
                user.getRoles().clear();
                for (ERole r : roles) {
                    Roles role = new Roles();
                    role.setRole(r);
                    user.getRoles().add(role);
                }
            }
            
            clientService.SaveUser(user);
            historyService.saveHistory(History_DocumentChange, "Адміністратор " + admin.getEmail() + " оновив дані користувача " + user.getEmail(), admin);
        }

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/admin/users");
    }

    @GetMapping("/enable/{id}")
    public String enableUser(@PathVariable Long id, HttpServletRequest request) {
        Users admin = clientService.GetCurrentUser();
        if (admin == null) return "redirect:/login";
        
        Users user = adminService.GetUsersById(id);
        if (user != null) {
            user.getClientdetail().setConfirminfo(true);
            user.getClientdetail().setConfirmDate(new java.util.Date());
            clientService.SaveUser(user);
        }
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/admin/users");
    }

    @GetMapping("/reject/{id}")
    public String rejectUser(@PathVariable Long id, HttpServletRequest request) {
        Users admin = clientService.GetCurrentUser();
        if (admin == null) return "redirect:/login";
        
        Users user = adminService.GetUsersById(id);
        if (user != null) {
            user.getClientdetail().setConfirminfo(false);
            clientService.SaveUser(user);
        }
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/admin/users");
    }

    @GetMapping("/apruveinfo/{id}")
    public String apruveInfo(@PathVariable Long id, HttpServletRequest request) {
        Users admin = clientService.GetCurrentUser();
        if (admin == null) return "redirect:/login";
        
        Users user = adminService.GetUsersById(id);
        if (user != null) {
            user.getClientdetail().setConfirminfo(true);
            user.getClientdetail().setConfirmDate(new java.util.Date());
            clientService.SaveUser(user);
        }
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/admin/users");
    }

    @GetMapping("/lock/{id}")
    public String lockUser(@PathVariable Long id, HttpServletRequest request) {
        Users admin = clientService.GetCurrentUser();
        if (admin == null) return "redirect:/login";
        
        Users user = adminService.GetUsersById(id);
        if (user != null) {
            user.getRoles().clear();
            clientService.SaveUser(user);
        }
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/admin/users");
    }

    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id, HttpServletRequest request) {
        Users admin = clientService.GetCurrentUser();
        if (admin == null) return "redirect:/login";

        Users user = adminService.GetUsersById(id);
        if (user != null) {
            user.getRoles().clear();
            clientService.SaveUser(user);
        }
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/admin/users");
    }
}
