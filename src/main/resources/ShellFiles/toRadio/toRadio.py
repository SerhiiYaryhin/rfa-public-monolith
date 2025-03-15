from Crypto.PublicKey import RSA
from Crypto.Cipher import PKCS1_OAEP
import base64

# Завантажуємо приватний ключ із файлу
def load_private_key(filename):
    with open(filename, "rb") as key_file:
        key_bytes = base64.b64decode(key_file.read())
        return RSA.import_key(key_bytes)

# Функція для розшифрування повідомлення
def decrypt_rsa(encrypted_message_base64, private_key):
    cipher_rsa = PKCS1_OAEP.new(private_key)
    encrypted_bytes = base64.b64decode(encrypted_message_base64)
    decrypted_bytes = cipher_rsa.decrypt(encrypted_bytes)
    return decrypted_bytes.decode('utf-8')

# 1️⃣ Завантажуємо приватний ключ
private_key = load_private_key("private_key.pem")

# 2️⃣ Встав сюди зашифроване повідомлення з Java
encrypted_message = "ВСТАВ СЮДИ ЗАШИФРОВАНИЙ ТЕКСТ"

# 3️⃣ Розшифруємо
decrypted_message = decrypt_rsa(encrypted_message, private_key)

print("🔓 Розшифроване повідомлення:", decrypted_message)

