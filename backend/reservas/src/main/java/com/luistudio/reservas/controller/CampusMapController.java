package com.luistudio.reservas.controller;

import com.luistudio.reservas.dto.admin.BuildingLocationRequest;
import com.luistudio.reservas.dto.admin.CampusMapResponse;
import com.luistudio.reservas.exception.NotFoundException;
import com.luistudio.reservas.repository.PabellonRepository;
import com.luistudio.reservas.service.AccessGuard;
import com.luistudio.reservas.service.CampusMapService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CampusMapController {
    private final AccessGuard guard; private final CampusMapService service; private final PabellonRepository buildings;
    public CampusMapController(AccessGuard guard, CampusMapService service, PabellonRepository buildings) {
        this.guard = guard; this.service = service; this.buildings = buildings;
    }
    @GetMapping("/campus/map")
    public CampusMapResponse map(@RequestParam(required = false) String campus) { guard.requireUser(); return service.getCampusMap(campus); }
    @PutMapping("/admin/buildings/{id}/location")
    public CampusMapResponse.Pavilion updateLocation(@PathVariable Long id, @Valid @RequestBody BuildingLocationRequest request) {
        guard.requireAdmin();
        var building = buildings.findById(id).orElseThrow(() -> new NotFoundException("Pabellón no encontrado"));
        building.setLatitude(request.latitude()); building.setLongitude(request.longitude()); buildings.save(building); service.evictAll();
        return service.getCampusMap(building.getCampus().getNombre()).campuses().stream().flatMap(c -> c.pavilions().stream()).filter(p -> p.id().equals(id)).findFirst().orElseThrow();
    }
}
