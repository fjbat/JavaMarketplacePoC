package com.dtme.marketplace.helper;

import java.util.Base64;
import java.util.UUID;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.common.GeneratePublicId;
import com.example.config.ConfigService;

/**
 * This class is responsible for generating and verifying the tokens issued when new accounts are registered
 * or when a password reset is requested.
 */
@Service
public class VerificationTokenGenerator {
    private final ConfigService configService;

    @Autowired
    public VerificationTokenGenerator(ConfigService configService) {
        this.configService = configService;
    }

    /**
     * Generates a verification token which encodes the time of generation and concatenates it with a
     * random id.
     */
    public String generateVerificationToken() {
        Instant now = Instant.now();
        String base64Now = Base64.getEncoder().encodeToString(now.toString().getBytes());
        String id = UUID.randomUUID().toString();
        return base64Now + "_" + id;
    }

    /**
     * Checks the age of the verification token to see if it falls within the token duration
     * as specified in the VendureConfig.
     */
    public boolean verifyVerificationToken(String token) {
        Duration duration = Duration.parse(this.configService.getAuthOptions().getVerificationTokenDuration());
        String[] parts = token.split("_");
        String generatedOnString = new String(Base64.getDecoder().decode(parts[0]));
        Instant generatedOn = Instant.parse(generatedOnString);
        Instant now = Instant.now();
        return Duration.between(generatedOn, now).compareTo(duration) < 0;
    }
}

