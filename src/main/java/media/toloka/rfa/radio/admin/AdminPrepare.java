package media.toloka.rfa.radio.admin;

import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Documents;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.security.model.ERole;
import media.toloka.rfa.security.model.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.*;

@Profile("Front")
@Controller
@Log4j2
public class AdminPrepare {

    @Autowired
    private ClientService clientService;

    @Autowired
    private StoreService storeService;

    private static void performAction(File file1, File file2) {
        try {
            // Каталог першого файлу
            File parentDir = file1.getParentFile();

            // Отримуємо ім'я файлу без розширення
            String name = file1.getName();
//            int dotIndex = name.lastIndexOf(".");
//            String baseName = (dotIndex > 0) ? name.substring(0, dotIndex) : name;
//            String extension = (dotIndex > 0) ? name.substring(dotIndex) : "";

            // Створюємо нове ім’я з "_source"
//            String newName = baseName + extension + "._source";
            String newName = name + "._source";
            File renamedFile1 = new File(parentDir, newName);

            // Перейменування
            boolean renamed = file1.renameTo(renamedFile1);
            if (!renamed) {
                System.out.println("❌ Не вдалося перейменувати перший файл.");
                return;
            }
            System.out.println("✅ Перший файл перейменовано на: " + renamedFile1.getName());

            // Переміщення другого файлу в каталог першого
            Path targetPath = Path.of(parentDir.getAbsolutePath(), file2.getName());
            Files.move(file2.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("✅ Другий файл переміщено до каталогу першого: " + targetPath);
        } catch (IOException e) {
            System.out.println("❌ Помилка під час переміщення або перейменування: " + e.getMessage());
        }
    }

    @GetMapping(value = "/admin/prepare/mp3")
    public String getUserHome(
            Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        // перевірили, чи адмін тут працює
        if (!clientService.checkRole(user, ERole.ROLE_ADMIN)) return "redirect:/logout";
        log.info("Опрацьовуємо mp3 файли користувача виключно під адміністратором. ");
        String dirPrepared = "/tmp/";
        List<Store> storeList = storeService.GetAll();
        // перебираємо весь store і вибираємо всі звукові файли
        for (Store store : storeList) {
            if (store.getContentMimeType().contains("audio/")) {
                String locateFile = store.getFilepatch(); // повний шлях до файлу у сховищі
                String fileName = store.getFilename();    // Імʼя файлу
                String outputFile = dirPrepared + fileName;

                // Формуємо команду як список аргументів
                List<String> command = Arrays.asList(
                        "ffmpeg", "-y",
                        "-i", locateFile,
                        "-map", "0:a:0",
                        "-b:a", "48k",
                        outputFile
                );

                try {
                    ProcessBuilder builder = new ProcessBuilder(command);
                    builder.redirectErrorStream(true); // об'єднує stderr та stdout

                    Process process = builder.start();

                    // Читання виводу команди
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println(line);
                        }
                    }

                    int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        System.out.println("⚠️ ffmpeg завершився з кодом: " + exitCode + " для файлу: " + locateFile);
                    } else {
                        System.out.println(" ✅ ffmpeg завершився з чудовим кодом: для файлу: " + locateFile);
                    }

                } catch (IOException | InterruptedException e) {
                    System.out.println("Йой! Щось пішло не так! " + locateFile);
                    e.printStackTrace();
                }
                // завершили перетворення
                // змінюємо дату останньої обробки файла у сховищі
                // Порівнюємо розмір нового і перетвореного файлів
                // Шляхи до файлів
                File file1 = new File(locateFile);
                File file2 = new File(outputFile);

                // Перевірка існування
                if (!file1.exists()) {
                    System.out.println("❌ Перший файл не існує: " + file1.getAbsolutePath());
//                    return;
                }

                if (!file2.exists()) {
                    System.out.println("❌ Другий файл не існує: " + file2.getAbsolutePath());
//                    return;
                }

                long size1 = file1.length();
                long size2 = file2.length();

                if (size1 == 0 || size2 == 0) {
                    System.out.println("⚠️ Один із файлів має розмір 0 байт. Порівняння неможливе.");
//                    return;
                }

                // Обчислюємо співвідношення
                double ratio = (double) size1 / size2;

                // Округлення до 1 знака після коми
                DecimalFormat df = new DecimalFormat("#.#");
                String rounded = df.format(ratio);

                System.out.println("📏 Співвідношення: перший / другий = " + rounded);

                // Перевірка умови ≥ 1.5
                if (ratio >= 1.5) {

                    performAction(file1, file2);


                    // 🎯 Тут виконуються потрібні дії
                    // якщо перетворений файл менше ніж у півтора рази, то замінюємо джерельний файл

                    // перейменовуємо джерельний файл

                    // переносимо перетворений файл в сховище

                    // переписуємо розмір файлу.
                    log.info("Старий розмір {} - новий розмір {} . Коефіціент {}", size1, size2, rounded);
                    store.setFilelength(size2);

                    // Зберігаємо запис у базі
                    store.setPreparedate(new Date());
                    store.setPrepared(0);
                    storeService.SaveStore(store);
                    // видаляємо файл
//                    File fileToDelete = new File(outputFile);
//                    boolean success = fileToDelete.delete();
                } else {
                    System.out.println("ℹ️ Перший файл НЕ перевищує другий у 1.5 рази");
                }
            }



        }
//        model.addAttribute("clientdetailList", clientdetailList );
        return "/admin/documents";
    }
}
