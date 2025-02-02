package media.toloka.rfa.podcast;


//import dev.stalla.builder.episode.EpisodeBuilder;
//import dev.stalla.model.Episode;
//import dev.stalla.model.MediaType;
//import dev.stalla.model.Podcast;
//import dev.stalla.model.rss.Enclosure;
//import dev.stalla.model.rss.RssCategory;
import com.rometools.rome.io.FeedException;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.podcast.model.PodcastChannel;
//import media.toloka.rfa.podcast.model.PodcastImage;
import media.toloka.rfa.podcast.model.PodcastItem;
import media.toloka.rfa.podcast.service.PodcastService;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.SyndFeedOutput;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;



@Controller
public class PodcastEditController {


    @Autowired
    private PodcastService podcastService;
    @Autowired
    private ClientService clientService;

    final Logger logger = LoggerFactory.getLogger(PodcastController.class);

    @GetMapping(value = "/podcast/pedit/{puuid}")
    public String PodcastChanel(
            @PathVariable String puuid,
            Model model ) {
/* */

        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) { return "redirect:/"; }

        logger.info("Зайшли на /podcast/pedit/",puuid);
        PodcastChannel podcast;
        if (puuid.length() < 3) {
            // створюємо новий подкаст
            podcast = new PodcastChannel();
            podcast.setClientdetail(cd); // todo повернути після отладки
            podcastService.SavePodcast(podcast);
            model.addAttribute("success",  "Створили новий подкаст."
                    +" Збережіть його і після цього додайте епізоди до подкасту.");
        } else {
            // шукаємо за UUID подкасту
            podcast = podcastService.GetChanelByUUID(puuid);
            if (podcast == null ) {
                model.addAttribute("warning",  "Йой! Щось пішло не так - ми не знайшли Ваш Подкаст."
                        +" Зверніться будь ласка до служби підтримки");
            }
        }


        List<PodcastItem> itemList = podcast.getItem();
        if (itemList.size() == 0) {
            model.addAttribute("warning", "Ваш подкаст ще не має епізодів."
                    +" Завантажте будь ласка епізоди, заповніть в них необхідні поля та додаайте до них обкладинку.");
        }

//        logger.info("image filename: " + podcast.getImage().getStoreidimage().getFilename());

//        працюємо з переліком категорій
//        ITUNES
        Map<String, List<String> > itunesCategory = podcastService.ItunesCategory();
        // вчимося працювати з Мапом
//        List<String> listFirstLevel;
//        ArrayList<String> listSecondLevel;
//        // беремо перелік ключів першого рівня
//        listFirstLevel = new ArrayList<String>(itunesCategory.keySet());
//        for (String firstLevel : listFirstLevel) {
//            logger.info( firstLevel );
//            ArrayList<String> ooo = (ArrayList<String>) itunesCategory.get(firstLevel);
//            for (String secondLevel : ooo) {
//                logger.info("--- " + secondLevel );
//            }
//        }
        Set<String> languages = Arrays.stream(Locale.getISOLanguages())
                .map(Locale::new)
                .map(Locale::getDisplayLanguage)
                .collect(Collectors.toCollection(TreeSet::new));

        model.addAttribute("languages", languages);
        model.addAttribute("itunesCategory", itunesCategory);
        model.addAttribute("podcast",  podcast);
        model.addAttribute("itemslist",  itemList);
        return "/podcast/pedit";
    }

    // публікуємо або знімаємо з публікації подкаст
    @PostMapping(value = "/podcast/channelpublish")
    public String PodcastChanelPublish (
            @ModelAttribute PodcastChannel podcast,
//            @ModelAttribute Users formUserPSW,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) { return "redirect:/"; }

        PodcastChannel tpodcast = podcastService.GetChanelByUUID(podcast.getUuid());
        if (tpodcast == null) {
          model.addAttribute("danger","Ви намагаєтесь опублікувати не збережений подкаст. Збережіть, а потім спробуйте опублікувати.");
//            model.addAttribute("podcast",  tpodcast);
//            model.addAttribute("itemslist",  tpodcast.getItem());
            return "redirect:/podcast/home/";
        }
        if (tpodcast.getApruve()) { // Якщо подкаст схвалено адміністратором, то можна змінювати статус публікації
            if (!tpodcast.getPublishing()) { //Змінюємо статус публікації подкасту
                // публікуємо
                tpodcast.setPublishing(true);
                tpodcast.setDatepublish(new Date());
            } else tpodcast.setPublishing(false); // Знімаємо з публікації
            podcastService.SavePodcast(tpodcast);
        }
//        model.addAttribute("podcast",  tpodcast);
//        model.addAttribute("itemslist",  tpodcast.getItem());
        return "redirect:/podcast/pedit/"+tpodcast.getUuid();
    }

    /* Зберігаємо подкаст */
    @PostMapping(value = "/podcast/chanelsave")
    public String PodcastChanelSave (
            @ModelAttribute PodcastChannel podcast,
//            @ModelAttribute Users formUserPSW,
            Model model ) {
        // http://localhost:3080/podcast/pedit/df61e94f-f74e-401b-9e81-c30a48982d21

        // Users user = clientService.GetCurrentUser();

        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) { return "redirect:/"; }

        podcast.setClientdetail(cd);


        PodcastChannel tpodcast = podcastService.GetChanelByUUID(podcast.getUuid());
        if (tpodcast != null) {
            tpodcast.setTitle(podcast.getTitle());
            tpodcast.setDescription(podcast.getDescription());
            tpodcast.setLastbuilddate(new Date());
            tpodcast.setClientdetail(cd);


            tpodcast.setLanguage(podcast.getLanguage());
            podcastService.SavePodcast(tpodcast);
        } else {
            podcast.setClientdetail(cd);
            podcastService.SavePodcast(podcast);
        }
        // пробуємо побудувати RSS
        // -  https://github.com/mpgirro/stalla/tree/master?tab=readme-ov-file
        // https://rometools.github.io/rome/HowRomeWorks/RomeV0.4TutorialUsingRomeToCreateAndWriteASyndicationFeed.html
