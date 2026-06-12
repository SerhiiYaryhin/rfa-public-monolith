package media.toloka.rfa.ansible;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnsibleJsonToFileService {

    // Jackson ObjectMapper для конвертації Map в JSON-рядок
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean executePlaybookWithJson(String stationServerIp, String clientUuid, String stationUuid) {
        Path tempJsonFile = null;
        try {
            // 1. Формуємо карту (Map) з усіма нашими змінними
            Map<String, Object> variables = new HashMap<>();
            variables.put("client_dir", "/home/rfa");
            variables.put("client_uuid", clientUuid);
            variables.put("station_uuid", stationUuid);

            // Параметри БД
            variables.put("db_server_ip", "postgres.rfa.local");
            variables.put("libretime_postgresql_dbname", "radio_db_" + stationUuid);
            variables.put("libretime_postgresql_username", "user_" + stationUuid);
            variables.put("libretime_postgresql_password", "SuperSecretPass123");

            // Параметри RabbitMQ та Nginx
            variables.put("mq_server_ip", "rabbitmq.rfa.local");
            variables.put("libretime_rabbitmq_vhost", "/" + stationUuid);
            variables.put("nginx_gateway_ip", "rfagate.rfa.local");

            // 2. Створюємо тимчасовий файл на диску
            tempJsonFile = Files.createTempFile("ansible-vars-", ".json");

            // Записуємо нашу Map у файл як JSON
            objectMapper.writeValue(tempJsonFile.toFile(), variables);

            // 3. Формуємо команду для ProcessBuilder
            List<String> command = new ArrayList<>();
            command.add("ansible-playbook");
            command.add("playbooks/create_station_full.yml");
            command.add("-i"); command.add(stationServerIp + ",");
            command.add("-u"); command.add("rfa-worker");

            // 🎯 ЗАМІСТЬ довгого рядка передаємо шлях до файлу із символом @
            command.add("-e");
            command.add("@" + tempJsonFile.toAbsolutePath().toString());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(System.getProperty("user.dir")));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            // ... (тут твоє зчитування логів BufferedReader) ...

            int exitCode = process.waitFor();
            return exitCode == 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            // 4. Обов'язково видаляємо тимчасовий файл із паролями після виконання
            if (tempJsonFile != null) {
                try {
                    Files.deleteIfExists(tempJsonFile);
                } catch (Exception ex) {
                    // лог помилки видалення файлу
                }
            }
        }
    }
}
