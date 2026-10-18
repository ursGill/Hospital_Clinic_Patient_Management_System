-- Run once as HCPMS_USER after APP_USERS already exists.
-- Links doctor login accounts to exactly one record in DOCTORS.
ALTER TABLE app_users ADD (doctor_id NUMBER NULL);
ALTER TABLE app_users ADD CONSTRAINT fk_app_users_doctor
    FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id);

-- Optional consistency rule: only doctor accounts may carry a doctor_id.
ALTER TABLE app_users ADD CONSTRAINT ck_app_users_doctor_role
    CHECK ((role = 'DOCTOR' AND doctor_id IS NOT NULL)
        OR (role IN ('ADMIN', 'RECEPTIONIST') AND doctor_id IS NULL));
