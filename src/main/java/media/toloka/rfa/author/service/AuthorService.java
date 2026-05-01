package media.toloka.rfa.author.service;

import media.toloka.rfa.author.model.AuthorColumn;
import media.toloka.rfa.author.model.AuthorRequest;
import media.toloka.rfa.author.model.enumerate.EAuthorRequestStatus;
import media.toloka.rfa.author.repository.AuthorColumnRepository;
import media.toloka.rfa.author.repository.AuthorRequestRepository;
import media.toloka.rfa.security.model.ERole;
import media.toloka.rfa.security.model.Roles;
import media.toloka.rfa.security.model.Users;
import media.toloka.rfa.security.repository.UserSecurityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class AuthorService {

    @Autowired
    private AuthorRequestRepository requestRepository;

    @Autowired
    private AuthorColumnRepository columnRepository;

    @Autowired
    private UserSecurityRepository userRepository;

    public AuthorRequest saveRequest(AuthorRequest request) {
        return requestRepository.save(request);
    }

    public List<AuthorRequest> getAllPendingRequests() {
        return requestRepository.findAll().stream()
                .filter(r -> r.getStatus() == EAuthorRequestStatus.PENDING)
                .toList();
    }

    public Optional<AuthorRequest> getRequestByUuid(String uuid) {
        return requestRepository.findByUuid(uuid);
    }

    @Transactional
    public void approveRequest(String uuid) {
        AuthorRequest request = requestRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Заявку не знайдено"));

        request.setStatus(EAuthorRequestStatus.APPROVED);
        request.setProcessedAt(new Date());
        requestRepository.save(request);

        // Створюємо колонку для автора
        AuthorColumn column = new AuthorColumn();
        column.setAuthor(request.getClient());
        
        // Переносимо дані із запиту в колонку
        if (request.getTopic() != null && !request.getTopic().isBlank()) {
            column.setTitle(request.getTopic());
        } else {
            column.setTitle("Авторська колонка: " + request.getClient().getCustname());
        }
        column.setDescription(request.getResume());
        
        column.setSlug(generateSlug(request.getClient().getCustname() + "-" + request.getClient().getCustsurname()));
        columnRepository.save(column);

        // Оновлюємо роль користувача
        Users user = request.getClient().getUser();
        if (user != null) {
            boolean alreadyAuthor = user.getRoles().stream()
                    .anyMatch(r -> r.getRole() == ERole.ROLE_AUTHOR);
            
            if (!alreadyAuthor) {
                Roles authorRole = new Roles();
                authorRole.setRole(ERole.ROLE_AUTHOR);
                user.getRoles().add(authorRole);
                userRepository.save(user);
            }
        }
    }

    @Transactional
    public void rejectRequest(String uuid) {
        AuthorRequest request = requestRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Заявку не знайдено"));

        request.setStatus(EAuthorRequestStatus.REJECTED);
        request.setProcessedAt(new Date());
        requestRepository.save(request);
    }

    private String generateSlug(String input) {
        return input.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-") + "-" + (System.currentTimeMillis() % 1000);
    }
}
