import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;

public class EncryptString {
    public static void main(String[] args) throws Exception {
        // Читаємо публічний ключ із файлу
        PublicKey publicKey = loadPublicKey("public_key.pem");

        // Строка для шифрування
        //String originalString = "Hello, RSA Encryption with PEM!";
        String originalString = "Це анкета реєстрації на інформаційні сесії, що відбудуться в рамках оголошеного конкурсу Програми Polaris.";

        // Шифруємо
        byte[] encryptedData = encrypt(originalString, publicKey);

        // Зберігаємо шифровану строку у файл
        Files.write(Paths.get("encrypted_data.bin"), encryptedData);

        System.out.println("🔒 Дані зашифровані та збережені у encrypted_data.bin!");
    }

    private static PublicKey loadPublicKey(String filename) throws Exception {
        // Читаємо вміст файлу
        String pem = new String(Files.readAllBytes(Paths.get(filename)))
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        // Декодуємо Base64
        byte[] keyBytes = Base64.getDecoder().decode(pem);

        // Відновлюємо публічний ключ
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    private static byte[] encrypt(String data, PublicKey publicKey) throws Exception {
        //Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
	Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data.getBytes());
    }
}

