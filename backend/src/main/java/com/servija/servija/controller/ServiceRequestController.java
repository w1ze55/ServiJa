package com.servija.servija.controller;

import com.servija.servija.dto.FreelancerResponseDTO;
import com.servija.servija.dto.ServiceRequestDTO;
import com.servija.servija.dto.ServiceRequestResponse;
import com.servija.servija.service.ServiceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/service-requests")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;

    @PostMapping
    public ResponseEntity<ServiceRequestResponse> create(
            @RequestParam Long contractorId,
            @RequestBody ServiceRequestDTO dto) {
        ServiceRequestResponse response = serviceRequestService.create(contractorId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<ServiceRequestResponse>> getMyRequests(
            @RequestParam Long contractorId) {
        return ResponseEntity.ok(serviceRequestService.getByContractor(contractorId));
    }

    @GetMapping("/received")
    public ResponseEntity<List<ServiceRequestResponse>> getReceivedRequests(
            @RequestParam Long freelancerId) {
        return ResponseEntity.ok(serviceRequestService.getByFreelancer(freelancerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceRequestResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceRequestService.getById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ServiceRequestResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        String reason = body.get("reason");
        return ResponseEntity.ok(serviceRequestService.updateStatus(id, status, reason));
    }

    @PatchMapping("/{id}/respond")
    public ResponseEntity<ServiceRequestResponse> respond(
            @PathVariable Long id,
            @RequestBody FreelancerResponseDTO dto) {
        return ResponseEntity.ok(serviceRequestService.respondToRequest(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serviceRequestService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
