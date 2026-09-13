package com.footballstats.backend.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "w_user_favorite", schema = "work")
public class UserFavorite {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 16)
    private FavoriteType targetType;
    @Column(name = "target_id", nullable = false)
    private Long targetId;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
    public Long getId(){return id;} public AppUser getUser(){return user;} public void setUser(AppUser value){user=value;}
    public FavoriteType getTargetType(){return targetType;} public void setTargetType(FavoriteType value){targetType=value;}
    public Long getTargetId(){return targetId;} public void setTargetId(Long value){targetId=value;}
    public OffsetDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(OffsetDateTime value){createdAt=value;}
}
