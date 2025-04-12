package media.toloka.rfa.radio.stt;


import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletResponse;
import media.toloka.rfa.config.gson.service.GsonService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.history.service.HistoryService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.newstoradio.model.*;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.radio.stt.model.ESttModel;
import media.toloka.rfa.radio.stt.model.ESttStatus;
import media.toloka.rfa.radio.stt.model.Stt;
import media.toloka.rfa.radio.stt.model.SttRPC;
import media.toloka.rfa.radio.stt.service.STTBackServerService;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static media.toloka.rfa.rpc.model.ERPCJobType.JOB_STT;

@Controller
public class STTHome {

    @Value("${rabbitmq.queueTTS}")
    private String queueTTS;

    @Value("${rabbitmq.queueSTT}")
    private String queueSTT;
//    @Value("${media.toloka.rfa.station.basename}")
//    private String baseSiteAddress;

    //    @Value("${media.toloka.rfa.server.libretime.output.site}")
    @Value("${media.toloka.rfa.server.globalname}")
    private String globalServerName; // глобальний url порталу
    @Value("${media.toloka.rfa.server.localname}")
    private String localServerName;  // сервер, на який приходить RabbitMQ відповідь

    @Autowired
    private RabbitTemplate template;

    @Autowired
    private GsonService gsonService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private STTBackServerService sttBackServerService;

    @Autowired
    private HistoryService historyService;

    final Logger logger = LoggerFactory.getLogger(STTHome.class);

    /// Зберегти результат
    @GetMapping(value = "/stt/download/{scurpage}/{uuid}")
    public @ResponseBody byte[] getSttResultContent(
            @PathVariable String uuid, // uuid або запису stt, або запису storage
            @PathVariable String scurpage,
            HttpServletResponse response,
            Model model) {
        Stt SttObject = sttBackServerService.GetByUUID(uuid);
        if (SttObject == null) {
            logger.info("getSttResultContent: UUID не знайдено  {}", uuid);
            return null;
        }
        try {
            return SttObject.getText().getBytes("UTF8");
        }catch (UnsupportedEncodingException e) {
            return null;
        }
    }

        /// видаляємо трек

        /// видаляємо запис
        @GetMapping(value = "/stt/deletestt/{scurpage}/{uuidstt}")
        public String userDeleteStt (
                @PathVariable String uuidstt,
                @PathVariable String scurpage,
                Model model){

            Users user = clientService.GetCurrentUser();
            if (user == null) {
                return "redirect:/";
            }
            Clientdetail cd = clientService.GetClientDetailByUser(user);

            Stt stt = sttBackServerService.GetByUUID(uuidstt);


            if (stt == null) {

                return "/stt/home/0";
            }


//        model.addAttribute("liststation", listStation);
//        model.addAttribute("categorys", category);
            model.addAttribute("curstt", uuidstt);
            model.addAttribute("currentPage", scurpage);

            Long rc = 0L;
            if (stt != null) rc = sttBackServerService.deleteStt(uuidstt);
            if (rc == 0L) model.addAttribute("success", "Файл з голосом успішно видалено");
            else model.addAttribute("error", "Файл з голосом не видалено");
            return "redirect:/stt/home/" + scurpage;
        }

        /// відправляємо текст на перетворення
        @GetMapping(value = "/stt/sttprepare/{scurpage}/{uuidstt}")
        public String ttsprepare (
                @PathVariable String uuidstt,
                @PathVariable String scurpage,
                Model model){

            Users user = clientService.GetCurrentUser();
            Clientdetail clientdetail = clientService.GetClientDetailByUser(user);
            if (user == null) {
                return "redirect:/";
            }
            Clientdetail cd = clientService.GetClientDetailByUser(user);

            Stt curstt = sttBackServerService.GetByUUID(uuidstt);
            if (curstt != null) {
                // знайшли новину. Відправляємо на tts
                SttRPC rjob = new SttRPC();
                rjob.setRJobType(JOB_STT);

                rjob.getFront().setUser(System.getenv("USER"));
                rjob.getFront().setLocalserver(localServerName); // сервер на який відправляємо відповідь
                rjob.getFront().setGlobalserver(globalServerName); // сервер з якого беремо файл через curl

                rjob.setFilenamevoice(curstt.getStorespeach().getFilename()); // Імʼя файлу з голосом
                rjob.setSttUUID(curstt.getUuid());
                rjob.setModel(curstt.getModel().label);
                rjob.setUuidvoice(curstt.getStorespeach().getUuid());

                rjob.setRc(1024L);

                Gson gson = gsonService.CreateGson();
                String sgson = gson.toJson(rjob).toString();
                template.convertAndSend(queueSTT, sgson);
                model.addAttribute("success", "Завдання перетворення тексту в голос надіслано на обробку.");
                curstt.setStatus(ESttStatus.STT_STATUS_SEND);
                curstt.setDatechangestatus(new Date());
                sttBackServerService.Save(curstt);

            } else {
                model.addAttribute("error", "<b>Щось пішло не так - не знайшли новину "
                        + ".Завдання перетворення тексту в голос не надіслано на обробку.</b>");
                logger.info("==== NEWS ttsprepare: Щось пішло не так - не знайшли новину {}", uuidstt);
            }
            // формуємо інформацію для відображення
            Integer curpage = 0;

// Пейджинг для сторінки
            Page pageStore = sttBackServerService.GetSttPageByClientDetail(curpage, 10, cd);
            List<News> viewList = pageStore.stream().toList();

            model.addAttribute("totalPages", pageStore.getTotalPages());
            model.addAttribute("currentPage", scurpage);
            model.addAttribute("linkPage", "/creater/tracks/");
            model.addAttribute("viewList", viewList);

            return "redirect:/stt/home/" + scurpage;
        }

