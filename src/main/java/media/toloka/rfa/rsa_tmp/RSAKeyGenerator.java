package media.toloka.rfa.rsa_tmp;

import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class RSAKeyGenerator {
    public static void main(String[] args) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        // Зберігаємо публічний ключ
        try (FileOutputStream fos = new FileOutputStream("public_key.pem")) {
            fos.write(Base64.getEncoder().encode(publicKey.getEncoded()));
        }

        // Зберігаємо приватний ключ
        try (FileOutputStream fos = new FileOutputStream("private_key.pem")) {
            fos.write(Base64.getEncoder().encode(privateKey.getEncoded()));
        }

        System.out.println("🔑 Ключі успішно згенеровані та збережені у файли!");
    }
}

