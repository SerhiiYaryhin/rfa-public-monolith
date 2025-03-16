from Crypto.PublicKey import RSA
from Crypto.Cipher import PKCS1_OAEP

# 1️⃣ Читаємо приватний ключ із файлу (PEM-формат)
with open("private_key.pem", "r") as key_file:
    private_key = RSA.import_key(key_file.read())

# 2️⃣ Читаємо зашифровані дані з файлу
with open("encrypted_data.bin", "rb") as enc_file:
    encrypted_data = enc_file.read()

# 3️⃣ Розшифровуємо дані
cipher_rsa = PKCS1_OAEP.new(private_key)

try:
    decrypted_data = cipher_rsa.decrypt(encrypted_data)
    
    # 4️⃣ Записуємо розшифровану строку у файл
    with open("decrypted_text.txt", "w") as output_file:
        output_file.write(decrypted_data.decode())

    print("✅ Дані успішно розшифровані та збережені у decrypted_text.txt!")

except ValueError:
    print("❌ ПОМИЛКА: Невірне розшифрування! Можливо, використано неправильний ключ або алгоритм.")

