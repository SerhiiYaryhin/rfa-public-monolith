package media.toloka.rfa.rpc.service;

import com.google.gson.Gson;
import media.toloka.rfa.config.gson.service.GsonService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.email.service.EmailSenderService;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.newstoradio.model.NewsRPC;
import media.toloka.rfa.radio.station.service.StationService;
import media.toloka.rfa.rpc.model.RPCJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ServerRunnerService {
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


    @Autowired
            private StationService stationService;
    @Autowired
            private ClientService clientService;
    @Autowired
            private GsonService gsonService;
    @Autowired
            private EmailSenderService emailSenderService;

    Logger logger = LoggerFactory.getLogger(ServerRunnerService.class);

    @Autowired
    RabbitTemplate template; // працюємо з Thymeleaf - відправка пошти, формування файлів конфігурації

    public void CompletedPartRPCJob (RPCJob rpcjob) {
        // 1. перевіряємо, чи на верхівці стеку завдання, яке ми виконали - якщо так, то видаляємо.
        // 2. якщо ще залишилися завдання, то ставимо в чергу на виконання.

        if (rpcjob.getJobchain().isEmpty()) {
            logger.info("Результат виконання Завдання: {}",rpcjob.getResultJobList().toString());
            return;
        }
        // якщо в черзі є елементи, то відправляємо на виконання вибираючи з черги черговий елемент.

//        rpcjob.setRJobType(rpcjob.getJobchain().poll()); // set job type
        Gson gson = gsonService.CreateGson();
        String strgson = gson.toJson(rpcjob).toString();
        template.convertAndSend(queueName,gson.toJson(rpcjob).toString());
        // TODO Занести в історию запись про проведення міграції з кодом завершення.
        return;
    }

    //======================================================================
    public Integer  StationGetStatus(RPCJob rpcJob) {
        return 0;
    }

    // Формуємо файли конфігурації для Nginx, перезавантажуємо конфігурацію сервера.
    public Long StationPrepareNginx(RPCJob rpcJob) {
        Long rc = 129L;
        Gson gson = gsonService.CreateGson();
        Station tmpstation = gson.fromJson(rpcJob.getRjobdata(), Station.class);
        Station station = stationService.GetStationById(tmpstation.getId());
        //    docker-compose run --rm api libretime-api migrate
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", preparenginxforstationcommand);
        Map<String, String> env = pb.environment();
        // працюємо з темплейтом для Nginx
        // TODO зробити роботу з текстовим шаблоном!!!!
        Map<String, Object> props = new HashMap<>();
        props.put("stationname",station.getDbname());
        props.put("guiport", station.getGuiport());
        props.put("servergui",station.getGuiserver());
        String nginxconfig = emailSenderService.getTextContent(nginxtemplate,props);
        // Записуємо файл конфігурації в робочий каталог станції
        // todo перенести всі операції з файлами до каталогу радіостанцій
        String pathConfigFile = env.get("HOME") + clientdir + "/" + station.getClientdetail().getUuid() + "/"
                + station.getUuid() + "/" + station.getDbname() + ".rfa.toloka.media";
        try {
//            logger.info("============== ПИШИМО ФАЙЛ КОНФІГУРАЦІЇ ДЛЯ NGINX: " + pathConfigFile );
            //записуємо файл конфігурації Nginx в каталог користувача
            Files.write(Paths.get(pathConfigFile), nginxconfig.getBytes() );

        } catch (IOException e) {
            logger.info("================= Щось пішло не так при запису файлу конфігурації для Nginx.");
            e.printStackTrace();
        }
        //================================
        SetEnvironmentForProcessBuilder(env, station);
        pb.directory(new File(env.get("HOME")+ clientdir + "/" + env.get("CLIENT_UUID") + "/" +env.get("STATION_UUID")));
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            // виводимо на консоль
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info(line);
            }
//            try {
            int exitcode = p.waitFor();
            rc = Long.valueOf(exitcode);
//            logger.info("=========================== PREPARE NGINX: exit code = {}", String.valueOf(exitcode)  );
//            } catch (InterruptedException e){
//                logger.warn(" Щось пішло не так при виконанні завдання (p.waitFor) InterruptedException");
//                e.printStackTrace();
//            }
        } catch (IOException e) {
            logger.warn(" Щось пішло не так при виконанні завдання в операційній системі");
            e.printStackTrace();
        } catch (InterruptedException e){
            logger.warn(" Щось пішло не так при виконанні завдання (p.waitFor) InterruptedException");
            e.printStackTrace();
        }
