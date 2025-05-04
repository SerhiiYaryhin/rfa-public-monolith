package media.toloka.rfa.radio.client.service;

import media.toloka.rfa.podcast.model.PodcastChannel;
import media.toloka.rfa.radio.login.service.TokenService;
import media.toloka.rfa.radio.model.Clientaddress;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.client.ClientHomeInfoController;
import media.toloka.rfa.radio.model.Token;
import media.toloka.rfa.radio.repository.ClientAddressRepository;
import media.toloka.rfa.radio.repository.ClientDetailRepository;
import media.toloka.rfa.radio.repository.UserRepository;
import media.toloka.rfa.radio.repository.DocumentRepository;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.security.model.ERole;
import media.toloka.rfa.security.model.Roles;
import media.toloka.rfa.security.model.Users;
import media.toloka.rfa.tetegrambot.model.UserRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

import static media.toloka.rfa.security.model.ERole.ROLE_TELEGRAM;

@Service
public class ClientService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ClientDetailRepository clientDetailRepository;
    @Autowired
    private ClientAddressRepository clientAddressRepository;
    @Autowired
    private TokenService tokenService;

    @Autowired
    private DocumentRepository documentRepository;

    // додавання викликає зациклювання вкладень
//    @Autowired
//    private StoreService storeService;

    final Logger logger = LoggerFactory.getLogger(ClientHomeInfoController.class);


    public Users GetUserByEmail(String email) {
        return userRepository.getUserByEmail(email);
    }

    public Clientdetail GetUserFromTelegram(String telegramUser) {
        return clientDetailRepository.getUserByTelegramuser(telegramUser);
    }

    /**
     * отримуємо пошту авторизованого користувача
     *
     * @return поштова адреса зареєстрованого користувача
     */
    public Users GetCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
//        List<Users> usersList= userRepository.findUserByEmail(email);
        return GetUserByEmail(email);
    }

    public List<Roles> getListRole() {

        return GetCurrentUser().getRoles();
    }

    public boolean checkRole(ERole role) {
        List<Roles> roles = getListRole();
        Iterator<Roles> iterator = roles.iterator();
        while (iterator.hasNext()) {
            Roles curRole = iterator.next();
            if (role.equals(curRole.getRole())) {
                return true;
            }
        }
        return false;
    }

    public boolean checkRole(Users user, ERole role) {
        List<Roles> roles = user.getRoles();
        Iterator<Roles> iterator = roles.iterator();
        while (iterator.hasNext()) {
            Roles curRole = iterator.next();
            if (role.equals(curRole.getRole())) {
                return true;
            }
        }
        return false;
    }

    public void SaveUser(Users user) {

        userRepository.save(user);
    }

    public Clientdetail GetClientDetailById(Long id) {

        return clientDetailRepository.getReferenceById(id);
//        return clientDetailRepository.getById(id);
    }

    public Clientdetail GetClientDetailByUUID(String uuid) {
        return clientDetailRepository.getByUuid(uuid);
    }

    public Clientdetail GetClientDetailByUser(Users user) {
        // todo якась херня :(
        if (user == null) {
            return null;
        }
        List<Clientdetail> cdl = clientDetailRepository.getByUser(user);
        if (cdl.isEmpty()) {
            return null;
        }
        if (cdl.size() > 1) {
            logger.info("Йой! Знайшли більше одної ClientDetail!!!");
            return null;
        }
        Clientdetail cd = cdl.get(0);
        return cd;
    }

    public void CreateClientsDetail(Users user, String name, String surname) {
//        Clientdetail clientdetail = new Clientdetail();
        Clientdetail clientdetail = user.getClientdetail();
        clientdetail.setUser(user);
        clientdetail.setCustname(name);
        clientdetail.setCustsurname(surname);
        clientdetail.setUuid(UUID.randomUUID().toString());
//        clientDetailRepository.save(clientdetail);
    }

    public void SaveClientDetail(Clientdetail curuserdetail) {
        clientDetailRepository.save(curuserdetail);
    }

    public Clientaddress GetAddress(Long id) {
        return clientAddressRepository.getById(id);
    }

    public void SaveAddress(Clientaddress fclientaddress) {
        clientAddressRepository.save(fclientaddress);
    }

    public List<Clientaddress> GetAddressList(Clientdetail Clientdetailrfa) {
        return clientAddressRepository.findByClientdetail(Clientdetailrfa);
    }

    public Users GetUserById(Long iduser) {
        return userRepository.getReferenceById(iduser);
//        return userRepository.getById(iduser);
    }

    public List<Clientaddress> GetClientAddressList(Clientdetail clientdetail) {
        List<Clientaddress> cal = clientAddressRepository.findByClientdetail(clientdetail);
        return cal;
    }

    public boolean ClientCanDownloadFile(Clientdetail cd) {
        // todo перевірка прав користувача завантажувати файли
        // перевіряємо, чи може клієнт завантажувати файли
        // причини, чому може не мати права:
        // - перевищено ліміт для зберігання;
        // - помічено, що завантажує всяку дурню
        // ..... Ще придумаю.
        return true;
    }

    public Clientdetail GetClientDetailByUuid(String clientUUID) {
        Clientdetail cd = clientDetailRepository.getByUuid(clientUUID);
        return cd;

    }

    public List<Users> GetAllUsers() {
//        return userRepository.findAll();
        return userRepository.findAllByOrderByIdDesc();
    }

    public List<Users> GetSearchUsers(String template) {
//        List<Users> usersList = new ArrayList<>();
        List<Users> usersList = userRepository.findUsersByTemplateEmail(template);
        List<Clientdetail> cdl = clientDetailRepository.findByTemplate(template);
//
//        List<MyDataClass> arrayList = new ArrayList<MyDataClass>();
//
//        Set<MyDataClass> uniqueElements = new HashSet<MyDataClass>(arrayList);
//        arrayList.clear();
//        arrayList.addAll(uniqueElements);

        for (Clientdetail cd : cdl) {
            usersList.add(cd.getUser());
        }
//        Set<Users> uniqueElements = new HashSet<Users>(usersList);
        Set<Long> uniqueElements = new HashSet<Long>();
        for (Users u : usersList) {
            uniqueElements.add(u.getId());
        }

        List<Users> usersUnic = new ArrayList<Users>();
        for (Long id : uniqueElements) {
            for (Users rul : usersList) {
                if (id == rul.getId()) {
                    usersUnic.add(rul);
                    break;
                }
            }
        }
        return usersUnic;
//        return userRepository.findAll();
    }


    public List<Clientaddress> GetUnApruvedDocumentsOrderLoaddate() {
        return clientAddressRepository.findByApruve(false);
    }

    public Clientaddress GetClientAddressById(Long idAddress) {
        return clientAddressRepository.getById(idAddress);
    }

