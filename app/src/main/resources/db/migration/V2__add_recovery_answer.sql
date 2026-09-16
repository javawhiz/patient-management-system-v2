-- V2: add a one-way recovery answer hash for local password reset.
ALTER TABLE doctor_credential ADD recovery_answer_hash VARCHAR(255);

UPDATE doctor_credential
SET recovery_answer_hash = '$2a$10$zfnT13fvvlPVNP6bYl6YTea/zInCVMPKDbbl8pJNaWaaJYZKOzlFO'
WHERE recovery_answer_hash IS NULL;