        /// відображаємо сторінку з новинами
        @GetMapping(value = "/stt/home/{cPage}")
        public String GetNewsHome (
                @PathVariable String cPage,
                Model model){
            Users user = clientService.GetCurrentUser();
            if (user == null) {
                return "redirect:/";
            }
            Clientdetail cd = clientService.GetClientDetailByUser(user);
            Integer curpage = Integer.parseInt(cPage);

// Пейджинг для сторінки
            Page pageStore = sttBackServerService.GetSttPageByClientDetail(curpage, 10, cd);
            List<Stt> viewList = pageStore.stream().toList();
            List<Stt> sttList = sttBackServerService.GetListSttByCd(cd);

            //        model.addAttribute("runstatus", runTTS);
            model.addAttribute("totalPages", pageStore.getTotalPages());
            model.addAttribute("currentPage", curpage);
            model.addAttribute("linkPage", "/creater/tracks/");
            model.addAttribute("viewList", viewList);

            return "/stt/home";
        }

        /// Створюємо або редагуємо новину
        @GetMapping(value = "/stt/editstt/{scurpage}/{uuid}")
        public String GetEditNews (
                @PathVariable String uuid, // uuid або запису stt, або запису storage
                @PathVariable String scurpage,
                Model model){
            Users user = clientService.GetCurrentUser();
            if (user == null) {
                return "redirect:/";
            }
            Clientdetail cd = clientService.GetClientDetailByUser(user);

            Stt curstt = sttBackServerService.GetByUUID(uuid);
            if (curstt == null) {
                curstt = new Stt();
                curstt.setClientdetail(cd);
                curstt.setStorespeach(storeService.GetStoreByUUID(uuid));
            }

            List<ESttModel> modelList = Arrays.asList(ESttModel.values());
//        curstt.setClientdetail(cd);

//        List<ENewsCategory> category = Arrays.asList(ENewsCategory.values());


//        List<ENewsVoice> voices = Arrays.asList(ENewsVoice.values());

//        model.addAttribute("voices", voices);
            model.addAttribute("modelList", modelList);
            model.addAttribute("curstt", curstt);
            model.addAttribute("currentPage", scurpage);

            return "/stt/editstt";
        }

        /// зберігаємо створену або відредаговану новину
        @PostMapping(value = "/stt/editstt/{pagelist}")
        public String newsCreateEditNews (
//            @PathVariable String uuidNews,
                @PathVariable String pagelist,
                @ModelAttribute Stt fStt,
                Model model){
            Users user = clientService.GetCurrentUser();
            if (user == null) {
                return "redirect:/";
            }
            Clientdetail cd = clientService.GetClientDetailByUser(user);

//        News stt = newsService.GetByUUID(uuidNews);

            Stt stt = null;
            if (fStt.getUuid() != null) {
                stt = sttBackServerService.GetByUUID(fStt.getUuid());
            }
            Boolean type;
            if (stt != null) {
                stt.setTitle(fStt.getTitle());
                stt.setModel(fStt.getModel());
                type = false;
            } else {
                stt = new Stt();
                stt.setClientdetail(cd);
                stt.setTitle(fStt.getTitle());
                stt.setStorespeach(fStt.getStorespeach());
                stt.setId(System.currentTimeMillis());
                stt.setUuid(UUID.randomUUID().toString());
                stt.setModel(fStt.getModel());
                type = true;
            }

//        logger.info(stt.toString());
            if (stt != null) sttBackServerService.Save(stt);

            if (type) return "redirect:/stt/home/0";
            return "redirect:/stt/home/" + pagelist;

        }

    }