//    public void DeleteUser(Users curuser) {
//        userRepository.delete(curuser);
//    }

    public String GetCurrentTrack(URL url) {
        StringBuilder json = null;
        try { //(
            InputStream input = url.openStream();
            // ) {
            InputStreamReader isr = new InputStreamReader(input);
            BufferedReader reader = new BufferedReader(isr);
            json = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                json.append((char) c);
            }

        } catch (IOException e) {
            logger.error("Помилка при отриманні інформації зі станції про поточний трек.");
            e.printStackTrace();
        }
        return json.toString();
    }

    // Записуємо ідентифікатор людини з Телеграму
    // rfaTelegramUuid - повторюємо процедуру при поштовій реєстрації.
    public boolean setTelegramLink(String rfaTelegramUuid, UserRequest userRequest) {
        Token myToken = tokenService.findByToken(rfaTelegramUuid);
        if (myToken != null) { // Знайшли токен для реєстрації
            Users user = myToken.getUser();
            tokenService.delete(myToken);
            Clientdetail cd = GetClientDetailByUser(user);
            // додаємо ROLE_TELEGRAM якщо його він відсутній
            Roles role = null;
            List<Roles> lroles = user.getRoles();
            for (Roles crole : lroles) {
                if (crole.getRole().equals(ROLE_TELEGRAM)) {
                    role = crole;
                }
            }
            if (role == null) { //в ролях не знайшли ROLE_TELEGRAM та дадаємо його
                role = new Roles();
                role.setRole(ROLE_TELEGRAM);
                user.getRoles().add(role);
                SaveUser(user);
            }
            cd.setTelegramuser(userRequest.getUpdate().getMessage().getFrom().getId().toString());
            cd.setTelegramuserchatid(userRequest.getUpdate().getMessage().getChatId().toString());
            logger.info("Set Telegram Link. UserId: {} ChatId {}", cd.getTelegramuser(), cd.getTelegramuserchatid());
            SaveClientDetail(cd);
            return true;
        }
        return false;
    }

    public List<Clientdetail> GetAllClientDetail() {
        return clientDetailRepository.findAll();
    }

//    public void SetProfilePhoto(String storeUUID, Clientdetail cd) {
//        Store storeNewPhoto = storeService.GetStoreByUUID(storeUUID);
//        if (storeNewPhoto == null) {
//            logger.info("З якогось дива переданий storeUUID для фото профайлу не знайдено.");
//            return;
//        } else {
//            if (cd.getProfilephoto() != null) {
//                // видаляємо старе фото
//                logger.info("Є старе фото в профайлі");
//                // Перевірити, чи новий сторе не такий самий, як старий.
//                // Це можливо при завантаженні файлу з тим самим іменем.
//                if (!storeNewPhoto.getUuid().equals(storeUUID)) storeService.DeleteInStore(cd.getProfilephoto()); // видалили старий
//            }
//            // Зберігаємо фото в ClientDetail
//            cd.setProfilephoto(storeNewPhoto);
//            clientDetailRepository.save(cd);
//            return;
//        }
//    }
}

/// Додаємо завантажене фото для профайлу в ClientDetail


