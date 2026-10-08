package org.example.pokerproto.dto;

public record LeaderboardEntryDto(
        int rank,
        Long tgId,
        String username,
        String firstName,
        String lastName,
        int points,
        int tournamentsPlayed,
        int wins,
        int bestPlace
) {}