//        }
        //================================================================
        // https://www.javaguides.net/2019/11/gson-localdatetime-localdate.html
        // Наступний крок: Міграція моделі перед першим запуском LibreTime
//        rpcJob.setRJobType(JOB_STATION_START); // set job type
//        String strgson = gson.toJson(rpcJob).toString();
//        template.convertAndSend(queueName,gson.toJson(rpcJob).toString());
//        // TODO Занести в історию запись про проведення міграції з кодом завершення.
        return rc;
    }

    public Long StationStart(RPCJob rpcJob) {
        Long rc = 129L;
        Gson gson = gsonService.CreateGson();
        Station tmpstation = gson.fromJson(rpcJob.getRjobdata(), Station.class);
        Station station = stationService.GetStationById(tmpstation.getId());
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", startStationCommand);
        Map<String, String> env = pb.environment();
        SetEnvironmentForProcessBuilder(env, station);
        String server_workdir;

        server_workdir = env.get("HOME")+ clientdir + "/" + env.get("CLIENT_UUID") + "/" +env.get("STATION_UUID");
        logger.info("============== Start Station {}", server_workdir);
        pb.directory(new File(server_workdir));
        pb.redirectErrorStream(true);
//        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
        try {
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            // виводимо на консоль
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info(line);
            }
//            try {
            int exitcode = p.waitFor();
            rc = Long.valueOf(exitcode);

            stationService.SetStationRunState(station,true); // Change runnining status station

        } catch (IOException e) {
            logger.warn(" IOException. Щось пішло не так при виконанні завдання в операційній системі при старті станції.");
            stationService.SetStationRunState(station,false); // Change runnining status station
            e.printStackTrace();
        } catch (InterruptedException e){
            logger.warn("InterruptedException. Щось пішло не так при виконанні завдання.");
            stationService.SetStationRunState(station,false); // Change runnining status station
            e.printStackTrace();
        }
//        }
        //================================================================
        // https://www.javaguides.net/2019/11/gson-localdatetime-localdate.html
        // Наступний крок: Міграція моделі перед першим запуском LibreTime
//        rpcJob.setRJobType(JOB_STATION_STOP); // set job type
//        String strgson = gson.toJson(rpcJob).toString();
//        template.convertAndSend(queueName,gson.toJson(rpcJob).toString());
//        // TODO Занести в історию запись про проведення міграції з кодом завершення.



        return rc;
    }

    public Long  StationStop(RPCJob rpcJob) {
        Long rc = 129L;
        Gson gson = gsonService.CreateGson();
        Station tmpstation = gson.fromJson(rpcJob.getRjobdata(), Station.class);
        Station station = stationService.GetStationById(tmpstation.getId());
        //    docker-compose run --rm api libretime-api migrate
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", stopStationCommand);
        Map<String, String> env = pb.environment();
        SetEnvironmentForProcessBuilder(env, station);
        String server_workdir;

        server_workdir = env.get("HOME")+ clientdir + "/" + env.get("CLIENT_UUID") + "/" +env.get("STATION_UUID");
        pb.directory(new File(server_workdir));
        pb.redirectErrorStream(true);
//        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
        try {
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            // виводимо на консоль
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info(line);
            }
//            try {
            int exitcode = p.waitFor();
            rc = Long.valueOf(exitcode);
            stationService.SetStationRunState(station,false); // Change runnining status station
//            } catch (InterruptedException e){
//                logger.warn(" Щось пішло не так при виконанні завдання (p.waitFor) InterruptedException");
//                e.printStackTrace();
//            }
        } catch (IOException e) {
            logger.warn(" Щось пішло не так при виконанні завдання в операційній системі");
            e.printStackTrace();
        } catch (InterruptedException e){
            logger.warn(" Щось пішло не так при виконанні завдання (p.waitFor) InterruptedException");
            e.printStackTrace();
        }
//        }
        //================================================================
        // https://www.javaguides.net/2019/11/gson-localdatetime-localdate.html
        // Наступний крок: Міграція моделі перед першим запуском LibreTime
