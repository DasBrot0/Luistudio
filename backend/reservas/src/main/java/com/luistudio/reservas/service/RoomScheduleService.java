package com.luistudio.reservas.service;

import com.luistudio.reservas.dto.admin.CampusScheduleDayInput;
import com.luistudio.reservas.dto.admin.CampusScheduleDayResponse;
import com.luistudio.reservas.dto.admin.CampusScheduleListResponse;
import com.luistudio.reservas.dto.admin.CampusScheduleResponse;
import com.luistudio.reservas.dto.admin.CampusScheduleUpdateRequest;
import com.luistudio.reservas.dto.room.RoomScheduleInput;
import com.luistudio.reservas.dto.room.RoomScheduleResponse;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.model.CampusScheduleEntity;
import com.luistudio.reservas.model.CampusEntity;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.RoomScheduleEntity;
import com.luistudio.reservas.model.RoomState;
import com.luistudio.reservas.repository.CampusScheduleRepository;
import com.luistudio.reservas.repository.CampusRepository;
import com.luistudio.reservas.repository.ReservationRepository;
import com.luistudio.reservas.repository.RoomRepository;
import com.luistudio.reservas.repository.RoomScheduleRepository;
import com.luistudio.reservas.util.AppTime;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomScheduleService {

    public record EffectiveSchedule(
        int dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime,
        boolean closed,
        boolean override
    ) {
    }

    private final CampusScheduleRepository campusScheduleRepository;
    private final CampusRepository campusRepository;
    private final RoomScheduleRepository roomScheduleRepository;
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final RoomCatalogTranslator roomCatalogTranslator;
    private final SystemConfigService systemConfigService;

    public RoomScheduleService(
        CampusScheduleRepository campusScheduleRepository,
        CampusRepository campusRepository,
        RoomScheduleRepository roomScheduleRepository,
        RoomRepository roomRepository,
        ReservationRepository reservationRepository,
        RoomCatalogTranslator roomCatalogTranslator,
        SystemConfigService systemConfigService
    ) {
        this.campusScheduleRepository = campusScheduleRepository;
        this.campusRepository = campusRepository;
        this.roomScheduleRepository = roomScheduleRepository;
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.roomCatalogTranslator = roomCatalogTranslator;
        this.systemConfigService = systemConfigService;
    }

    @Transactional(readOnly = true)
    public CampusScheduleListResponse listCampusSchedules() {
        List<RoomEntity> rooms = roomRepository.findByEstadoNot(RoomState.INACTIVA);
        List<String> campusesFromRooms = rooms.stream()
            .map(RoomEntity::getCampus)
            .distinct()
            .toList();
        List<String> campusesFromConfig = campusScheduleRepository.findAll().stream()
            .map(schedule -> schedule.getCampus().getNombre())
            .distinct()
            .toList();
        List<String> campuses = java.util.stream.Stream.concat(campusesFromRooms.stream(), campusesFromConfig.stream())
            .distinct()
            .sorted(String::compareToIgnoreCase)
            .toList();

        List<CampusScheduleResponse> values = campuses.stream()
            .map(campus -> toCampusResponse(campus, List.of()))
            .toList();
        return new CampusScheduleListResponse(values);
    }

    @Transactional
    public CampusScheduleResponse updateCampusSchedule(CampusScheduleUpdateRequest request) {
        String campus = request.campus().trim();
        systemConfigService.validateCampusSlotMinutes(request.slotMinutes());
        validateScheduleInputs(request.days(), request.slotMinutes());
        int currentSlot = systemConfigService.getCampusSlotMinutes(campus);
        if (currentSlot != request.slotMinutes()
            && reservationRepository.existsFutureActiveReservationsByCampus(campus, AppTime.today(), AppTime.nowTime())) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "No se puede cambiar la duración por reserva del campus porque existen reservas futuras activas"
            );
        }
        systemConfigService.setCampusSlotMinutes(campus, request.slotMinutes());

        CampusEntity campusEntity = campusRepository.findByNombreIgnoreCase(campus).orElseThrow(
            () -> new BusinessException(HttpStatus.BAD_REQUEST, "Campus no encontrado")
        );
        List<CampusScheduleEntity> existing = campusScheduleRepository.findByCampus_NombreIgnoreCaseOrderByDiaSemanaAsc(campus);
        Map<Integer, CampusScheduleEntity> existingByDay = existing.stream()
            .collect(Collectors.toMap(CampusScheduleEntity::getDiaSemana, Function.identity()));

        for (CampusScheduleDayInput dayInput : request.days()) {
            CampusScheduleEntity day = existingByDay.getOrDefault(dayInput.dayOfWeek(), new CampusScheduleEntity());
            day.setCampus(campusEntity);
            day.setDiaSemana(dayInput.dayOfWeek());
            day.setCerrado(dayInput.closed());
            day.setHoraApertura(dayInput.closed() ? null : dayInput.openTime());
            day.setHoraCierre(dayInput.closed() ? null : dayInput.closeTime());
            day.setUpdatedAt(OffsetDateTime.now());
            campusScheduleRepository.save(day);
        }

        List<String> warnings = collectCampusConflicts(campus);
        return toCampusResponse(campus, warnings);
    }

    @Transactional
    public void saveRoomSchedule(RoomEntity room, List<RoomScheduleInput> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            clearRoomSchedule(room);
            return;
        }

        validateRoomScheduleInputs(schedules);
        clearRoomSchedule(room);
        for (RoomScheduleInput input : schedules) {
            RoomScheduleEntity entity = new RoomScheduleEntity();
            entity.setSala(room);
            entity.setDiaSemana(input.dayOfWeek());
            entity.setCerrado(input.closed());
            entity.setHoraApertura(input.closed() ? null : input.openTime());
            entity.setHoraCierre(input.closed() ? null : input.closeTime());
            entity.setUpdatedAt(OffsetDateTime.now());
            roomScheduleRepository.save(entity);
        }
    }

    @Transactional(readOnly = true)
    public EffectiveSchedule getEffectiveScheduleForRoomDay(RoomEntity room, LocalDate date) {
        int day = date.getDayOfWeek().getValue();
        List<RoomScheduleEntity> roomSchedule = roomScheduleRepository.findBySalaOrderByDiaSemanaAsc(room);
        RoomScheduleEntity roomDay = roomSchedule.stream()
            .filter(item -> item.getDiaSemana() == day)
            .findFirst()
            .orElse(null);
        if (roomDay != null) {
            return new EffectiveSchedule(day, roomDay.getHoraApertura(), roomDay.getHoraCierre(), roomDay.getCerrado(), true);
        }

        CampusScheduleEntity campusDay = campusScheduleRepository.findByCampus_NombreIgnoreCaseOrderByDiaSemanaAsc(room.getCampus())
            .stream()
            .filter(item -> item.getDiaSemana() == day)
            .findFirst()
            .orElse(null);

        if (campusDay == null) {
            return new EffectiveSchedule(day, null, null, true, false);
        }
        return new EffectiveSchedule(day, campusDay.getHoraApertura(), campusDay.getHoraCierre(), campusDay.getCerrado(), false);
    }

    @Transactional(readOnly = true)
    public List<RoomScheduleResponse> getEffectiveWeeklySchedule(RoomEntity room) {
        Map<Integer, RoomScheduleEntity> roomOverrides = roomScheduleRepository.findBySalaOrderByDiaSemanaAsc(room)
            .stream()
            .collect(Collectors.toMap(RoomScheduleEntity::getDiaSemana, Function.identity()));
        Map<Integer, CampusScheduleEntity> campus = campusScheduleRepository.findByCampus_NombreIgnoreCaseOrderByDiaSemanaAsc(room.getCampus())
            .stream()
            .collect(Collectors.toMap(CampusScheduleEntity::getDiaSemana, Function.identity()));

        List<RoomScheduleResponse> result = new ArrayList<>();
        for (int day = DayOfWeek.MONDAY.getValue(); day <= DayOfWeek.SUNDAY.getValue(); day++) {
            RoomScheduleEntity roomDay = roomOverrides.get(day);
            if (roomDay != null) {
                result.add(new RoomScheduleResponse(day, roomDay.getHoraApertura(), roomDay.getHoraCierre(), roomDay.getCerrado(), true));
                continue;
            }

            CampusScheduleEntity campusDay = campus.get(day);
            if (campusDay == null) {
                result.add(new RoomScheduleResponse(day, null, null, true, false));
            } else {
                result.add(new RoomScheduleResponse(day, campusDay.getHoraApertura(), campusDay.getHoraCierre(), campusDay.getCerrado(), false));
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public boolean isTimeWithinSchedule(RoomEntity room, LocalDate date, LocalTime start, LocalTime end) {
        EffectiveSchedule schedule = getEffectiveScheduleForRoomDay(room, date);
        if (schedule.closed()) {
            return false;
        }
        if (schedule.openTime() == null || schedule.closeTime() == null) {
            return false;
        }
        return !start.isBefore(schedule.openTime()) && !end.isAfter(schedule.closeTime());
    }

    private void clearRoomSchedule(RoomEntity room) {
        List<RoomScheduleEntity> current = roomScheduleRepository.findBySalaOrderByDiaSemanaAsc(room);
        if (!current.isEmpty()) {
            roomScheduleRepository.deleteAll(current);
        }
    }

    private CampusScheduleResponse toCampusResponse(String campus, List<String> warnings) {
        List<CampusScheduleDayResponse> days = campusScheduleRepository.findByCampus_NombreIgnoreCaseOrderByDiaSemanaAsc(campus)
            .stream()
            .map(day -> new CampusScheduleDayResponse(day.getDiaSemana(), day.getHoraApertura(), day.getHoraCierre(), day.getCerrado()))
            .sorted(Comparator.comparingInt(CampusScheduleDayResponse::dayOfWeek))
            .toList();

        return new CampusScheduleResponse(
            campus,
            roomCatalogTranslator.campusToEs(campus),
            systemConfigService.getCampusSlotMinutes(campus),
            days,
            warnings
        );
    }

    @Transactional(readOnly = true)
    public int getCampusSlotMinutes(String campus) {
        return systemConfigService.getCampusSlotMinutes(campus);
    }

    private List<String> collectCampusConflicts(String campus) {
        List<RoomEntity> rooms = roomRepository.findByPabellon_Campus_NombreIgnoreCaseAndEstadoNot(campus, RoomState.INACTIVA);
        List<CampusScheduleEntity> campusSchedule = campusScheduleRepository.findByCampus_NombreIgnoreCaseOrderByDiaSemanaAsc(campus);
        Map<Integer, CampusScheduleEntity> campusByDay = campusSchedule.stream()
            .collect(Collectors.toMap(CampusScheduleEntity::getDiaSemana, Function.identity()));
        List<String> warnings = new ArrayList<>();

        for (RoomEntity room : rooms) {
            List<RoomScheduleEntity> overrides = roomScheduleRepository.findBySalaOrderByDiaSemanaAsc(room);
            for (RoomScheduleEntity override : overrides) {
                CampusScheduleEntity campusDay = campusByDay.get(override.getDiaSemana());
                if (campusDay == null) {
                    warnings.add("La sala " + room.getCodigo() + " tiene override sin horario general para día " + override.getDiaSemana());
                    continue;
                }
                if (override.getCerrado()) {
                    continue;
                }
                if (campusDay.getCerrado()) {
                    warnings.add("La sala " + room.getCodigo() + " abre en un día que el campus está cerrado (día " + override.getDiaSemana() + ")");
                    continue;
                }
                if (override.getHoraApertura() != null
                    && override.getHoraCierre() != null
                    && (override.getHoraApertura().isBefore(campusDay.getHoraApertura())
                        || override.getHoraCierre().isAfter(campusDay.getHoraCierre()))) {
                    warnings.add("La sala " + room.getCodigo() + " tiene horario fuera del rango general del campus (día " + override.getDiaSemana() + ")");
                }
            }
        }
        return warnings;
    }

    private void validateScheduleInputs(List<CampusScheduleDayInput> days, int slotMinutes) {
        for (CampusScheduleDayInput day : days) {
            validateDay(day.dayOfWeek());
            if (day.closed()) {
                continue;
            }
            if (day.openTime() == null || day.closeTime() == null || !day.closeTime().isAfter(day.openTime())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Horario general inválido para el día " + day.dayOfWeek());
            }
            if (!isAlignedToSlot(day.openTime(), slotMinutes) || !isAlignedToSlot(day.closeTime(), slotMinutes)) {
                throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "La apertura y cierre del día " + day.dayOfWeek()
                        + " deben alinearse con la duración por reserva de " + slotMinutes + " minutos"
                );
            }
        }
    }

    private void validateRoomScheduleInputs(List<RoomScheduleInput> days) {
        for (RoomScheduleInput day : days) {
            validateDay(day.dayOfWeek());
            if (day.closed()) {
                continue;
            }
            if (day.openTime() == null || day.closeTime() == null || !day.closeTime().isAfter(day.openTime())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Horario de sala inválido para el día " + day.dayOfWeek());
            }
        }
    }

    private void validateDay(int day) {
        if (day < DayOfWeek.MONDAY.getValue() || day > DayOfWeek.SUNDAY.getValue()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Día de semana inválido: " + day);
        }
    }

    private boolean isAlignedToSlot(LocalTime time, int slotMinutes) {
        return (time.getHour() * 60 + time.getMinute()) % slotMinutes == 0;
    }
}
