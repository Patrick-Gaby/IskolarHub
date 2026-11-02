-- One optional-field profile per user. NULL means "not provided" and must never be read as zero.
CREATE TABLE student_profiles (
                                  id                       uuid          NOT NULL DEFAULT gen_random_uuid(),
                                  user_id                  uuid          NOT NULL,
                                  gwa                      numeric(5,2),
                                  gwa_scale                varchar(20),
                                  family_income_annual_php numeric(14,2),
                                  region_code              varchar(20),
                                  course_code              varchar(50),
                                  year_level               integer,
                                  created_at               timestamptz   NOT NULL DEFAULT now(),
                                  updated_at               timestamptz   NOT NULL DEFAULT now(),
                                  CONSTRAINT pk_student_profiles PRIMARY KEY (id),
                                  CONSTRAINT uq_student_profiles_user UNIQUE (user_id),
                                  CONSTRAINT fk_student_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                                  CONSTRAINT ck_student_profiles_gwa_pair CHECK ((gwa IS NULL) = (gwa_scale IS NULL)),
                                  CONSTRAINT ck_student_profiles_gwa_scale CHECK (gwa_scale IS NULL OR gwa_scale IN ('PH_1_TO_5', 'PERCENTAGE', 'GPA_4_0')),
                                  CONSTRAINT ck_student_profiles_gwa_range CHECK (
                                      gwa IS NULL
                                          OR (gwa_scale = 'PH_1_TO_5'  AND gwa BETWEEN 1 AND 5)
                                          OR (gwa_scale = 'PERCENTAGE' AND gwa BETWEEN 0 AND 100)
                                          OR (gwa_scale = 'GPA_4_0'    AND gwa BETWEEN 0 AND 4)
                                      ),
                                  CONSTRAINT ck_student_profiles_income CHECK (family_income_annual_php IS NULL OR family_income_annual_php >= 0),
                                  CONSTRAINT ck_student_profiles_region CHECK (region_code IS NULL OR region_code ~ '^[A-Z0-9_]{2,20}$'),
    CONSTRAINT ck_student_profiles_course CHECK (course_code IS NULL OR course_code ~ '^[A-Z0-9_]{2,50}$'),
    CONSTRAINT ck_student_profiles_year CHECK (year_level IS NULL OR year_level BETWEEN 1 AND 8)
);
