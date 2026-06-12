package media.toloka.rfa.ansible;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервіс для каскадного видалення радіостанції через Ansible.
 */
@Slf4j
@Service
public class AnsibleStationDeletionService {

    @Value("${media.toloka.rfa.server.libretime.postgresql.host}")
    private String dbServerIp;

    @Value("${rabbitmq.host}")
    private String mqServerIp;

    @Value("${media.toloka.rfa.server.libretime.gateway}")
    private String nginxGatewayIp;

    @Value("${media.toloka.rfa.server.client_dir:/Clients}")
    private String clientBaseDir;

    /**
     * Метод для каскадного видалення радіостанції через Ansible.
     *
     * @param stationServerIp             IP-адреса сервера мовлення
     * @param clientUuid                  UUID клієнта
     * @param stationUuid                 UUID станції
     * @param libretimePostgresqlDbName   Назва бази даних
     * @param libretimePostgresqlUserName Ім'я користувача БД
     * @param libretimeRabbitmqVhost      Назва vhost в RabbitMQ
     * @param libretimeRabbitmqUserName   Ім'я користувача RabbitMQ
     * @return true, якщо видалення пройшло успішно
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
            command.add("playbooks/delete_station_with_quota.yml");

            // Цільовий сервер мовлення
            command.add("-i");
            command.add(stationServerIp + ",");

            // Користувач для SSH
            command.add("-u");
            command.add("rfa-worker");

            // Формуємо блок extra-vars (-e)
            StringBuilder extraVars = new StringBuilder();
            extraVars.append("client_dir=").append(clientBaseDir).append(" ");
            extraVars.append("client_uuid=").append(clientUuid).append(" ");
            extraVars.append("station_uuid=").append(stationUuid).append(" ");
            extraVars.append("db_server_ip=").append(dbServerIp).append(" ");
            extraVars.append("libretime_postgresql_dbname=").append(libretimePostgresqlDbName).append(" ");
            extraVars.append("libretime_postgresql_username=").append(libretimePostgresqlUserName).append(" ");
            extraVars.append("mq_server_ip=").append(mqServerIp).append(" ");
            extraVars.append("libretime_rabbitmq_vhost=").append(libretimeRabbitmqVhost).append(" ");
            extraVars.append("libretime_rabbitmq_username=").append(libretimeRabbitmqUserName).append(" ");
            extraVars.append("nginx_gateway_ip=").append(nginxGatewayIp);

            command.add("-e");
            command.add(extraVars.toString());

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new File(System.getProperty("user.dir")));
            processBuilder.redirectErrorStream(true);

            log.debug("Запуск команди: {}", String.join(" ", command));
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[Ansible Delete] {}", line);
                }
            }

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
