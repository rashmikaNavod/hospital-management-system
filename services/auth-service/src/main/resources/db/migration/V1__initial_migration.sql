CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_users_email CHECK (
        email = BTRIM(email) AND email <> ''
    ),
    CONSTRAINT chk_users_password_hash CHECK (BTRIM(password_hash) <> '')
);

CREATE UNIQUE INDEX uq_users_email_lower ON users (LOWER(email));

--  <> කියන්නේ SQL වල “සමාන නොවේ” කියන එක.
--  CONSTRAINT chk_users_password_hash CHECK (BTRIM(password_hash) <> '') meken wenne CHECH(TRUE) unoth pass wenw
--  CHECK(FALSE) unoth save wenne ne database eke fail wenawa
--

CREATE TABLE roles (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    CONSTRAINT uq_roles_name UNIQUE (name),
    CONSTRAINT chk_roles_name CHECK (
        name = UPPER(BTRIM(name)) AND name <> ''
    )
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id)
        REFERENCES roles (id) ON DELETE RESTRICT
);

CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);

--  ON DELETE CASCADE නිසා user කෙනෙක් delete කළොත් ඔහුගේ user_roles rows ටිකත් delete වෙනවා.
--  ON DELETE RESTRICT නිසා usersලාට assign කරලා තියෙන role එකක් delete කරන්න බැහැ. උදාහරණයක්
--  ලෙස DOCTOR role එක usersලා භාවිත කරනවා නම් ඒ role එක delete කිරීම නවත්වනවා.

INSERT INTO roles (name)
VALUES ('PATIENT'), ('DOCTOR'), ('NURSE'), ('ADMIN');
