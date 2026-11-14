CREATE TABLE identity_user (
    id UUID PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_identity_user_display_name
        CHECK (display_name ~ '[^[:space:]]'),

    CONSTRAINT chk_identity_user_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED')),

    CONSTRAINT chk_identity_user_updated_at
        CHECK (
            updated_at IS NULL
            OR (updated_at >= created_at)
        ),

    CONSTRAINT chk_identity_user_deleted_at
        CHECK (
            deleted_at IS NULL
            OR (deleted_at >= COALESCE(updated_at, created_at))
        )
);

CREATE TABLE local_credential (
    user_id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email_verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_local_credential_email
        CHECK (email ~ '[^[:space:]]'),

    CONSTRAINT chk_local_credential_password_hash
        CHECK (password_hash ~ '[^[:space:]]'),

    CONSTRAINT chk_local_credential_updated_at
        CHECK (
            updated_at IS NULL
            OR (updated_at >= created_at)
        ),

    CONSTRAINT chk_local_credential_email_verified_at
        CHECK (
            email_verified_at IS NULL
            OR (
                updated_at IS NOT NULL
                AND email_verified_at >= created_at
                AND email_verified_at <= updated_at
            )
        )
);

CREATE TABLE email_verification_token (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    email VARCHAR(254) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_email_verification_token_email
        CHECK (email ~ '[^[:space:]]'),

    CONSTRAINT chk_email_verification_token_hash
        CHECK (token_hash ~ '[^[:space:]]'),

    CONSTRAINT chk_email_verification_token_expires_at
        CHECK (expires_at > created_at),

    CONSTRAINT chk_email_verification_token_consumed_at
        CHECK (
            consumed_at IS NULL
            OR (consumed_at >= created_at AND consumed_at < expires_at)
        ),

    CONSTRAINT chk_email_verification_token_revoked_at
        CHECK (
            revoked_at IS NULL
            OR revoked_at >= created_at
        ),

    CONSTRAINT chk_email_verification_token_nand_consumed_at_revoked_at
        CHECK (consumed_at IS NULL OR revoked_at IS NULL)
);

CREATE TABLE external_identity (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    provider VARCHAR(100) NOT NULL,
    issuer VARCHAR(2048) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    unlinked_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_external_identity_provider
        CHECK (provider ~ '[^[:space:]]'),

    CONSTRAINT chk_external_identity_issuer
        CHECK (issuer ~ '[^[:space:]]'),

    CONSTRAINT chk_external_identity_subject
        CHECK (subject ~ '[^[:space:]]'),

    CONSTRAINT chk_external_identity_unlinked_at
        CHECK (
            unlinked_at IS NULL
            OR (unlinked_at >= created_at)
        )
);

ALTER TABLE local_credential
    ADD CONSTRAINT fk_local_credential_user
    FOREIGN KEY (user_id)
    REFERENCES identity_user (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE email_verification_token
    ADD CONSTRAINT fk_email_verification_token_user
    FOREIGN KEY (user_id)
    REFERENCES identity_user (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE external_identity
    ADD CONSTRAINT fk_external_identity_user
    FOREIGN KEY (user_id)
    REFERENCES identity_user (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT;

CREATE UNIQUE INDEX uk_local_credential_email
    ON local_credential (email);

CREATE UNIQUE INDEX uk_external_identity
    ON external_identity (issuer, subject)
    WHERE unlinked_at IS NULL;

CREATE INDEX idx_email_verification_token_user
    ON email_verification_token (user_id);

CREATE INDEX idx_email_verification_token_hash
    ON email_verification_token (token_hash);

CREATE INDEX idx_external_identity_user
    ON external_identity (user_id);
