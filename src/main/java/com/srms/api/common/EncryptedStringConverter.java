package com.srms.api.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

/**
 * Encrypts a column's value at rest (AES-256/GCM, via Spring Security Crypto — already a
 * transitive dependency of spring-boot-starter-security, no new library needed). This is the
 * first place the app stores a third-party credential in its own database rather than injecting
 * it as a platform-level env var (compare {@code ZynlePayClient}, which reads its API key from
 * a JVM property and never touches the database — that pattern doesn't fit a per-school,
 * admin-entered-through-a-UI credential like an integration's API key).
 *
 * Transparent to callers on the Java side: entity fields using this converter always hold
 * plaintext in memory. Only the database column holds ciphertext. Callers still must never echo
 * a decrypted value back to the frontend — see IntegrationConnectionView's masking.
 */
@Component
@Converter(autoApply = false)
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    private final TextEncryptor encryptor;

    public EncryptedStringConverter(
            @Value("${integration.encryption.key}") String key,
            @Value("${integration.encryption.salt}") String salt) {
        this.encryptor = Encryptors.delux(key, salt);
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) return null;
        return encryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        try {
            return encryptor.decrypt(dbData);
        } catch (Exception e) {
            // A value written under a different key (e.g. after a key rotation) is unreadable,
            // not a reason to fail loading the whole row — treat it as absent.
            return null;
        }
    }
}
