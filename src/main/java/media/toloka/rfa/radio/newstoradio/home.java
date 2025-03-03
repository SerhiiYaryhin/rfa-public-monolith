package media.toloka.rfa.radio.newstoradio;


import media.toloka.rfa.radio.client.ClientHomeController;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Album;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.newstoradio.model.News;
import media.toloka.rfa.radio.newstoradio.service.NewsService;
import media.toloka.rfa.radio.station.service.StationService;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class home {
    @Autowired
    private ClientService clientService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private StationService stationService;

    @Autowired
    private NewsService newsService;

    final Logger logger = LoggerFactory.getLogger(ClientHomeController.class);

    @GetMapping(value = "/newstoradio/home/{cPage}")
    public String GetNewsHome(
            @PathVariable String cPage,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);

        Integer curpage = Integer.parseInt(cPage);

//        String baseaddress = filesService.GetBaseClientDirectory(cd);


// Пейджинг для сторінки
//        Page pageStore = storeService.GetStorePageByClientDetail(curpage,10, cd);
        Page pageStore = newsService.GetNewsPageByClientDetail(curpage,10, cd);
        List<Store> viewList = pageStore.stream().toList();

        model.addAttribute("totalPages", pageStore.getTotalPages() );
        model.addAttribute("currentPage",curpage);
        model.addAttribute("linkPage","/creater/tracks/");

        model.addAttribute("viewList", viewList );

        return "/newstoradio/home";
    }


    @GetMapping(value = "/newstoradio/editnews/{uuidnews}")
    public String GetEditNews(
            @PathVariable String uuidnews,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);

        News curnews = newsService.GetByUUID(uuidnews);
        if (curnews == null) {
            curnews = new News();
        }

        model.addAttribute("curnews", curnews );

        return "/newstoradio/editnews";
    }


}
