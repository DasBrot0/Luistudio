package com.luistudio.reservas.service;

import com.luistudio.reservas.dto.admin.AnnouncementRequest;
import com.luistudio.reservas.dto.admin.AnnouncementResponse;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.model.InstitutionalAnnouncementEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.model.UserStatus;
import com.luistudio.reservas.repository.InstitutionalAnnouncementRepository;
import com.luistudio.reservas.repository.UserRepository;
import com.luistudio.reservas.service.email.EmailTemplateService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnouncementService {

    private final InstitutionalAnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final EmailOutboxService emailOutboxService;
    private final EmailTemplateService emailTemplateService;

    public AnnouncementService(
        InstitutionalAnnouncementRepository announcementRepository,
        UserRepository userRepository,
        EmailOutboxService emailOutboxService,
        EmailTemplateService emailTemplateService
    ) {
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
        this.emailOutboxService = emailOutboxService;
        this.emailTemplateService = emailTemplateService;
    }

    @Transactional
    public AnnouncementResponse publish(Long authorId, AnnouncementRequest request) {
        UserEntity author = userRepository.findById(authorId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        InstitutionalAnnouncementEntity entity = new InstitutionalAnnouncementEntity();
        entity.setAutor(author);
        entity.setTitle(request.title());
        entity.setContent(request.content());
        entity.setAnnouncementType(request.announcementType());
        entity.setStatus("PUBLICADO");
        announcementRepository.save(entity);

        String body = emailTemplateService.branded(
            request.title(),
            request.content(),
            List.of(),
            List.of(),
            null,
            null
        );

        int recipientCount = 0;
        int pageNumber = 0;
        Page<UserEntity> page;
        do {
            page = userRepository.searchUsers(null, null, UserStatus.HABILITADO, null, PageRequest.of(pageNumber, 200));
            for (UserEntity student : page.getContent()) {
                if (!"ESTUDIANTE".equalsIgnoreCase(student.getRol().getNombre())) continue;
                emailOutboxService.enqueue(student, request.title(), body, "{\"notificationType\":\"ANNOUNCEMENT\"}");
                recipientCount++;
            }
            pageNumber++;
        } while (pageNumber < page.getTotalPages());

        return new AnnouncementResponse(entity.getId(), entity.getTitle(), entity.getAnnouncementType(), entity.getCreatedAt(), recipientCount);
    }
}
