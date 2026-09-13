package com.footballstats.backend.controller;
import com.footballstats.backend.service.BusinessAuditService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
@RestController
@RequestMapping("/api/admin/audit")
public class BusinessAuditController {
    private final BusinessAuditService service;
    public BusinessAuditController(BusinessAuditService service){this.service=service;}
    @GetMapping @PreAuthorize("hasRole('SUPER_ADMIN')")
    public BusinessAuditService.AuditPage list(@RequestParam(required=false) OffsetDateTime from,@RequestParam(required=false) OffsetDateTime to,
        @RequestParam(required=false) Long actorUserId,@RequestParam(required=false) String author,@RequestParam(required=false) String entityType,
        @RequestParam(required=false) Long entityId,@RequestParam(required=false) String action,
        @RequestParam(defaultValue="0") int pagenum,@RequestParam(defaultValue="25") int pagesize) {
        return service.list(from,to,actorUserId,author,entityType,entityId,action,pagenum,pagesize);
    }
}
