package com.hoandev.pinedrink.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Component
public class GoogleTokenVerifier {

    private final String clientId;
    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(@Value("${app.oauth2.google.client-id:}") String clientId) {
        this.clientId = clientId;
        this.verifier = buildVerifier(clientId);
    }

    public GoogleUserInfo verify(String rawIdToken) {
        if (rawIdToken == null || rawIdToken.isBlank()) {
            throw new BaseException(ErrorCode.AUTH_GOOGLE_001);
        }
        if (clientId == null || clientId.isBlank()) {
            throw new BaseException(ErrorCode.AUTH_GOOGLE_002, "Google client ID is not configured");
        }

        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(rawIdToken);
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            throw new BaseException(ErrorCode.AUTH_GOOGLE_002);
        }

        if (idToken == null) {
            throw new BaseException(ErrorCode.AUTH_GOOGLE_002);
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new BaseException(ErrorCode.AUTH_GOOGLE_003);
        }

        String subject = payload.getSubject();
        String email = payload.getEmail();
        if (subject == null || subject.isBlank() || email == null || email.isBlank()) {
            throw new BaseException(ErrorCode.AUTH_GOOGLE_002);
        }

        return new GoogleUserInfo(
                subject,
                email.trim().toLowerCase(),
                stringClaim(payload, "name"),
                stringClaim(payload, "picture")
        );
    }

    private GoogleIdTokenVerifier buildVerifier(String clientId) {
        try {
            return new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance()
            ).setAudience(Collections.singletonList(clientId)).build();
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Failed to initialize Google token verifier", e);
        }
    }

    private String stringClaim(GoogleIdToken.Payload payload, String claimName) {
        Object value = payload.get(claimName);
        return value instanceof String text && !text.isBlank() ? text : null;
    }

    @Getter
    public static class GoogleUserInfo {
        private final String subject;
        private final String email;
        private final String name;
        private final String picture;

        public GoogleUserInfo(String subject, String email, String name, String picture) {
            this.subject = subject;
            this.email = email;
            this.name = name;
            this.picture = picture;
        }
    }
}
