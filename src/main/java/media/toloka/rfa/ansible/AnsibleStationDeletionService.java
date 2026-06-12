package media.toloka.rfa.ansible;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnsibleStationDeletionService {

    private static final Logger log = LoggerFactory.getLogger(AnsibleStationDeletionService.class);

    // Загальні параметри інфраструктури, які підтягуються з application.properties
    @Value("${media.toloka.rfa.server.libretime.postgresql.host}")
    private String dbServerIp;

    @Value("${rabbitmq.host}")
    private String mqServerIp;

    @Value("${media.toloka.rfa.server.libretime.gateway}")
    private String nginxGatewayIp;

    @Value("${media.toloka.rfa.server.client_dir:/Clients}")
    private String clientBaseDir;

    @Value("${media.toloka.rfa.upload_directory}")
    private String PATHuploadDirectory;

    /**
     * Метод для каскадного видалення радіостанції через Ansible
     *
     * @param stationServerIp             IP-адреса сервера мовлення, де крутиться Docker цієї станції
     * @param clientUuid                  UUID клієнта (наприклад, "client-456")
     * @param stationUuid                 UUID станції (наприклад, "station-789")
     * @param libretimePostgresqlDbName   Назва бази даних, яку треба видалити
     * @param libretimePostgresqlUserName Ім'я користувача БД, якого треба видалити
     * @param libretimeRabbitmqVhost      Назва vhost в RabbitMQ
     * @param libretimeRabbitmqUserName   Ім'я користувача RabbitMQ
     * @return true, якщо видалення пройшло успішно (exit code 0)
     */
    public boolean deleteStation(
            String stationServerIp,
            String clientUuid,
            String stationUuid,
            String libretimePostgresqlDbName,
            String libretimePostgresqlUserName,
            String libretimeRabbitmqVhost,
            String libretimeRabbitmqUserName
    ) {
        log.info("Початок каскадного видалення станції {} для клієнта {} на сервері {}", stationUuid, clientUuid, stationServerIp);

        try {
            List<String> command = new ArrayList<>();
            command.add("ansible-playbook");
            command.add("playbooks/delete_station_full.yml");

            // Задаємо цільовий сервер мовлення (кома в кінці обов'язкова для inline-інвентаря)
            command.add("-i");
            command.add(stationServerIp + ",");

            // Користувач, під яким Ansible підключається по SSH
            command.add("-u");
            command.add("rfa-worker");

            // Формуємо блок extra-vars (-e)
            StringBuilder extraVars = new StringBuilder();

            // 1. Шляхи та ідентифікатори
            extraVars.append("client_dir=").append(clientBaseDir).append(" ");
            extraVars.append("client_uuid=").append(clientUuid).append(" ");
            extraVars.append("station_uuid=").append(stationUuid).append(" ");

            // 2. Параметри для очищення PostgreSQL
            extraVars.append("db_server_ip=").append(dbServerIp).append(" ");
            extraVars.append("libretime_postgresql_dbname=").append(libretimePostgresqlDbName).append(" ");
            extraVars.append("libretime_postgresql_username=").append(libretimePostgresqlUserName).append(" ");

            // 3. Параметри для очищення RabbitMQ
            extraVars.append("mq_server_ip=").append(mqServerIp).append(" ");
            extraVars.append("libretime_rabbitmq_vhost=").append(libretimeRabbitmqVhost).append(" ");
            extraVars.append("libretime_rabbitmq_username=").append(libretimeRabbitmqUserName).append(" ");

            // 4. Параметр для очищення Nginx Proxy Gateway
            extraVars.append("nginx_gateway_ip=").append(nginxGatewayIp);

            command.add("-e");
            command.add(extraVars.toString());

            // Ініціалізуємо ProcessBuilder
            ProcessBuilder processBuilder = new ProcessBuilder(command);

            // Робочий каталог — корінь проєкту (там, де лежить папка playbooks)
            processBuilder.directory(new File(System.getProperty("user.dir")));

            // Перенаправляємо stderr в stdout, щоб бачити всі помилки Ansible в одному потоці
            processBuilder.redirectErrorStream(true);

            log.debug("Запуск команди: {}", String.join(" ", command));
            Process process = processBuilder.start();

            // Зчитуємо вивід Ansible в реальному часі та перенаправляємо в логер додатка
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[Ansible Delete] {}", line);
                }
            }

            // Чекаємо завершення процесу
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("Станція {} успішно видалена з усієї інфраструктури.", stationUuid);
                return true;
            } else {
                log.error("Помилка видалення станції {}. Ansible повернув код помилки: {}", stationUuid, exitCode);
                return false;
            }

        } catch (Exception e) {
            log.error("Критична помилка під час запуску Ansible плейбука для видалення станції {}", stationUuid, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
