package media.toloka.rfa.rsa_toradio;

import javax.crypto.Cipher;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSAEncrypt {
    public static PublicKey loadPublicKey(String filename) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(Files.readAllBytes(Paths.get(filename)));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    public static String encrypt(String data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

//    public static void main(String[] args) throws Exception {
//        // 1️⃣ Завантажуємо публічний ключ
//        PublicKey publicKey = loadPublicKey("public_key.pem");
//
//        // 2️⃣ Шифруємо повідомлення
//        String message = "Привіт, Python!";
//        String encryptedMessage = encrypt(message, publicKey);
//
//        System.out.println("🔒 Зашифроване повідомлення: " + encryptedMessage);
//    }
}

