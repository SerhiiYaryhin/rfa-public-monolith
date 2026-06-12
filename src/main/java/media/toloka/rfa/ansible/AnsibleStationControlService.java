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
public class AnsibleStationControlService {

    private static final Logger log = LoggerFactory.getLogger(AnsibleStationControlService.class);

    @Value("${rfa.infrastructure.client-base-dir:/home/rfa-worker}")
    private String clientBaseDir;

    /**
     * Запуск радіостанції (docker-compose up -d)
     */
    public boolean startStation(String stationServerIp, String clientUuid, String stationUuid) {
        log.info("Запуск радіостанції {} для клієнта {}", stationUuid, clientUuid);
        return executePlaybook("playbooks/start_station.yml", stationServerIp, clientUuid, stationUuid);
    }

    /**
     * Зупинка радіостанції (docker-compose down)
     */
    public boolean stopStation(String stationServerIp, String clientUuid, String stationUuid) {
        log.info("Зупинка радіостанції {} для клієнта {}", stationUuid, clientUuid);
        return executePlaybook("playbooks/stop_station.yml", stationServerIp, clientUuid, stationUuid);
    }

    /**
     * Універсальний приватний метод для запуску плейбуків керування
     */
    private boolean executePlaybook(String playbookPath, String stationServerIp, String clientUuid, String stationUuid) {
        try {
            List<String> command = new ArrayList<>();
            command.add("ansible-playbook");
            command.add(playbookPath);

            // Цільовий сервер мовлення (inline-інвентар з комою)
            command.add("-i");
            command.add(stationServerIp + ",");

            // Користувач для SSH підключення
            command.add("-u");
            command.add("rfa-worker");

            // Формуємо змінні оточення для Ansible
            String extraVars = String.format("client_dir=%s client_uuid=%s station_uuid=%s",
                    clientBaseDir, clientUuid, stationUuid);

            command.add("-e");
            command.add(extraVars);

            ProcessBuilder processBuilder = new ProcessBuilder(command);

            // Встановлюємо робочий каталог процесу — корінь проєкту
            processBuilder.directory(new File(System.getProperty("user.dir")));
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // Читання логів у реальному часі
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[Ansible Control] {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("Плейбук {} успішно виконано (Код: 0)", playbookPath);
                return true;
            } else {
                log.error("Помилка виконання плейбука {}. Код виходу: {}", playbookPath, exitCode);
                return false;
            }

        } catch (Exception e) {
            log.error("Критична помилка під час роботи з плейбуком {}", playbookPath, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
