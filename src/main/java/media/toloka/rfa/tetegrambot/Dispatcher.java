package media.toloka.rfa.tetegrambot;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.tetegrambot.handler.UserRequestHandler;
import media.toloka.rfa.tetegrambot.model.UserRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@Getter
public class Dispatcher {

    @Autowired
    private final List<UserRequestHandler> handlers; // = new ArrayList<>();

    @Autowired
    private  TelegramBot telegramBot;

    public Dispatcher(List<UserRequestHandler> handlers,TelegramBot telegramBot) {
//        this.handlers = handlers;
        telegramBot.setDispatcher(this);
        List<UserRequestHandler> handlers1 = handlers
                .stream()
                .sorted(Comparator
                        .comparing(UserRequestHandler::isGlobal)
                        .reversed())
                .collect(Collectors.toList());
//                log.info ("{}",handlers);
        this.handlers = handlers1;
    }


    public boolean dispatch(UserRequest userRequest) {
        for (UserRequestHandler userRequestHandler : handlers) {
            if(userRequestHandler.isApplicable(userRequest)){
                userRequestHandler.handle(userRequest);
                return true;
            }
        }
        return false;
    }
}