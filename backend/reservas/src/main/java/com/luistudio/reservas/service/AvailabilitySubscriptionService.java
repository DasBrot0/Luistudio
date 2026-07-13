package com.luistudio.reservas.service;

import com.luistudio.reservas.dto.room.AvailabilitySubscriptionListResponse;
import com.luistudio.reservas.dto.room.AvailabilitySubscriptionRequest;
import com.luistudio.reservas.dto.room.AvailabilitySubscriptionResponse;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.model.ReservationEntity;
import com.luistudio.reservas.model.RoomAvailabilitySubscriptionEntity;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.RoomAvailabilitySubscriptionRepository;
import com.luistudio.reservas.service.email.EmailTemplateService;
import com.luistudio.reservas.util.AppTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilitySubscriptionService {

    private final RoomAvailabilitySubscriptionRepository subscriptionRepository;
    private final UserService userService;
    private final RoomService roomService;
    private final EmailOutboxService emailOutboxService;
    private final EmailTemplateService emailTemplateService;

    public AvailabilitySubscriptionService(
        RoomAvailabilitySubscriptionRepository subscriptionRepository,
        UserService userService,
        RoomService roomService,
        EmailOutboxService emailOutboxService,
        EmailTemplateService emailTemplateService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.userService = userService;
        this.roomService = roomService;
        this.emailOutboxService = emailOutboxService;
        this.emailTemplateService = emailTemplateService;
    }

    @Transactional
    public AvailabilitySubscriptionResponse subscribe(Long userId, Long roomId, AvailabilitySubscriptionRequest request) {
        UserEntity user = userService.getById(userId);
        RoomEntity room = roomService.getRoomEntity(roomId);

        subscriptionRepository.findActiveByUserAndRoom(user, room)
            .ifPresent(s -> { throw new BusinessException(HttpStatus.CONFLICT, "Ya existe una suscripción activa para esa sala"); });

        RoomAvailabilitySubscriptionEntity entity = new RoomAvailabilitySubscriptionEntity();
        entity.setUsuario(user);
        entity.setSala(room);
        entity.setTargetDate(request.targetDate());
        entity.setStartTime(request.startTime());
        entity.setEndTime(request.endTime());
        entity.setStatus("ACTIVA");
        entity.setCreatedAt(OffsetDateTime.now());
        return toResponse(subscriptionRepository.save(entity));
    }

    @Transactional
    public void unsubscribe(Long userId, Long roomId) {
        UserEntity user = userService.getById(userId);
        RoomEntity room = roomService.getRoomEntity(roomId);
        subscriptionRepository.findByUsuarioAndStatus(user, "ACTIVA").stream()
            .filter(s -> s.getSala().getId().equals(roomId))
            .forEach(s -> {
                s.setStatus("CANCELADA");
                subscriptionRepository.save(s);
            });
    }

    @Transactional(readOnly = true)
    public AvailabilitySubscriptionListResponse getMySubscriptions(Long userId) {
        UserEntity user = userService.getById(userId);
        List<AvailabilitySubscriptionResponse> responses = subscriptionRepository
            .findByUsuarioAndStatus(user, "ACTIVA")
            .stream()
            .map(this::toResponse)
            .toList();
        return new AvailabilitySubscriptionListResponse(responses);
    }

    @Transactional
    public void notifySubscribers(RoomEntity room, LocalDate date, LocalTime startTime, LocalTime endTime) {
        List<RoomAvailabilitySubscriptionEntity> subscribers = subscriptionRepository
            .findActiveSubscriptionsForRoom(room, date, startTime, endTime);

        for (RoomAvailabilitySubscriptionEntity sub : subscribers) {
            String body = emailTemplateService.roomAvailableAlert(room.getNombre(), date, startTime, endTime);
            boolean queued = emailOutboxService.enqueue(
                sub.getUsuario(),
                "Sala disponible: " + room.getNombre(),
                body,
                "{\"notificationType\":\"ROOM_AVAILABLE\",\"subscriptionId\":" + sub.getId() + "}"
            );
            if (queued) {
                sub.setStatus("EN_COLA");
                subscriptionRepository.save(sub);
            }
        }
    }

    @Transactional
    public void processNewlyAvailableRooms() {
        for (RoomAvailabilitySubscriptionEntity subscription : subscriptionRepository.findByStatus("ACTIVA")) {
            if (subscription.getTargetDate().isBefore(AppTime.today())) continue;
            if (roomService.isRoomAvailable(
                subscription.getSala(),
                subscription.getTargetDate(),
                subscription.getStartTime(),
                subscription.getEndTime(),
                null
            )) {
                notifySubscribers(
                    subscription.getSala(),
                    subscription.getTargetDate(),
                    subscription.getStartTime(),
                    subscription.getEndTime()
                );
            }
        }
    }

    private AvailabilitySubscriptionResponse toResponse(RoomAvailabilitySubscriptionEntity s) {
        return new AvailabilitySubscriptionResponse(
            s.getId(),
            s.getSala().getId(),
            s.getSala().getNombre(),
            s.getTargetDate(),
            s.getStartTime(),
            s.getEndTime(),
            s.getStatus(),
            s.getCreatedAt()
        );
    }
}