/*
пробуємо працювати з RSS feed для подкасту
 */
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("atom_1.0");

        feed.setTitle("Sample Feed (created with Rome)");
        feed.setLink("http://rome.dev.java.net");
        feed.setDescription("This feed has been created using Rome (Java syndication utilities");

        List entries = new ArrayList();
        SyndEntry entry;
        SyndContent description;

        entry = new SyndEntryImpl();
        entry.setTitle("Rome v1.0");
        entry.setLink("http://wiki.java.net/bin/view/Javawsxml/Rome01");
        entry.setPublishedDate(new Date());
        description = new SyndContentImpl();
        description.setType("text/plain");
        description.setValue("Initial release of Rome");
        entry.setDescription(description);
        entries.add(entry);

        entry = new SyndEntryImpl();
        entry.setTitle("Rome v3.0");
        entry.setLink("http://wiki.java.net/bin/view/Javawsxml/Rome03");
        entry.setPublishedDate(new Date());
        description = new SyndContentImpl();
        description.setType("text/html");
        description.setValue("<p>More Bug fixes, mor API changes, some new features and some Unit testing</p>"+
                "<p>For details check the <a href=\"http://wiki.java.net/bin/view/Javawsxml/RomeChangesLog#RomeV03\">Changes Log</a></p>");
        entry.setDescription(description);
        entries.add(entry);

        feed.setEntries(entries);

        try {
            Writer writer = new FileWriter("/home/ysv/rss.xml");
            SyndFeedOutput output = new SyndFeedOutput();
            try {
                output.output(feed, writer);
                logger.info(output.outputString(feed));
            } catch (FeedException fe) {
                logger.info("Помилка 1 при запису RSS");
                logger.info(fe.getMessage());
            }
            writer.close();
        } catch (IOException e) {
            logger.info("Помилка 2 при запису RSS");
            logger.info(e.getMessage());
        }

        // TODO відправити повідомлення на сторінку
        model.addAttribute("success",  "Реакція на POST зі сторінки /podcast/proot");
        return "redirect:/podcast/home";
    }

//    /podcast/addepisode/
    @GetMapping(value = "/podcast/addepisode/{puuid}/{euuid}")
    public String PodcastAddEpisode (
            @PathVariable String euuid,
            @PathVariable String puuid,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) { return "redirect:/"; }

        // витягуємо епізод
        PodcastItem episode = podcastService.GetEpisodeByUUID(euuid);
        PodcastChannel podcastFromEpisode = episode.getChanel();
        PodcastChannel podcastTarget = podcastService.GetChanelByUUID(puuid);

        if (podcastFromEpisode != null) {
        // чистимо посилання на подкаст в епізоді та батьківському подкасті
            PodcastItem delItem = null;
            for (PodcastItem item : podcastFromEpisode.getItem()) {
                if (item.getId() == episode.getId() ) {
                    delItem = item;
                    break;

                }
            }
            if (delItem != null) {
                podcastFromEpisode.getItem().remove(delItem);
                podcastService.SavePodcast(podcastFromEpisode);
            } else {
                logger.warn("Щось пішло не так при видаленні епізоду з подкасту");
            }
        }
        podcastTarget.getItem().add(episode);
        episode.setChanel(podcastTarget);

        podcastService.SavePodcast(podcastTarget);
        model.addAttribute("podcast",  podcastTarget);
        model.addAttribute("itemslist",  podcastTarget.getItem());
        return "redirect:/podcast/pedit/"+podcastTarget.getUuid();

    }

    // http://localhost:3080/podcast/addcover/9fb3d97f-9080-4fea-ad81-c4eceaba8cac/2c76b4fe-861c-4eb9-a5b3-a168eb10acf5
    @GetMapping(value = "/podcast/addcover/{puuid}/{iuuid}")
    public String PodcastAddCover (
            @PathVariable String iuuid,
            @PathVariable String puuid,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) { return "redirect:/"; }

        // витягуємо епізод
        Store image = podcastService.GetStoreByUUID(iuuid);
        PodcastChannel podcastTarget = podcastService.GetChanelByUUID(puuid);

        podcastTarget.setImagestoreitem(image);

        podcastService.SavePodcast(podcastTarget);
        model.addAttribute("podcast",  podcastTarget);
        model.addAttribute("itemslist",  podcastTarget.getItem());
        return "redirect:/podcast/pedit/"+podcastTarget.getUuid();

    }

}
