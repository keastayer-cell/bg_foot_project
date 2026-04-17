package com.footballstats.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class StandingsRankingRules {

    public static final String POINTS = "POINTS";
    public static final String GOAL_DIFFERENCE = "GOAL_DIFFERENCE";
    public static final String GOALS_FOR = "GOALS_FOR";
    public static final String WINS = "WINS";
    public static final String HEAD_TO_HEAD = "HEAD_TO_HEAD";
    public static final String ALPHABETICAL = "ALPHABETICAL";

    public static final List<String> DEFAULT_RULES = List.of(
        POINTS,
        GOAL_DIFFERENCE,
        GOALS_FOR,
        ALPHABETICAL
    );

    private static final Set<String> SUPPORTED_RULES = Set.of(
        POINTS,
        GOAL_DIFFERENCE,
        GOALS_FOR,
        WINS,
        HEAD_TO_HEAD,
        ALPHABETICAL
    );

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private StandingsRankingRules() {
    }

    public static List<String> fromJson(String rawJson, ObjectMapper objectMapper) {
        if (rawJson == null || rawJson.isBlank()) {
            return DEFAULT_RULES;
        }

        try {
            List<String> parsed = objectMapper.readValue(rawJson, STRING_LIST);
            return normalize(parsed);
        } catch (JsonProcessingException ignored) {
            return DEFAULT_RULES;
        }
    }

    public static String toJson(List<String> rawRules, ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(normalize(rawRules));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Не удалось сохранить правила сортировки таблицы.", exception);
        }
    }

    public static List<String> normalize(List<String> rawRules) {
        List<String> normalized = new ArrayList<>();
        LinkedHashSet<String> uniqueRules = new LinkedHashSet<>();

        if (rawRules != null) {
            for (String rawRule : rawRules) {
                String normalizedRule = normalizeCode(rawRule);
                if (normalizedRule != null && !ALPHABETICAL.equals(normalizedRule)) {
                    uniqueRules.add(normalizedRule);
                }
            }
        }

        uniqueRules.remove(POINTS);
        normalized.add(POINTS);
        normalized.addAll(uniqueRules);
        normalized.add(ALPHABETICAL);

        if (normalized.size() <= 2) {
            return DEFAULT_RULES;
        }

        return List.copyOf(normalized);
    }

    private static String normalizeCode(String rawRule) {
        String normalized = String.valueOf(rawRule == null ? "" : rawRule)
            .trim()
            .toUpperCase(Locale.ROOT);
        if (normalized.isEmpty() || !SUPPORTED_RULES.contains(normalized)) {
            return null;
        }
        return normalized;
    }
}