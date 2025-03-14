package media.toloka.rfa.rsa_toradio;

import com.google.gson.JsonObject;
import com.google.gson.Gson;
import media.toloka.rfa.radio.dropfile.service.FilesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.util.Base64;

@Service
public class SendToRadio {

    @Autowired
    private RabbitTemplate template;

    @Autowired
    private FilesService filesService;

    @Value("${media.toloka.rfa.server.client_dir}")
    private String baseClientsDir;

    @Value("${media.toloka.rfa.station.basename}")
    private String externalGuiServer;

    @Value("${media.toloka.rfa.server.libretime.guiserver}")
    private String localGuiServer;

    @Value("${media.toloka.rfa.server.toradiosever.queue}")
    private String libretimeQueue;
    final Logger logger = LoggerFactory.getLogger(SendToRadio.class);


    ///  Надсилаємо повідомлення для програвання на радіо
    /// libretimeuser користувач на станції з правами адміністратора
    /// libretimecriptPSW криптований пароль libretime користувача
    /// libretimecriptPSW криптований пароль libretime користувача
    /// libretimeport порт main радіостанції libretime
    /// toradioserver Імʼя черги toRadio сервера
    /// newsUUID UUID для новини
    public void PutToRadio(
            String libretimeuser, /// libretimeuser користувач на станції з правами адміністратора
            String libretimecriptPSW, /// libretimecriptPSW криптований пароль libretime користувача
            String libretimeserver, /// libretimeserver сервер, на який направляємо потік
            String libretimeport, /// libretimeport порт main радіостанції libretime
            String toradioserver, /// toradioserver Імʼя черги toRadio сервера
            String newsUUID /// newsUUID UUID для новини
    ) {
        // 1️⃣ Створюємо JSON-об'єкт вручну
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("job", "toRadio");
        jsonObject.addProperty("user", libretimeuser);
        jsonObject.addProperty("port", libretimeport);
        jsonObject.addProperty("psw", libretimecriptPSW);
        jsonObject.addProperty("radioserver", toradioserver);
        jsonObject.addProperty("frontserver", libretimeserver);
        jsonObject.addProperty("uuid", newsUUID);

        // 2️⃣ Конвертуємо JsonObject у JSON-рядок
        Gson gson = new Gson();
        String jsonString = gson.toJson(jsonObject);
        template.convertAndSend(libretimeQueue, jsonString);
    }

    ///  генеруємо ключі для паролей користувачів станцій, через яких транслюємо потік новин
    public void ToRadioKeyGen(
            String to_radios_server /// toradioserver Імʼя черги toRadio сервера

    ) {
        KeyPairGenerator keyGen;
        KeyPair keyPair;
        PublicKey publicKey;
        PrivateKey privateKey;
        try {
            keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            keyPair = keyGen.generateKeyPair();
            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();

            // Зберігаємо публічний ключ
            String guiKeyDirectory = System.getenv("HOME") + baseClientsDir + "/key";

            try (FileOutputStream fos = new FileOutputStream(guiKeyDirectory + "/" + localGuiServer + ".pub")) {
                fos.write(Base64.getEncoder().encode(publicKey.getEncoded()));
            } catch (FileNotFoundException e) {
                logger.error("FileNotFoundException Проблема з записом публічного ключа для сервера {}.", localGuiServer);
            } catch (IOException e ) {
                logger.error("IOException Проблема з записом публічного ключа для сервера {}.", localGuiServer);
            }

            // Передаємо приватний ключ на сервер трансляції новин
            Gson gson = new Gson();
            // 1️⃣ Створюємо JSON-об'єкт вручну
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("job", "toRadiokey");
            jsonObject.addProperty("guiserver", localGuiServer);
            byte[] privKey = Base64.getEncoder().encode(privateKey.getEncoded());
            String sprivkey = "";
            for (int i = 0; i < privKey.length; i++) {
                sprivkey += (char) privKey[i];
            }
            jsonObject.addProperty("key", sprivkey);
            String jsonString = gson.toJson(jsonObject);
            template.convertAndSend(to_radios_server, jsonString);
        } catch (NoSuchAlgorithmException e) {
            logger.error("ToRadioKeyGen: Проблема з бібліотекою шифрування.");
        }

    }
}

