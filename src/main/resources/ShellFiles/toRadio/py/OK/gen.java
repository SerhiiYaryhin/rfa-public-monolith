import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class RSAKeyGenerator {
    public static void main(String[] args) throws Exception {
        // Генеруємо пару ключів (2048 біт)
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        // Отримуємо приватний і публічний ключі
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        // Зберігаємо у форматі PEM
        savePEM("private_key.pem", "RSA PRIVATE KEY", privateKey.getEncoded());
        savePEM("public_key.pem", "PUBLIC KEY", publicKey.getEncoded());

        System.out.println("✅ Ключі збережені у PEM-форматі!");
    }

    // Зберігаємо у форматі PEM
    private static void savePEM(String filename, String type, byte[] keyData) throws Exception {
        String pemContent = "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder().encodeToString(keyData) + "\n"
                + "-----END " + type + "-----\n";
        Files.write(Paths.get(filename), pemContent.getBytes());
    }
}

