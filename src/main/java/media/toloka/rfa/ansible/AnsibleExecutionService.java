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
 * Сервіс для первинного розгортання радіостанції через Ansible.
 */
@Slf4j
@Service
public class AnsibleExecutionService {

    @Value("${media.toloka.rfa.server.client_dir:/Clients}")
    private String clientBaseDir;

    @Value("${media.toloka.rfa.server.libretime.postgresql.host}")
    private String dbServerIp;

    @Value("${rabbitmq.host}")
    private String mqServerIp;

    @Value("${media.toloka.rfa.server.libretime.gateway}")
    private String nginxGatewayIp;

    @Value("${media.toloka.rfa.server.libretime.timezone:Europe/Kyiv}")
    private String timezone;

    /**
     * Повне розгортання нової станції.
     *
     * @param stationServerIp   IP сервера мовлення (де буде Docker)
     * @param clientUuid        UUID клієнта
     * @param stationUuid       UUID станції
     * @param libretimeApiKey   Ключ API для LibreTime
     * @param libretimeSecretKey Секретний ключ для LibreTime
     * @param postgresDbName    Назва БД
     * @param postgresUsername  Користувач БД
     * @param postgresPassword  Пароль БД
     * @param rabbitVhost       VHost RabbitMQ
     * @param rabbitUsername    Користувач RabbitMQ
     * @param rabbitPassword    Пароль RabbitMQ
     * @param stationWebPort    Порт для Nginx Docker-контейнера
     * @return true, якщо розгортання успішне
     */
    public boolean createStation(
            String stationServerIp,
            String clientUuid,
            String stationUuid,
            String libretimeApiKey,
            String libretimeSecretKey,
            String postgresDbName,
            String postgresUsername,
            String postgresPassword,
            String rabbitVhost,
            String rabbitUsername,
            String rabbitPassword,
            int stationWebPort
    ) {
        log.info("Початок розгортання станції {} для клієнта {} на сервері {}", stationUuid, clientUuid, stationServerIp);

        try {
            List<String> command = new ArrayList<>();
            command.add("ansible-playbook");
            command.add("playbooks/create_station_full.yml");

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
            extraVars.append("libretime_api_key=").append(libretimeApiKey).append(" ");
            extraVars.append("libretime_secret_key=").append(libretimeSecretKey).append(" ");
            extraVars.append("libretime_timezone=").append(timezone).append(" ");
            extraVars.append("db_server_ip=").append(dbServerIp).append(" ");
            extraVars.append("libretime_postgresql_dbname=").append(postgresDbName).append(" ");
            extraVars.append("libretime_postgresql_username=").append(postgresUsername).append(" ");
            extraVars.append("libretime_postgresql_password=").append(postgresPassword).append(" ");
            extraVars.append("mq_server_ip=").append(mqServerIp).append(" ");
            extraVars.append("libretime_rabbitmq_vhost=").append(rabbitVhost).append(" ");
            extraVars.append("libretime_rabbitmq_username=").append(rabbitUsername).append(" ");
            extraVars.append("libretime_rabbitmq_password=").append(rabbitPassword).append(" ");
            extraVars.append("nginx_gateway_ip=").append(nginxGatewayIp).append(" ");
            extraVars.append("station_web_port=").append(stationWebPort);

            command.add("-e");
            command.add(extraVars.toString());

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new File(System.getProperty("user.dir")));
            processBuilder.redirectErrorStream(true);

            log.debug("Команда Ansible: {}", String.join(" ", command));
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[Ansible Create] {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("Станція {} успішно створена через Ansible!", stationUuid);
                return true;
            } else {
                log.error("Помилка виконання плейбука створення станції {}. Код виходу: {}", stationUuid, exitCode);
                return false;
            }

        } catch (Exception e) {
            log.error("Критична помилка під час розгортання станції {}", stationUuid, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
