package media.toloka.rfa.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.atomic.AtomicLong;

//import static FileDownloader.downloadFile;

public class FileDownloader {

    /**
     * Рекурсивна функція для завантаження файлу за URL, з обробкою редиректів.
     * Ім'я файлу визначається з останньої частини URL.
     *
     * @param urlString URL файлу для завантаження
     * @return Об'єкт DownloadResult, що містить вхідний потік та ім'я файлу
     * @throws DownloadFileException Якщо виникла помилка під час завантаження
     */
    public static DownloadFileResult downloadFile(String urlString) throws DownloadFileException {
        try {
            // Створюємо об'єкт URL
            URL url = new URL(urlString);
            // Відкриваємо з'єднання з сервером
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // Отримуємо код відповіді сервера
            int responseCode = connection.getResponseCode();

            if (responseCode >= 300 && responseCode <= 399) { // Редирект
                // Отримуємо новий URL з заголовка Location
                String newUrl = connection.getHeaderField("Location");
                System.out.println("Перенаправлено на: " + newUrl);
                // Рекурсивно викликаємо функцію для нового URL
                return downloadFile(newUrl);
            } else if (responseCode == 200) { // Успішне завантаження
                // Визначаємо ім'я файлу з URL
                String fileName = extractFileNameFromUrl(urlString);
                // Повертаємо результат, що містить вхідний потік та ім'я файлу
                return new DownloadFileResult(connection.getInputStream(), fileName);
            } else {
                // Кидаємо виключення при несподіваному коді відповіді
                throw new DownloadFileException("Несподіваний HTTP статус: " + responseCode);
            }
        } catch (IOException e) {
            // Обробляємо виключення введення-виведення
            throw new DownloadFileException("Помилка при завантаженні файлу: " + e.getMessage(), e);
        }
    }

    /**
     * Витягує ім'я файлу з URL.
     *
     * @param urlString URL файлу
     * @return Ім'я файлу або "default_filename.bin" за замовчуванням
     */
    private static String extractFileNameFromUrl(String urlString) {
        // Використовуємо URI для зручного парсингу URL
        URI uri = URI.create(urlString);
        String path = uri.getPath();
        // Отримуємо останню частину шляху (ім'я файлу)
        int lastSlashIndex = path.lastIndexOf('/');
        return lastSlashIndex >= 0 ? path.substring(lastSlashIndex + 1) : "default_filename.bin";
    }

    /**
     * Зберігає файл на диск за вказаним шляхом.
     *
     * @param result Результат завантаження (вхідний потік та ім'я файлу)
     * @param outputPath Шлях для збереження файлу
     * @throws IOException Якщо виникла помилка при записі у файл
     */

}