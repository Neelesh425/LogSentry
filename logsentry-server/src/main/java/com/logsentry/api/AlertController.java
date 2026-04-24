package com.logsentry.api;

import com.logsentry.model.AnomalyAlert;
import com.logsentry.repository.AnomalyAlertRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AnomalyAlertRepository alertRepository;

    public AlertController(AnomalyAlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @GetMapping
    public ResponseEntity<Page<AnomalyAlert>> getAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<AnomalyAlert> alerts = alertRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "detectedAt")));
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/active")
    public ResponseEntity<List<AnomalyAlert>> getActiveAlerts() {
        return ResponseEntity.ok(alertRepository.findByResolvedFalseOrderByDetectedAtDesc());
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<AnomalyAlert> resolveAlert(@PathVariable Long id) {
        return alertRepository.findById(id)
                .map(alert -> {
                    alert.setResolved(true);
                    alert.setResolvedAt(Instant.now());
                    return ResponseEntity.ok(alertRepository.save(alert));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/count/active")
    public ResponseEntity<Long> getActiveCount() {
        return ResponseEntity.ok(alertRepository.countActiveAlerts());
    }
}
