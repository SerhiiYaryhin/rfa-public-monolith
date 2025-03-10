package media.toloka.rfa.radio.admin;


import media.toloka.rfa.radio.admin.service.AdminService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.document.service.DocumentService;
import media.toloka.rfa.radio.model.Clientaddress;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.security.model.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Profile("Front")
@Controller
public class AdminAddress {

    @Autowired
    private AdminService adminService;
    @Autowired
    private ClientService clientService;
    @Autowired
    private DocumentService documentService;

    // http://localhost:8080/admin/addresses
    @GetMapping(value = "/admin/addresses")
    public String getAdminAdresses(
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        List<Clientaddress> clientaddressList = adminService.GetNotApruvedAddresses();
        HashMap<Long, Integer> clientdetailLongHashMap = new HashMap<>();
        List<Clientdetail> clientdetailList =  new ArrayList<>();
        for (Clientaddress adr : clientaddressList) {
            if (!clientdetailLongHashMap.containsKey(adr.getClientdetail().getId())) {
                Clientdetail cd = adr.getClientdetail();
                Integer sz = cd.getClientaddressList().size();
                clientdetailList.add(cd);
                clientdetailLongHashMap.put(cd.getId(), sz);
            }
        }

//        model.addAttribute("usersList", adminService.GetAllUsers() );
        model.addAttribute("clientdetailList", clientdetailList );
        return "/admin/addresses";
    }


//    http://localhost:8080/admin/invertaddressapruve/1
    @GetMapping("/admin/invertaddressapruve/{idAddress}")
    public String GetInvertAddressApruve(
            @PathVariable Long idAddress,
            Model model
    ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) { return "redirect:/"; }

        Clientaddress clientaddress = adminService.GetClientAddress(idAddress);
        clientaddress.setApruve(!clientaddress.getApruve());
        adminService.SaveClientAddress(clientaddress);
        return "redirect:/admin/addresses";

    }


    @GetMapping(value = "/admin/editaddress/{idAddress}")
    public String ClientAddressEdit(
            @PathVariable Long idAddress,
//            @RequestParam(value = "id") Long id,
//            @ModelAttribute Station station,
            Model model ) {
        Users frmuser = clientService.GetCurrentUser();
        if (frmuser == null) {
            return "redirect:/";
        }
        Clientaddress clientaddress = clientService.GetAddress(idAddress);
        model.addAttribute("clientaddress", clientaddress );
        return "/admin/address";
    }

    @PostMapping(value = "/admin/editaddress/{idAddress}")
    public String ClientAddressSave(
            @PathVariable Long idAddress,
//            @RequestParam(value = "id", required = true) Long id,
//            @RequestParam(value = "id") Long id,
            @ModelAttribute Clientaddress fclientaddress,
            Model model ) {
        Users frmuser = clientService.GetCurrentUser();
        if (frmuser == null) {
            return "redirect:/";
        }

//        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
//        Clientdetail cdf = fclientaddress.getClientdetail();


        Clientaddress cal = clientService.GetClientAddressById(idAddress) ;
        cal.setShortaddress(fclientaddress.getShortaddress());
        cal.setUserAddressType(fclientaddress.getUserAddressType());
        cal.setStreet(fclientaddress.getStreet());
        cal.setBuildnumber(fclientaddress.getBuildnumber());
        cal.setKorpus(fclientaddress.getKorpus());
        cal.setAppartment(fclientaddress.getAppartment());
        cal.setCityname(fclientaddress.getCityname());
        cal.setArea(fclientaddress.getArea());
        cal.setRegion(fclientaddress.getRegion());
        cal.setCountry(fclientaddress.getCountry());
        cal.setZip(fclientaddress.getZip());
        cal.setPhone(fclientaddress.getPhone());
        cal.setComment(fclientaddress.getComment());
        clientService.SaveAddress(cal);

        return "redirect:/admin/addresses";
    }




}
