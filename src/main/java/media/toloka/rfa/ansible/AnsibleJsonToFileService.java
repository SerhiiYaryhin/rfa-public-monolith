package media.toloka.rfa.ansible;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnsibleJsonToFileService {

    // беремо з файлу конфігурації всю інформацію щодо параметрів стосовно файлової системи:
    // місце розташування, назви скриптів, файли шаблонів тощо.
    @Value("${rabbitmq.queue}")
    private String queueName;
    @Value("${media.toloka.rfa.server.client_dir}")
    private String clientdir;
    @Value("${media.toloka.rfa.server.run_dir}")
    private String server_rundir;
    @Value("${media.toloka.rfa.server.createStationCommand}")
    private String createStationCommand;

    @Value("${media.toloka.rfa.server.deleteStationCommand}")
    private String stationDeleteCommand;

    @Value("${media.toloka.rfa.server.setLibreTimeAdminPSW}")
    private String setLibreTimeAdminPSW;

    @Value("${media.toloka.rfa.server.preparenginxforstationcommand}")
    private String preparenginxforstationcommand;

    @Value("${media.toloka.rfa.server.migrateStationCommand}")
    private String migrateStationCommand;
    @Value("${media.toloka.rfa.server.startStationCommand}")
    private String startStationCommand;
    @Value("${media.toloka.rfa.server.stopStationCommand}")
    private String stopStationCommand;
    @Value("${media.toloka.rfa.server.psStationCommand}")
    private String psStationCommand;
    @Value("${media.toloka.rfa.server.logsStationCommand}")
    private String logStationCommand;
    @Value("${media.toloka.rfa.server.libretime.timezone}")
    private String libretime_timezone;
    @Value("${media.toloka.rfa.server.libretime.path}")
    private String libretime_path;
    @Value("${media.toloka.rfa.server.libretime.postgresql.host}")
    private String libretime_postgresql_host;
    @Value("${media.toloka.rfa.server.libretime.postgresql.port}")
    private String libretime_postgresql_port;

    @Value("${media.toloka.rfa.server.libretime.rabbitmq.host}")
    private String libretime_rabbitmq_host;
    @Value("${media.toloka.rfa.server.libretime.rabbitmq.port}")
    private String libretime_rabbitmq_port;
    @Value("${media.toloka.rfa.server.libretime.fromaddress}")
    private String libretime_from_address;
    @Value("${media.toloka.rfa.server.libretime.smtp.server}")
    private String libretime_smtp_server;
    @Value("${media.toloka.rfa.server.libretime.smtp.port}")
    private String libretime_smtp_port;
    @Value("${media.toloka.rfa.server.libretime.smtp.encryption}")
    private String libretime_smtp_encryption;
    @Value("${media.toloka.rfa.server.libretime.icecast.host}")
    private String libretime_icecast_host;
    @Value("${media.toloka.rfa.server.libretime.icecast.port}")
    private String libretime_icecast_port;
    @Value("${media.toloka.rfa.server.libretime.icecast.sourcepassword}")
    private String libretime_icecast_source_password;
    @Value("${media.toloka.rfa.server.libretime.icecast.adminpassword}")
    private String libretime_icecast_admin_password;

    @Value("${media.toloka.rfa.server.libretime.icecast.source.user}")
    private String libretime_icecast_sourceuser;
    @Value("${media.toloka.rfa.server.libretime.icecast.admin.user}")
    private String libretime_icecast_adminuser;

    @Value("${media.toloka.rfa.server.libretime.output.name}")
    private String libretime_output_name;
    @Value("${media.toloka.rfa.server.libretime.output.description}")
    private String libretime_output_description;
    @Value("${media.toloka.rfa.server.libretime.output.site}")
    private String libretime_output_site;
    @Value("${media.toloka.rfa.server.libretime.output.genre}")
    private String libretime_output_genre;
    @Value("${media.toloka.rfa.server.libretime.output.mobile}")
    private String libretime_output_mobile;

    @Value("${media.toloka.rfa.server.nginxtemplate}")
    private String nginxtemplate;


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