//        rpcJob.setRJobType(JOB_STATION_STOP); // set job type
//        String strgson = gson.toJson(rpcJob).toString();
//        template.convertAndSend(queueName,gson.toJson(rpcJob).toString());
//        // TODO Занести в історию запись про проведення міграції з кодом завершення.



        return rc;

    }

    public Long StationMigrateLibretimeOnInstall(RPCJob rpcJob) {
        Long rc = 129L;
        Gson gson = gsonService.CreateGson();
        Station tmpstation = gson.fromJson(rpcJob.getRjobdata(), Station.class);
        Station station = stationService.GetStationById(tmpstation.getId());
        //    docker-compose run --rm api libretime-api migrate
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", migrateStationCommand);
        Map<String, String> env = pb.environment();
        SetEnvironmentForProcessBuilder(env, station);
        String server_workdir;

        server_workdir = env.get("HOME")+ clientdir + "/" + env.get("CLIENT_UUID") + "/" +env.get("STATION_UUID");
//        logger.info("============== MIGRATE WORKER DIR {}", server_workdir);
        pb.directory(new File(server_workdir));
        pb.redirectErrorStream(true);
//        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
        try {
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            // виводимо на консоль
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info(line);
            }
//            try {
                int exitcode = p.waitFor();
                rc = Long.valueOf(exitcode);
//                logger.info("=========================== Migrate Init LibreTime: exit code = {}", String.valueOf(exitcode)  );
//            } catch (InterruptedException e){
//                logger.warn(" Щось пішло не так при виконанні завдання (p.waitFor) InterruptedException");
//                e.printStackTrace();
//            }
        } catch (IOException e) {
            logger.warn(" Щось пішло не так при виконанні завдання в операційній системі");
            e.printStackTrace();
        } catch (InterruptedException e){
            logger.warn(" Щось пішло не так при виконанні завдання (p.waitFor) InterruptedException");
            e.printStackTrace();
        }
//        }
        //================================================================
        // https://www.javaguides.net/2019/11/gson-localdatetime-localdate.html
        // Наступний крок: Міграція моделі перед першим запуском LibreTime
