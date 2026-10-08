package org.example.pokerproto.dto;

import java.time.Instant;

public record SaveTournamentRequest(
        String title,
        String excelUrl,
        Instant startsAt,
        Integer maxPlayers
) {}
