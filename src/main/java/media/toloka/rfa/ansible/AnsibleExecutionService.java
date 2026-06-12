package media.toloka.rfa.ansible;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class AnsibleExecutionService {

    public void createStation() {
        try {
            List<String> command = new ArrayList<>();
            command.add("ansible-playbook");
            command.add("playbooks/create_station_full.yml");
            command.add("-i"); command.add("192.168.1.50,"); // IP сервера мовлення
            command.add("-u"); command.add("rfa-worker");

            // Передаємо змінні
            command.add("-e");
            command.add("client_dir=/home/rfa-worker "+
                    "client_uuid=c1 "+
                    "station_uuid=st1 " +
                    "libretime_api_key=ak1 " +
                    "libretime_secret_key=sk1 " +
                    "libretime_timezone=Europe/Kyiv " +
                    "db_server_ip=pg.rfa " +
                    "libretime_postgresql_dbname=db1 " +
                    "libretime_postgresql_username=u1 " +
                    "libretime_postgresql_password=p1 " +
                    "mq_server_ip=rabbit.rfa " +
                    "libretime_rabbitmq_vhost=/vh1 " +
                    "libretime_rabbitmq_username=mq1 " +
                    "libretime_rabbitmq_password=mqp1 " +
                    "nginx_gateway_ip=gate.rfa " +
                    "station_web_port=8085");

            ProcessBuilder processBuilder = new ProcessBuilder(command);

            // Встановлюємо робочий каталог процесу — корінь проєкту, де лежить папка playbooks
            processBuilder.directory(new File(System.getProperty("user.dir")));
            processBuilder.redirectErrorStream(true); // Об'єднуємо stdout та stderr

            Process process = processBuilder.start();

            // Читаємо лог Ansible в реальному часі (можна писати в логер)
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Ansible] " + line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Станція успішно створена через Ansible!");
            } else {
                System.err.println("Помилка виконання плейбука. Код виходу: " + exitCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
