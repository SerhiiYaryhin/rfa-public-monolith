package media.toloka.rfa.radio.client;

import media.toloka.rfa.model.Clientaddress;
import media.toloka.rfa.model.Clientdetail;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.security.model.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ClientAddressEditController {

    @Autowired
    private ClientService clientService;

    @GetMapping(value = "/user/createaddress")
    public String ClientAddressEdit(
            @RequestParam(value = "id", required = true) Long id,
//            @RequestParam(value = "id") Long id,
//            @ModelAttribute Station station,
            Model model ) {
        Users frmuser = clientService.GetCurrentUser();
        if (frmuser == null) {
            return "redirect:/";
        }
        Clientaddress clientaddress;
        if (id == 0L) {
            clientaddress = new Clientaddress();
        } else {
            clientaddress = clientService.GetAddress(id);
        }
        model.addAttribute("clientaddress", clientaddress );
        return "/user/address";
    }
    @PostMapping(value = "/user/createaddress")
    public String ClientAddressSave(
//            @RequestParam(value = "id", required = true) Long id,
//            @RequestParam(value = "id") Long id,
            @ModelAttribute Clientaddress fclientaddress,
            Model model ) {
        Users frmuser = clientService.GetCurrentUser();
        if (frmuser == null) {
            return "redirect:/";
        }

        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (fclientaddress.getId() == null) {
            fclientaddress.setClientdetail(cd);
            clientService.SaveAddress(fclientaddress);
            clientService.GetAddressList(cd).add(fclientaddress);
            clientService.SaveClientDetail(cd);
        } else {
            clientService.SaveAddress(fclientaddress);
        }

        List<Clientaddress> clientaddresslist = clientService.GetAddressList(cd);
        model.addAttribute("clientaddresses", clientaddresslist );
        return "redirect:/user/usereditinfo";
    }
}
