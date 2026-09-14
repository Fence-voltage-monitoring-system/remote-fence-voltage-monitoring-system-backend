package com.nerdc.elephantfence.backend.reports.controller;
import com.nerdc.elephantfence.backend.reports.dto.ReportRequest;
import com.nerdc.elephantfence.backend.reports.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/reports") @RequiredArgsConstructor
public class ReportController {
    private final ReportService service;
    @GetMapping("/filters") public Map<String,Object> filters(@RequestParam(required=false) String province,@RequestParam(required=false) String district,@RequestParam(required=false) String fence) {
        return service.filters(new ReportRequest.Scope(province,district,fence,null));
    }
    @PostMapping("/preview") public Map<String,Object> preview(@Valid @RequestBody ReportRequest request) {return service.preview(request);}
    @PostMapping public Map<String,Object> generate(@Valid @RequestBody ReportRequest request) {return service.generate(request);}
    @GetMapping public Map<String,Object> history(@RequestParam(defaultValue="0") int page) {return service.history(page);}
    @GetMapping("/{id}/download") public ResponseEntity<byte[]> download(@PathVariable long id) {
        var file=service.download(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.format().equals("PDF")?"application/pdf":"text/csv;charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"report-"+id+"."+file.format().toLowerCase()+"\"")
            .cacheControl(CacheControl.noStore()).body(file.content());
    }
}