//        rpcJob.setRJobType(JOB_STATION_PREPARE_NGINX); // set job type
//        String strgson = gson.toJson(rpcJob).toString();
//        template.convertAndSend(queueName,gson.toJson(rpcJob).toString());
//        // TODO Занести в історию запись про проведення міграції з кодом завершення.
        return rc;
    }

    public Integer StationMigrateToNewVersion(RPCJob rpcJob) {
        return 0;
    }

    public void SetEnvironmentForProcessBuilder(Map<String, String> env, Station station) {
        // Призначаємо значення env для виконання скрипту
        String homedirectory = env.get("HOME");
        env.put("CLIENT_DIR", homedirectory + clientdir);
        env.put("CLIENT_UUID", station.getClientdetail().getUuid());
        env.put("STATION_UUID", station.getUuid());
        env.put("STATION_WEB_PORT", station.getGuiport().toString());
        env.put("STATION_MASTER_PORT", station.getMain().toString());
        env.put("STATION_SHOW_PORT", station.getShow().toString());
        env.put("STATION_ID", station.getId().toString());

        env.put("LIBRETIME_VERSION", "4.0.0");

        env.put("RABBITMQ_DEFAULT_PASS", station.getDbname());
        env.put("RABBITMQ_QUEUE", station.getDbname());
        env.put("RABBITMQ_VROOT", station.getDbname());

        env.put("POSTGRES_PASSWORD", station.getDbname());
        env.put("PUBLIC_URL", "https://" + station.getDbname());

        env.put("LIBRETIME_API_KEY", station.getDbname());
        env.put("LIBRETIME_SECRET_KEY", station.getDbname());
        env.put("LIBRETIME_TIMEZONE", libretime_timezone);
        env.put("LIBRETIME_PATH", libretime_path);
        env.put("LIBRETIME_POSTGRESQL_HOST", libretime_postgresql_host);
        env.put("LIBRETIME_POSTGRESQL_PORT", libretime_postgresql_port);
        env.put("LIBRETIME_POSTGRESQL_USERNAME", station.getDbname());
        env.put("LIBRETIME_POSTGRESQL_DBNAME", station.getDbname());
        env.put("LIBRETIME_POSTGRESQL_PASSWORD", station.getDbname());
        env.put("LIBRETIME_RABBITMQ_HOST", libretime_rabbitmq_host);
        env.put("LIBRETIME_RABBITMQ_PORT", libretime_rabbitmq_port);
        env.put("LIBRETIME_RABBITMQ_VHOST", "/" + station.getDbname());
        env.put("LIBRETIME_RABBITMQ_USERNAME", station.getDbname());
        env.put("LIBRETIME_RABBITMQ_PASSWORD", station.getDbname());
        env.put("LIBRETIME_FROM_ADDRESS", libretime_from_address);
        env.put("LIBRETIME_SMTP_SERVER", libretime_smtp_server);
        env.put("LIBRETIME_SMTP_PORT", libretime_smtp_port);
        env.put("LIBRETIME_SMTP_ENCRYPTION", libretime_smtp_encryption);
        env.put("LIBRETIME_ICECAST_HOST", libretime_icecast_host);
        env.put("LIBRETIME_ICECAST_PORT", libretime_icecast_port);
        env.put("LIBRETIME_ICECAST_SOURCE_USER", libretime_icecast_sourceuser);
        env.put("LIBRETIME_ICECAST_ADMIN_USER", libretime_icecast_adminuser);
        env.put("LIBRETIME_ICECAST_SOURCE_PASSWORD", libretime_icecast_source_password);
        env.put("LIBRETIME_ICECAST_ADMIN_PASSWORD", libretime_icecast_admin_password);
        // TODO При необхідності беремо з опису станції
        env.put("LIBRETIME_OUTPUT_NAME", libretime_output_name);
        env.put("LIBRETIME_OUTPUT_DESCRIPTION", libretime_output_description);
        env.put("LIBRETIME_OUTPUT_SITE", libretime_output_site);
        env.put("LIBRETIME_OUTPUT_GENRE", libretime_output_genre);
        env.put("LIBRETIME_OUTPUT_MOBILE", libretime_output_mobile);
    }

    public Long AllocateStationOnServer(RPCJob rpcJob) {
        Long rc = 129L;
        Gson gson = gsonService.CreateGson();
        Station tmpstation = gson.fromJson(rpcJob.getRjobdata(), Station.class);
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", createStationCommand);
        Station station = stationService.GetStationById(tmpstation.getId());
        Map<String, String> env = pb.environment();
        SetEnvironmentForProcessBuilder(env, station);

        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info(line);
            }
            int exitcode = p.waitFor();
            rc = Long.valueOf(exitcode);
//            assert pb.redirectInput() == ProcessBuilder.Redirect.PIPE;
//            assert pb.redirectOutput().file() == log;
//            assert p.getInputStream().read() == -1;
        } catch (IOException e) {
            logger.warn(" Щось пішло не так при виконанні завдання в операційній системі");
            e.printStackTrace();
        } catch (InterruptedException e){
            logger.warn(" Щось пішло не так при виконанні завдання (p.waitFor) InterruptedException");
            e.printStackTrace();
        }
        //================================================================
        // https://www.javaguides.net/2019/11/gson-localdatetime-localdate.html
        // Наступний крок: Міграція моделі перед першим запуском LibreTime
//        rpcJob.setRJobType(JOB_STATION_LIBRETIME_MIGRATE); // set job type
//        String strgson = gson.toJson(rpcJob).toString();
//        template.convertAndSend(queueName,gson.toJson(rpcJob).toString());
//        // TODO Занести в історию запись о создании конфігураційних файлів
        return rc;
    }

    public Long StationGetPS(RPCJob rjob) {
        Long rc = 129L;
        Gson gson = gsonService.CreateGson();
//        rpcJob.getRjobdata()
        Station station = gson.fromJson(rjob.getRjobdata(), Station.class);
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", psStationCommand);
        Map<String, String> env = pb.environment();
        SetEnvironmentForProcessBuilder(env, station);
        String server_workdir = env.get("HOME")+ clientdir + "/" + env.get("CLIENT_UUID") + "/" +env.get("STATION_UUID");
//        logger.info("============== MIGRATE WORKER DIR {}", server_workdir);
        pb.directory(new File(server_workdir));
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info(line);
            }
            int exitcode = p.waitFor();
            rc = Long.valueOf(exitcode);
