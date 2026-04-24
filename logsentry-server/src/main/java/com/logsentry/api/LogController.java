package com.logsentry.api;

import com.logsentry.model.LogEvent;
import com.logsentry.repository.LogEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LogEventRepository logEventRepository;

    public LogController(LogEventRepository logEventRepository) {
        this.logEventRepository = logEventRepository;
    }

    @GetMapping
    public ResponseEntity<Page<LogEvent>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<LogEvent> logs = logEventRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp")));
        return ResponseEntity.ok(logs);
    }

    @PostMapping
    public ResponseEntity<LogEvent> createLog(@RequestBody LogEvent logEvent) {
        LogEvent saved = logEventRepository.save(logEvent);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getCount() {
        return ResponseEntity.ok(logEventRepository.count());
    }

    @GetMapping("/sources")
    public ResponseEntity<?> getSources() {
        return ResponseEntity.ok(logEventRepository.findDistinctSources());
    }
}
