package com.footballstats.backend.controller;

import com.footballstats.backend.security.AppUserPrincipal;
import com.footballstats.backend.service.RegulationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
public class RegulationController {
    private final RegulationService service;
    public RegulationController(RegulationService service) { this.service = service; }

    @GetMapping("/api/league/regulations")
    public List<RegulationService.Document> published() { return service.list(false); }

    @GetMapping("/api/admin/league/regulations")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public List<RegulationService.Document> targets() { return service.list(true); }

    @PutMapping("/api/admin/league/competitions/{id}/regulation")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public RegulationService.Document save(@PathVariable Long id, @Valid @RequestBody Upload request, Authentication auth) {
        return service.save(id, request.documentDataUrl(), ((AppUserPrincipal) auth.getPrincipal()).getUserId());
    }

    @DeleteMapping("/api/admin/league/competitions/{id}/regulation")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public RegulationService.Document remove(@PathVariable Long id, Authentication auth) {
        return service.remove(id, ((AppUserPrincipal) auth.getPrincipal()).getUserId());
    }

    @GetMapping("/api/competitions/{id}/regulation/pdf")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        var pdf = service.download(id);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(pdf.fileName(), StandardCharsets.UTF_8).build().toString())
            .contentLength(pdf.bytes().length).body(pdf.bytes());
    }

    public record Upload(@NotBlank String documentDataUrl) {}
}
