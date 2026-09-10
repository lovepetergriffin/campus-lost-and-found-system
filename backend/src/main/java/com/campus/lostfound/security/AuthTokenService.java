package com.campus.lostfound.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthTokenService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationSeconds;

    public AuthTokenService(ObjectMapper objectMapper,
                            @Value("${app.security.token-secret}") String secret,
                            @Value("${app.security.token-expiration-seconds:604800}") long expirationSeconds) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    public String create(UserPrincipal principal) {
        try {
            String header = encode(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", principal.id());
            payload.put("username", principal.username());
            payload.put("role", principal.role());
            payload.put("exp", Instant.now().getEpochSecond() + expirationSeconds);
            String body = encode(objectMapper.writeValueAsBytes(payload));
            String unsigned = header + "." + body;
            return unsigned + "." + encode(sign(unsigned));
        } catch (Exception ex) {
            throw new IllegalStateException("无法生成登录令牌", ex);
        }
    }

    public UserPrincipal parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            String unsigned = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(unsigned), DECODER.decode(parts[2]))) return null;
            Map<String, Object> payload = objectMapper.readValue(DECODER.decode(parts[1]), new TypeReference<>() {});
            long expiresAt = ((Number) payload.get("exp")).longValue();
            if (expiresAt < Instant.now().getEpochSecond()) return null;
            return new UserPrincipal(
                    ((Number) payload.get("sub")).longValue(),
                    String.valueOf(payload.get("username")),
                    String.valueOf(payload.get("role"))
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private byte[] sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(byte[] value) {
        return ENCODER.encodeToString(value);
    }
}
