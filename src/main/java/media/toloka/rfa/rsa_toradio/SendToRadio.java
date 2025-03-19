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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
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
    private String toRadioServerQueue;

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
        template.convertAndSend(toRadioServerQueue, jsonString);
    }

    /// Зберігаємо ключі у форматі PEM
    private void savePEM(String filename, String type, byte[] keyData) throws Exception {
        String pemContent = "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder().encodeToString(keyData) + "\n"
                + "-----END " + type + "-----\n";
        Files.write(Paths.get(filename), pemContent.getBytes());
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

            try {
                savePEM(guiKeyDirectory + "/" + localGuiServer + ".pub", "PUBLIC KEY", publicKey.getEncoded());
            } catch (FileNotFoundException e) {
                logger.error("FileNotFoundException Проблема з записом публічного ключа для сервера {}.", localGuiServer);
            } catch (IOException e) {
                logger.error("IOException Проблема з записом публічного ключа для сервера {}.", localGuiServer);
            } catch (Exception e) {
                logger.error("Exception Проблема з записом публічного ключа для сервера {}.", localGuiServer);
            }
            try {
                savePEM(guiKeyDirectory + "/" + localGuiServer + ".priv", "RSA PRIVATE KEY", privateKey.getEncoded());
            } catch (FileNotFoundException e) {
                logger.error("FileNotFoundException Проблема з записом приватного ключа для сервера {}.", localGuiServer);
            } catch (IOException e) {
                logger.error("IOException Проблема з записом приватного ключа для сервера {}.", localGuiServer);
            } catch (Exception e) {
                logger.error("Exception Проблема з записом приватного ключа для сервера {}.", localGuiServer);
            }

            String pempriv = null;
            try {
                pempriv = new String(Files.readAllBytes(Paths.get(guiKeyDirectory + "/" + localGuiServer + ".priv")));
            }catch (IOException e) {
                logger.error("IOException Проблема з читанням приватного ключа для сервера {}.", localGuiServer);
            }

            // Передаємо приватний ключ на сервер трансляції новин
            Gson gson = new Gson();
            // 1️⃣ Створюємо JSON-об'єкт вручну
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("job", "toRadiokey");
            jsonObject.addProperty("guiserver", localGuiServer);
            jsonObject.addProperty("key", pempriv);
            String jsonString = gson.toJson(jsonObject);

            template.convertAndSend(toRadioServerQueue, jsonString);

        } catch (NoSuchAlgorithmException e) {
            logger.error("ToRadioKeyGen: Проблема з бібліотекою шифрування.");
        }

    }

    public PublicKey loadPublicKey(String filename) {
        byte[] keyBytes;
        try {
            // Читаємо вміст файлу
            String pem = new String(Files.readAllBytes(Paths.get(filename)))
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            // Декодуємо Base64
            keyBytes = Base64.getDecoder().decode(pem);
        } catch (IOException e) {
            logger.info("Помилка при зчитуванні файлу приватного ключа");
            return null;
        }
        try {
            // Відновлюємо публічний ключ
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            logger.info("NoSuchAlgorithmException: Проблема завантаження приватного ключа.");
            return null;
        } catch (InvalidKeySpecException e) {
            logger.info("InvalidKeySpecException: Проблема завантаження приватного ключа.");
            return null;

        }

    }

    public String encrypt(String data) {
        // load public key
        String filenamePubKey;
        PublicKey publicKey;
        try {
            Files.createDirectories(Paths.get(System.getenv("HOME") + baseClientsDir + "/key"));
        } catch (IOException e) {
            logger.info("IOException: Не можемо створити дерикторію для публічного ключа.");
            return null;
        }

        filenamePubKey = System.getenv("HOME") + baseClientsDir + "/key" + "/" + localGuiServer + ".pub";
        File f = new File(filenamePubKey);
        if (f.exists()) {
            publicKey = loadPublicKey(filenamePubKey);
        } else {
            // ключ відсутній. Створюємо його.
            ToRadioKeyGen(localGuiServer);
            publicKey = loadPublicKey(filenamePubKey);
        }
        if (publicKey == null) {
            logger.info("Проблема роботи з приватним ключем. Не можемо завантажити.");
            return null;
        }
//        PublicKey publicKey = loadPublicKey(filenamePubKey);
        String sencriptPSW;
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (NoSuchAlgorithmException e) {
            logger.info("NoSuchAlgorithmException: Проблема роботи з приватним ключем.");
            return null;
        } catch (InvalidKeyException e) {
            logger.info("InvalidKeyException: Проблема роботи з приватним ключем.");
            return null;
        } catch (IllegalBlockSizeException e) {
            logger.info("IllegalBlockSizeException: Проблема роботи з приватним ключем.");
            return null;
        } catch (NoSuchPaddingException e) {
            logger.info("InvalidKeyException: Проблема роботи з приватним ключем.");
            return null;
        } catch (BadPaddingException e) {
            logger.info("BadPaddingException: Проблема роботи з приватним ключем.");
            return null;
        }
//        return sencriptPSW;
    }
}

