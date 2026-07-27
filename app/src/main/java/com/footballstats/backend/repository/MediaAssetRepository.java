package com.footballstats.backend.repository;

import com.footballstats.backend.domain.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {

    Optional<MediaAsset> findFirstByOwnerTypeAndOwnerIdAndMediaKindAndActiveTrueOrderByIdDesc(
        String ownerType,
        Long ownerId,
        String mediaKind
    );

    @Query("""
        select asset
        from MediaAsset asset
        where asset.ownerType = :ownerType
          and asset.mediaKind = :mediaKind
          and asset.ownerId in :ownerIds
          and asset.active = true
          and not exists (
              select 1
              from MediaAsset newer
              where newer.ownerType = asset.ownerType
                and newer.ownerId = asset.ownerId
                and newer.mediaKind = asset.mediaKind
                and newer.active = true
                and newer.id > asset.id
          )
        order by asset.ownerId, asset.id desc
        """)
    List<MediaAsset> findActiveByOwners(
        @Param("ownerType") String ownerType,
        @Param("mediaKind") String mediaKind,
        @Param("ownerIds") Collection<Long> ownerIds
    );
}
