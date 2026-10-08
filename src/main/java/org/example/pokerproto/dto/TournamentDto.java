package org.example.pokerproto.dto;

import java.time.Instant;

public record TournamentDto(
        Long id,
        String title,
        String excelUrl,
        Instant startsAt,
        Integer maxPlayers,
        long participantsCount,
        boolean registered,
        boolean finished,
        String seasonName
) {}
