package com.clinic.pms.service;

import com.clinic.pms.entity.DoctorClinicProfile;
import com.clinic.pms.exception.NotFoundException;
import com.clinic.pms.repository.DoctorClinicProfileRepository;

public class ClinicProfileService {

    private final DoctorClinicProfileRepository repository;

    public ClinicProfileService(DoctorClinicProfileRepository repository) {
        this.repository = repository;
    }

    public DoctorClinicProfile get() {
        return repository.findFirst().orElseThrow(() -> new NotFoundException("Clinic profile has not been set up yet"));
    }

    public void saveInitialProfile(String doctorName, String clinicName, String contactDetails, String registrationNumber) {
        DoctorClinicProfile profile = new DoctorClinicProfile();
        profile.setDoctorName(doctorName);
        profile.setClinicName(clinicName);
        profile.setContactDetails(contactDetails);
        profile.setRegistrationNumber(registrationNumber);
        repository.save(profile);
    }

    public DoctorClinicProfile update(String doctorName, String clinicName, String contactDetails, String registrationNumber) {
        DoctorClinicProfile profile = get();
        profile.setDoctorName(doctorName);
        profile.setClinicName(clinicName);
        profile.setContactDetails(contactDetails);
        profile.setRegistrationNumber(registrationNumber);
        return repository.save(profile);
    }
}