//            assert pb.redirectInput() == ProcessBuilder.Redirect.PIPE;
//            assert pb.redirectOutput().file() == log;
//            assert p.getInputStream().read() == -1;
        } catch (IOException e) {
            logger.warn(" Щось пішло не так при виконанні завдання в операційній системі");
            e.printStackTrace();
        } catch (InterruptedException e){
            logger.warn(" Щось пішло не так при виконанні завдання (p.waitFor) InterruptedException");
            e.printStackTrace();
        }
        //================================================================
        // https://www.javaguides.net/2019/11/gson-localdatetime-localdate.html
//        // TODO Занести в історию запись
        return rc;
    }

    public Long StationSetPSW(RPCJob rpcJob) {
        Long rc = 129L;
        Gson gson = gsonService.CreateGson();
//        rpcJob.getRjobdata()  LIBRETIME_POSTGRESQL_ADMIN_PSW
        Station tmpstation = gson.fromJson(rpcJob.getRjobdata(), Station.class);
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", setLibreTimeAdminPSW);
        Station station = stationService.GetStationById(tmpstation.getId());
        Map<String, String> env = pb.environment();
        env.put("LIBRETIME_POSTGRESQL_ADMIN_PSW",rpcJob.getUser().getPassword());
        env.put("PGPASSWORD",station.getDbname());
        SetEnvironmentForProcessBuilder(env, station);

        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info(line);
            }
            int exitcode = p.waitFor();
            rc = Long.valueOf(exitcode);
//            assert pb.redirectInput() == ProcessBuilder.Redirect.PIPE;
//            assert pb.redirectOutput().file() == log;
//            assert p.getInputStream().read() == -1;
        } catch (IOException e) {
            logger.warn(" Щось пішло не так при виконанні завдання в операційній системі");
            e.printStackTrace();
        } catch (InterruptedException e){
            logger.warn(" Щось пішло не так при виконанні завдання (p.waitFor) InterruptedException");
            e.printStackTrace();
        }


//        // TODO Занести в історию запись
        return rc;
    }

    // todo ДОПИСАТИ ВИДАЛЕННЯ СТАНЦІЇ
    public Long StationDelete(RPCJob rpcJob) {
        Long rc = 129L;
        Gson gson = gsonService.CreateGson();
        Station tmpstation = gson.fromJson(rpcJob.getRjobdata(), Station.class);
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", stationDeleteCommand);
        Station station = stationService.GetStationById(tmpstation.getId());
        Map<String, String> env = pb.environment();
        env.put("LIBRETIME_POSTGRESQL_ADMIN_PSW",rpcJob.getUser().getPassword());
        env.put("PGPASSWORD",station.getDbname());
        SetEnvironmentForProcessBuilder(env, station);

        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info(line);
            }
            int exitcode = p.waitFor();
            rc = Long.valueOf(exitcode);
        } catch (IOException e) {
            logger.warn(" Щось пішло не так при виконанні завдання в операційній системі");
            e.printStackTrace();
        } catch (InterruptedException e){
            logger.warn(" Щось пішло не так при виконанні завдання (p.waitFor) InterruptedException");
            e.printStackTrace();
        }


//        // TODO Занести в історию запись
        return rc;
    }

    public List<String> StationGetRunStation(NewsRPC rjob) {
        String localCommand = "docker ps --format \"{{.Names}}}\"|grep playout|awk '{print substr($1,1,36)   }'";
//        String localCommand = "ls -la";
//        Gson gson = gsonService.CreateGson();
//        Station station = gson.fromJson(rjob.getRjobdata(), Station.class);
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", localCommand);
        Map<String, String> env = pb.environment();
        String server_workdir = env.get("HOME");
        pb.directory(new File(server_workdir));
        pb.redirectErrorStream(true);
        List<String> listResponce = new ArrayList<>();
        try {
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                listResponce.add(line);
//                logger.info("==== Local station:{}",line);
            }
            int exitcode = p.waitFor();
        } catch (IOException e) {
            logger.warn(" Щось пішло не так при виконанні завдання в операційній системі");
            e.printStackTrace();
        } catch (InterruptedException e){
            logger.warn(" Щось пішло не так при виконанні завдання (p.waitFor) InterruptedException");
            e.printStackTrace();
        }
        // https://www.javaguides.net/2019/11/gson-localdatetime-localdate.html
        return listResponce;
    }

}
