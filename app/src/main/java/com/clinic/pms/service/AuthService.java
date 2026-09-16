package com.clinic.pms.service;

import com.clinic.pms.entity.DoctorCredential;
import com.clinic.pms.exception.BadRequestException;
import com.clinic.pms.repository.DoctorCredentialRepository;
import java.time.LocalDateTime;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {

    public static final String RECOVERY_QUESTION = "What is the email id of this app creator's wife";
    private static final String RECOVERY_ANSWER_HASH =
            "$2a$10$zfnT13fvvlPVNP6bYl6YTea/zInCVMPKDbbl8pJNaWaaJYZKOzlFO";

    private final DoctorCredentialRepository credentialRepository;

    public AuthService(DoctorCredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    public boolean isInitialized() {
        return credentialRepository.count() > 0;
    }

    /** First-run setup only (FR-021): creates the single credential row; refuses if one already exists. */
    public void createInitialCredential(String username, String rawPassword) {
        if (isInitialized()) {
            throw new BadRequestException("Login credentials have already been set up.");
        }
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw new BadRequestException("Username and password are required.");
        }
        DoctorCredential credential = new DoctorCredential();
        credential.setUsername(username.trim());
        credential.setPasswordHash(BCrypt.hashpw(rawPassword, BCrypt.gensalt()));
        credential.setRecoveryAnswerHash(RECOVERY_ANSWER_HASH);
        LocalDateTime now = LocalDateTime.now();
        credential.setCreatedAt(now);
        credential.setUpdatedAt(now);
        credentialRepository.save(credential);
    }

    /**
     * Returns {@code false} uniformly for a wrong username OR password
     * (acceptance scenario US1.2) — never reveals which field was wrong.
     */
    public boolean login(String username, String rawPassword) {
        if (username == null || rawPassword == null) {
            return false;
        }
        return credentialRepository.findByUsername(username)
                .map(c -> BCrypt.checkpw(rawPassword, c.getPasswordHash()))
                .orElse(false);
    }

    public boolean resetPassword(String username, String recoveryAnswer, String newPassword) {
        if (username == null || recoveryAnswer == null || newPassword == null || newPassword.isBlank()) {
            return false;
        }
        return credentialRepository.findByUsername(username.trim())
                .filter(c -> c.getRecoveryAnswerHash() != null
                        && BCrypt.checkpw(recoveryAnswer.trim(), c.getRecoveryAnswerHash()))
                .map(c -> {
                    c.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
                    c.setUpdatedAt(LocalDateTime.now());
                    credentialRepository.save(c);
                    return true;
                })
                .orElse(false);
    }
}
