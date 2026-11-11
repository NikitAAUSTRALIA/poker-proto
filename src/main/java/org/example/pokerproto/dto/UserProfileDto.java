package org.example.pokerproto.dto;

public record UserProfileDto(
        Long tgId,
        String username,
        String firstName,
        boolean isAdmin,
        boolean termsAgreed,
        String nickname
) {}
