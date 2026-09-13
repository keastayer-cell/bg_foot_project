package com.footballstats.backend.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name="w_business_audit_log",schema="work")
public class BusinessAuditEntry {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="entity_type",nullable=false,length=40) private String entityType;
    @Column(name="entity_id",nullable=false) private Long entityId;
    @Column(name="action_code",nullable=false,length=60) private String actionCode;
    @Column(name="actor_user_id") private Long actorUserId;
    @Column(name="actor_name",length=160) private String actorName;
    @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
    @Column(name="changes_json",nullable=false,columnDefinition="text") private String changesJson;
    protected BusinessAuditEntry() {}
    public BusinessAuditEntry(String type,Long entityId,String action,Long actor,String name,String changes) {
        this.entityType=type;this.entityId=entityId;this.actionCode=action;this.actorUserId=actor;
        this.actorName=name;this.changesJson=changes;this.createdAt=OffsetDateTime.now();
    }
    public Long getId(){return id;}
    public String getEntityType(){return entityType;}
    public Long getEntityId(){return entityId;}
    public String getActionCode(){return actionCode;}
    public Long getActorUserId(){return actorUserId;}
    public String getActorName(){return actorName;}
    public OffsetDateTime getCreatedAt(){return createdAt;}
    public String getChangesJson(){return changesJson;}
}
