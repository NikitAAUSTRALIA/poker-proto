package org.example.pokerproto.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Slf4j
@Service
public class InitDataValidator {

    private final String botToken;
    private final ObjectMapper mapper = new ObjectMapper();

    public InitDataValidator(@Value("${telegram.bot.token}") String botToken) {
        this.botToken = botToken;
    }

    public Long extractTgId(String initData) {
        Map<String, String> params = parse(initData);
        String hash = params.remove("hash");
        if (hash == null || !validate(params, hash)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad initData");
        }

        String userJson = params.get("user");
        if (userJson == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No user in initData");
        }

        try {
            Map<?, ?> user = mapper.readValue(userJson, Map.class);
            Object id = user.get("id");
            if (id == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No id in user");
            }
            return Long.valueOf(id.toString());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad user json");
        }
    }

    private boolean validate(Map<String, String> params, String hash) {
        try {
            String dataCheckString = params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");

            byte[] secretKey = hmac("WebAppData".getBytes(StandardCharsets.UTF_8),
                    botToken.getBytes(StandardCharsets.UTF_8));
            byte[] calculated = hmac(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8));

            return hex(calculated).equals(hash);
        } catch (Exception e) {
            log.error("initData validation failed", e);
            return false;
        }
    }

    private static Map<String, String> parse(String initData) {
        Map<String, String> map = new HashMap<>();
        for (String pair : initData.split("&")) {
            int i = pair.indexOf('=');
            if (i < 0) continue;
            String key = pair.substring(0, i);
            String value = URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8);
            map.put(key, value);
        }
        return map;
    }

    private static byte[] hmac(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
