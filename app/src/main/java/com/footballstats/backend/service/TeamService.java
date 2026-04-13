package com.footballstats.backend.service;

import com.footballstats.backend.domain.Team;
import com.footballstats.backend.domain.MediaAsset;
import com.footballstats.backend.domain.SeasonTeam;
import com.footballstats.backend.repository.SeasonTeamRepository;
import com.footballstats.backend.repository.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final SeasonTeamRepository seasonTeamRepository;
    private final MediaAssetService mediaAssetService;

    public TeamService(
        TeamRepository teamRepository,
        SeasonTeamRepository seasonTeamRepository,
        MediaAssetService mediaAssetService
    ) {
        this.teamRepository = teamRepository;
        this.seasonTeamRepository = seasonTeamRepository;
        this.mediaAssetService = mediaAssetService;
    }

    @Transactional(readOnly = true)
    public List<Team> listTeams(boolean includeInactive, Long seasonId) {
        if (seasonId != null) {
            return seasonTeamRepository.findAllBySeasonIdOrderByTeamNameAsc(seasonId).stream()
                .map(SeasonTeam::getTeam)
                .filter(team -> includeInactive || team.isActive())
                .toList();
        }
        return includeInactive
            ? teamRepository.findAllByOrderByNameAsc()
            : teamRepository.findAllByActiveTrueOrderByNameAsc();
    }

    @Transactional
    public Team createTeam(TeamUpsertData data, Long actorUserId) {
        String name = normalizeRequired(data.name(), "Название команды обязательно.");
        validateUniqueName(name, null);

        Team team = new Team();
        team.setName(name);
        team.setShortName(normalizeOptional(data.shortName()));
        team.setCity(normalizeOptional(data.city()));
        team.setLogoDataUrl(normalizeOptional(data.logoDataUrl()));
        team.setActive(true);
        team.setCreatedByUserId(actorUserId);
        team.setUpdatedByUserId(actorUserId);
        team.setUpdatedAt(OffsetDateTime.now());

        Team saved = teamRepository.save(team);
        MediaAsset logo = mediaAssetService.saveAsset(
            MediaAssetService.OWNER_TEAM,
            saved.getId(),
            MediaAssetService.KIND_TEAM_LOGO,
            data.logoDataUrl(),
            actorUserId
        );
        if (logo != null) {
            saved.setLogoMediaId(logo.getId());
            saved.setLogoDataUrl(logo.getDataUrl());
            saved = teamRepository.save(saved);
        }
        return saved;
    }

    @Transactional
    public Team updateTeam(Long teamId, TeamUpsertData data, Long actorUserId) {
        Team team = getExistingTeam(teamId);

        String name = normalizeRequired(data.name(), "Название команды обязательно.");
        validateUniqueName(name, teamId);

        team.setName(name);
        team.setShortName(normalizeOptional(data.shortName()));
        team.setCity(normalizeOptional(data.city()));
        team.setUpdatedByUserId(actorUserId);
        team.setUpdatedAt(OffsetDateTime.now());
        Team saved = teamRepository.save(team);

        MediaAsset logo = mediaAssetService.saveAsset(
            MediaAssetService.OWNER_TEAM,
            saved.getId(),
            MediaAssetService.KIND_TEAM_LOGO,
            data.logoDataUrl(),
            actorUserId
        );
        if (logo != null) {
            saved.setLogoMediaId(logo.getId());
            saved.setLogoDataUrl(logo.getDataUrl());
            saved = teamRepository.save(saved);
        } else {
            saved.setLogoDataUrl(mediaAssetService.loadDataUrl(
                MediaAssetService.OWNER_TEAM,
                saved.getId(),
                MediaAssetService.KIND_TEAM_LOGO
            ));
        }

        return saved;
    }

    @Transactional
    public Team deactivateTeam(Long teamId, Long actorUserId) {
        Team team = getExistingTeam(teamId);
        if (!team.isActive()) {
            return team;
        }

        team.setActive(false);
        team.setUpdatedByUserId(actorUserId);
        team.setUpdatedAt(OffsetDateTime.now());
        return teamRepository.save(team);
    }

    private Team getExistingTeam(Long teamId) {
        return teamRepository.findById(teamId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Команда не найдена."));
    }

    private void validateUniqueName(String name, Long currentTeamId) {
        teamRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (currentTeamId == null || !existing.getId().equals(currentTeamId)) {
                throw new IllegalArgumentException("Команда с таким названием уже существует.");
            }
        });
    }

    private String normalizeRequired(String value, String errorMessage) {
        String normalized = String.valueOf(value == null ? "" : value).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        String normalized = String.valueOf(value == null ? "" : value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public record TeamUpsertData(String name, String shortName, String city, String logoDataUrl) {}
}