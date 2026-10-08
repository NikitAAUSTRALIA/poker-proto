package org.example.pokerproto.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PublicProxyController {

    private final TournamentController tournamentController;

    @GetMapping("/public/proxy-excel")
    public ResponseEntity<byte[]> proxyExcel(@RequestParam("url") String url) {
        return tournamentController.proxyExcel(url);
    }
}
