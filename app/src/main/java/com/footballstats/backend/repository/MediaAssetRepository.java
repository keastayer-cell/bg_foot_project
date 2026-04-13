package com.footballstats.backend.repository;

import com.footballstats.backend.domain.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {

    Optional<MediaAsset> findFirstByOwnerTypeAndOwnerIdAndMediaKindAndActiveTrueOrderByIdDesc(
        String ownerType,
        Long ownerId,
        String mediaKind
    );
}