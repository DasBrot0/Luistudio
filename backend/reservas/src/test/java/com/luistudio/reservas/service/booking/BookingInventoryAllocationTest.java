package com.luistudio.reservas.service.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.luistudio.reservas.dto.booking.BookingUpsertRequest;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.model.ReservationEntity;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.ReservationRepository;
import com.luistudio.reservas.service.AuditService;
import com.luistudio.reservas.service.AvailabilitySubscriptionService;
import com.luistudio.reservas.service.BookingService;
import com.luistudio.reservas.service.DtoMapper;
import com.luistudio.reservas.service.EmailOutboxService;
import com.luistudio.reservas.service.RoomService;
import com.luistudio.reservas.service.UserService;
import com.luistudio.reservas.service.booking.validation.BookingValidationService;
import com.luistudio.reservas.service.email.EmailTemplateService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingInventoryAllocationTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private RoomService roomService;
    @Mock private UserService userService;
    @Mock private EmailOutboxService emailOutboxService;
    @Mock private AuditService auditService;
    @Mock private DtoMapper dtoMapper;
    @Mock private BookingValidationService bookingValidationService;
    @Mock private EmailTemplateService emailTemplateService;
    @Mock private AvailabilitySubscriptionService availabilitySubscriptionService;
    @InjectMocks private BookingService service;

    private final BookingUpsertRequest request = new BookingUpsertRequest(
        20L, LocalDate.of(2026, 7, 15), LocalTime.of(10, 0), LocalTime.of(11, 0), 2, "2do piso", null
    );
    private RoomEntity room;

    @BeforeEach
    void setUp() {
        UserEntity user = new UserEntity();
        user.setId(30L);
        room = new RoomEntity();
        room.setId(20L);
        room.setCodigo("M-2-GRU-06");
        room.setNombre("Sala de estudio grupal - 6 personas");
        room.setUbicacion("2do piso");
        room.setCantidadUnidades(4);
        when(userService.getById(30L)).thenReturn(user);
        when(roomService.getRoomEntity(20L)).thenReturn(room);
        when(reservationRepository.findTopByUsuarioAndSalaAndFechaAndHoraInicioAndHoraFinOrderByIdDesc(
            user, room, request.date(), request.start(), request.end())).thenReturn(Optional.empty());
    }

    @Test
    void assignsTheFirstFreePhysicalUnit() {
        AtomicReference<ReservationEntity> saved = new AtomicReference<>();
        when(reservationRepository.findOccupiedUnitNumbers(room, request.date(), request.start(), request.end(), null))
            .thenReturn(List.of(1, 2));
        when(reservationRepository.save(any())).thenAnswer(invocation -> {
            ReservationEntity booking = invocation.getArgument(0);
            booking.setId(99L);
            saved.set(booking);
            return booking;
        });
        when(reservationRepository.findById(99L)).thenAnswer(ignored -> Optional.of(saved.get()));

        service.createBooking(30L, request);

        assertThat(saved.get().getNumeroUnidad()).isEqualTo(3);
        verify(roomService).lockRoomInventory(20L);
    }

    @Test
    void rejectsWhenNoPhysicalUnitRemains() {
        when(reservationRepository.findOccupiedUnitNumbers(room, request.date(), request.start(), request.end(), null))
            .thenReturn(List.of(1, 2, 3, 4));

        assertThatThrownBy(() -> service.createBooking(30L, request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("agotaron todas las unidades");
    }
}
