package media.toloka.rfa.ansible;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Тимчасовий контролер для тестування Ansible-сервісів.
 * Дозволяє вручну запускати плейбуки через HTTP-запити.
 */
@Slf4j
@RestController
@RequestMapping("/api/test/ansible")
@RequiredArgsConstructor
public class AnsibleTestController {

    private final AnsibleExecutionService executionService;
    private final AnsibleStationControlService controlService;
    private final AnsibleStationDeletionService deletionService;

    /**
     * Тест створення станції.
     * GET /api/test/ansible/create?ip=192.168.1.50&port=8085
     */
    @GetMapping("/create")
    public String testCreate(
            @RequestParam(defaultValue = "192.168.1.50") String ip,
            @RequestParam(defaultValue = "8085") int port
    ) {
        log.info("Тестовий запит на створення станції на IP: {}, Port: {}", ip, port);
        boolean success = executionService.createStation(
                ip,
                "test-client-1",
                "test-station-1",
                "test-api-key",
                "test-secret-key",
                "test_db",
                "test_user",
                "test_pass",
                "/test_vhost",
                "test_mq_user",
                "test_mq_pass",
                port
        );
        return success ? "Станція успішно створена (див. логи)" : "Помилка створення станції";
    }

    /**
     * Тест запуску станції.
     * GET /api/test/ansible/start?ip=192.168.1.50
     */
    @GetMapping("/start")
    public String testStart(@RequestParam(defaultValue = "192.168.1.50") String ip) {
        boolean success = controlService.startStation(ip, "test-client-1", "test-station-1");
        return success ? "Станція запущена" : "Помилка запуску";
    }

    /**
     * Тест зупинки станції.
     * GET /api/test/ansible/stop?ip=192.168.1.50
     */
    @GetMapping("/stop")
    public String testStop(@RequestParam(defaultValue = "192.168.1.50") String ip) {
        boolean success = controlService.stopStation(ip, "test-client-1", "test-station-1");
        return success ? "Станція зупинена" : "Помилка зупинки";
    }

    /**
     * Тест каскадного видалення.
     * GET /api/test/ansible/delete?ip=192.168.1.50
     */
    @GetMapping("/delete")
    public String testDelete(@RequestParam(defaultValue = "192.168.1.50") String ip) {
        boolean success = deletionService.deleteStation(
                ip,
                "test-client-1",
                "test-station-1",
                "test_db",
                "test_user",
                "/test_vhost",
                "test_mq_user"
        );
        return success ? "Станція видалена" : "Помилка видалення";
    }
}
