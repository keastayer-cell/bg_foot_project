package com.footballstats.backend.repository;

import com.footballstats.backend.domain.SeasonTransferRequest;
import com.footballstats.backend.domain.SeasonTransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import jakarta.persistence.LockModeType;

public interface SeasonTransferRequestRepository extends JpaRepository<SeasonTransferRequest, Long> {

  @EntityGraph(attributePaths = {"season", "player", "fromTeam", "toTeam"})
  @Query("""
    SELECT request
    FROM SeasonTransferRequest request
    WHERE request.season.id = :seasonId
      AND (request.fromTeam.id = :teamId OR request.toTeam.id = :teamId)
    ORDER BY request.requestedAt DESC, request.id DESC
    """)
  Page<SeasonTransferRequest> findPageDetailedBySeasonIdAndTeamId(
    @Param("seasonId") Long seasonId,
    @Param("teamId") Long teamId,
    Pageable pageable
  );

    @Query("""
        SELECT request
        FROM SeasonTransferRequest request
        JOIN FETCH request.season season
        JOIN FETCH request.player player
        JOIN FETCH request.fromTeam fromTeam
        JOIN FETCH request.toTeam toTeam
        WHERE request.season.id = :seasonId
          AND (request.fromTeam.id = :teamId OR request.toTeam.id = :teamId)
        ORDER BY request.requestedAt DESC, request.id DESC
        """)
    List<SeasonTransferRequest> findAllDetailedBySeasonIdAndTeamId(@Param("seasonId") Long seasonId, @Param("teamId") Long teamId);

    @Query("""
        SELECT request
        FROM SeasonTransferRequest request
        JOIN FETCH request.season season
        JOIN FETCH request.player player
        JOIN FETCH request.fromTeam fromTeam
        JOIN FETCH request.toTeam toTeam
        WHERE request.id = :requestId
        """)
    Optional<SeasonTransferRequest> findDetailedById(@Param("requestId") Long requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT request
        FROM SeasonTransferRequest request
        JOIN FETCH request.season season
        JOIN FETCH request.player player
        JOIN FETCH request.fromTeam fromTeam
        JOIN FETCH request.toTeam toTeam
        WHERE request.id = :requestId
        """)
    Optional<SeasonTransferRequest> findDetailedByIdForUpdate(@Param("requestId") Long requestId);

    @EntityGraph(attributePaths = {"player", "fromTeam", "toTeam"})
    @Query("""
      SELECT request
      FROM SeasonTransferRequest request
      WHERE request.season.id = :seasonId
      ORDER BY request.requestedAt DESC, request.id DESC
      """)
    Page<SeasonTransferRequest> findPageDetailedBySeasonId(@Param("seasonId") Long seasonId, Pageable pageable);

    @Query("""
      SELECT request
      FROM SeasonTransferRequest request
      JOIN FETCH request.player player
      JOIN FETCH request.fromTeam fromTeam
      JOIN FETCH request.toTeam toTeam
      WHERE request.season.id = :seasonId
      ORDER BY COALESCE(request.processedAt, request.requestedAt) DESC, request.id DESC
      """)
    List<SeasonTransferRequest> findAllDetailedBySeasonId(@Param("seasonId") Long seasonId);

    @EntityGraph(attributePaths = {"season", "player", "fromTeam", "toTeam"})
    @Query("""
        SELECT request
        FROM SeasonTransferRequest request
        WHERE request.status = com.footballstats.backend.domain.SeasonTransferStatus.PENDING
          AND request.fromTeam.id = :teamId
        ORDER BY request.requestedAt DESC, request.id DESC
        """)
    Page<SeasonTransferRequest> findIncomingPendingDetailedByTeamId(@Param("teamId") Long teamId, Pageable pageable);

    long countByStatusAndFromTeam_Id(SeasonTransferStatus status, Long teamId);

    boolean existsBySeason_IdAndPlayer_IdAndStatus(Long seasonId, Long playerId, SeasonTransferStatus status);

    boolean existsBySeason_IdAndPlayer_IdAndStatusIn(Long seasonId, Long playerId, Collection<SeasonTransferStatus> statuses);

    @Query("""
        SELECT DISTINCT request.player.id
        FROM SeasonTransferRequest request
        WHERE request.season.id = :seasonId
          AND request.status IN :statuses
        """)
    List<Long> findPlayerIdsBySeasonIdAndStatusIn(
        @Param("seasonId") Long seasonId,
        @Param("statuses") Collection<SeasonTransferStatus> statuses
    );
}
