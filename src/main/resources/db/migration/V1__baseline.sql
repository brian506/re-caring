CREATE TABLE public.anomaly_detections (
    anomaly_detection_id bigint NOT NULL,
    ward_member_key character varying(255) NOT NULL,
    detection_type character varying(50) NOT NULL,
    score double precision NOT NULL,
    recorded_at timestamp without time zone NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    evidence character varying(1000),
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone,
    alert_kind character varying(30),
    event_id character varying(100),
    event_started_at timestamp without time zone,
    event_ended_at timestamp without time zone,
    categories character varying(200),
    reason_code character varying(50),
    duration_min integer,
    usual_min integer,
    speed_kmh integer,
    event_hour integer,
    remind_seq integer
);

CREATE SEQUENCE public.anomaly_detections_anomaly_detection_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.anomaly_detections_anomaly_detection_id_seq OWNED BY public.anomaly_detections.anomaly_detection_id;

CREATE TABLE public.care_invitation (
    care_invitation_id bigint NOT NULL,
    request_key character varying(255) NOT NULL,
    requester_member_key character varying(255) NOT NULL,
    target_member_key character varying(255) NOT NULL,
    ward_member_key character varying(255) NOT NULL,
    care_role character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);

CREATE SEQUENCE public.care_invitation_care_invitation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.care_invitation_care_invitation_id_seq OWNED BY public.care_invitation.care_invitation_id;

CREATE TABLE public.care_relationships (
    care_relationship_id bigint NOT NULL,
    ward_member_key character varying(255) NOT NULL,
    caregiver_member_key character varying(255) NOT NULL,
    care_role character varying(20) NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    ward_nickname character varying(20)
);

CREATE SEQUENCE public.care_relationships_care_relationship_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.care_relationships_care_relationship_id_seq OWNED BY public.care_relationships.care_relationship_id;

CREATE TABLE public.designated_avatars (
    designated_avatar_id bigint NOT NULL,
    owner_member_key character varying(255) NOT NULL,
    ward_member_key character varying(255) NOT NULL,
    target_member_key character varying(255) NOT NULL,
    profile_avatar_code character varying(30) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone
);

CREATE SEQUENCE public.designated_avatars_designated_avatar_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.designated_avatars_designated_avatar_id_seq OWNED BY public.designated_avatars.designated_avatar_id;

CREATE TABLE public.fcm_device_tokens (
    fcm_device_token_id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    member_key character varying(255) NOT NULL,
    recipient_type character varying(20) NOT NULL,
    token character varying(512) NOT NULL,
    platform character varying(20)
);

CREATE SEQUENCE public.fcm_device_tokens_fcm_device_token_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.fcm_device_tokens_fcm_device_token_id_seq OWNED BY public.fcm_device_tokens.fcm_device_token_id;

CREATE TABLE public.gps_histories (
    gps_history_id bigint NOT NULL,
    ward_member_key character varying(255) NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    recorded_at timestamp without time zone NOT NULL,
    battery integer,
    accuracy double precision,
    speed double precision,
    measured_at timestamp without time zone
)
PARTITION BY RANGE (recorded_at);

CREATE SEQUENCE public.gps_histories_gps_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.gps_histories_gps_history_id_seq OWNED BY public.gps_histories.gps_history_id;

CREATE TABLE public.gps_histories_2026_08 (
    gps_history_id bigint DEFAULT nextval('public.gps_histories_gps_history_id_seq'::regclass) NOT NULL,
    ward_member_key character varying(255) NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    recorded_at timestamp without time zone NOT NULL,
    battery integer,
    accuracy double precision,
    speed double precision,
    measured_at timestamp without time zone
);

CREATE TABLE public.gps_histories_2026_09 (
    gps_history_id bigint DEFAULT nextval('public.gps_histories_gps_history_id_seq'::regclass) NOT NULL,
    ward_member_key character varying(255) NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    recorded_at timestamp without time zone NOT NULL,
    battery integer,
    accuracy double precision,
    speed double precision,
    measured_at timestamp without time zone
);

CREATE TABLE public.gps_histories_2026_10 (
    gps_history_id bigint DEFAULT nextval('public.gps_histories_gps_history_id_seq'::regclass) NOT NULL,
    ward_member_key character varying(255) NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    recorded_at timestamp without time zone NOT NULL,
    battery integer,
    accuracy double precision,
    speed double precision,
    measured_at timestamp without time zone
);

CREATE TABLE public.gps_histories_2026_11 (
    gps_history_id bigint DEFAULT nextval('public.gps_histories_gps_history_id_seq'::regclass) NOT NULL,
    ward_member_key character varying(255) NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    recorded_at timestamp without time zone NOT NULL,
    battery integer,
    accuracy double precision,
    speed double precision,
    measured_at timestamp without time zone
);

CREATE TABLE public.gps_histories_default (
    gps_history_id bigint DEFAULT nextval('public.gps_histories_gps_history_id_seq'::regclass) NOT NULL,
    ward_member_key character varying(255) NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    recorded_at timestamp without time zone NOT NULL,
    battery integer,
    accuracy double precision,
    speed double precision,
    measured_at timestamp without time zone
);

CREATE TABLE public.inquiries (
    inquiry_id bigint NOT NULL,
    inquiry_key character varying(255) NOT NULL,
    member_key character varying(255) NOT NULL,
    category character varying(30) NOT NULL,
    title character varying(50) NOT NULL,
    content character varying(1000) NOT NULL,
    status character varying(20) NOT NULL,
    answer character varying(2000),
    answered_at timestamp without time zone,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone
);

CREATE SEQUENCE public.inquiries_inquiry_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.inquiries_inquiry_id_seq OWNED BY public.inquiries.inquiry_id;

CREATE TABLE public.local_auth (
    local_auth_id bigint NOT NULL,
    member_key character varying(255) NOT NULL,
    password character varying(255) NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);

CREATE SEQUENCE public.local_auth_local_auth_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.local_auth_local_auth_id_seq OWNED BY public.local_auth.local_auth_id;

CREATE TABLE public.location_settings (
    location_setting_id bigint NOT NULL,
    ward_member_key character varying(255) NOT NULL,
    collection_interval_seconds integer NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    caregiver_member_key character varying(255) NOT NULL
);

CREATE SEQUENCE public.location_settings_location_setting_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.location_settings_location_setting_id_seq OWNED BY public.location_settings.location_setting_id;

CREATE TABLE public.login_history (
    login_history_id bigint NOT NULL,
    member_key character varying(255) NOT NULL,
    ip_address character varying(255) NOT NULL,
    user_agent character varying(255) NOT NULL,
    is_success boolean NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);

CREATE SEQUENCE public.login_history_login_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.login_history_login_history_id_seq OWNED BY public.login_history.login_history_id;

CREATE TABLE public.member_withdrawal (
    member_withdrawal_id bigint NOT NULL,
    member_key character varying(255) NOT NULL,
    role character varying(20) NOT NULL,
    withdrawn_at timestamp without time zone NOT NULL
);

CREATE SEQUENCE public.member_withdrawal_member_withdrawal_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.member_withdrawal_member_withdrawal_id_seq OWNED BY public.member_withdrawal.member_withdrawal_id;

CREATE TABLE public.members (
    member_id bigint NOT NULL,
    member_key character varying(255) NOT NULL,
    phone character varying(11) NOT NULL,
    name character varying(20) NOT NULL,
    birth date NOT NULL,
    gender character varying(10) NOT NULL,
    role character varying(20) NOT NULL,
    sign_up_type character varying(20) NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    profile_avatar_code character varying(30) NOT NULL
);

CREATE SEQUENCE public.members_member_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.members_member_id_seq OWNED BY public.members.member_id;

CREATE TABLE public.members_terms_agreements (
    terms_agreement_id bigint NOT NULL,
    member_key character varying(255) NOT NULL,
    terms_service_agreed_at timestamp without time zone NOT NULL,
    terms_privacy_agreed_at timestamp without time zone NOT NULL,
    terms_location_agreed_at timestamp without time zone NOT NULL
);

CREATE SEQUENCE public.members_terms_agreements_terms_agreement_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.members_terms_agreements_terms_agreement_id_seq OWNED BY public.members_terms_agreements.terms_agreement_id;

CREATE TABLE public.notification_feedbacks (
    notification_feedback_id bigint NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone,
    notification_id bigint NOT NULL,
    anomaly_detection_id bigint,
    accuracy character varying(20) NOT NULL,
    reason character varying(30),
    comment character varying(100)
);

CREATE SEQUENCE public.notification_feedbacks_notification_feedback_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.notification_feedbacks_notification_feedback_id_seq OWNED BY public.notification_feedbacks.notification_feedback_id;

CREATE TABLE public.notification_settings (
    notification_setting_id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    ward_member_key character varying(255) NOT NULL,
    safe_zone_entry_enabled boolean DEFAULT true NOT NULL,
    safe_zone_exit_enabled boolean DEFAULT true NOT NULL,
    route_deviation_enabled boolean DEFAULT true NOT NULL,
    speed_anomaly_enabled boolean DEFAULT true NOT NULL,
    wandering_anomaly_enabled boolean DEFAULT true NOT NULL,
    emergency_call_enabled boolean DEFAULT true NOT NULL,
    low_battery_enabled boolean DEFAULT true NOT NULL,
    route_deviation_sensitivity character varying(20) DEFAULT 'NORMAL'::character varying NOT NULL,
    speed_anomaly_sensitivity character varying(20) DEFAULT 'NORMAL'::character varying NOT NULL,
    wandering_anomaly_sensitivity character varying(20) DEFAULT 'NORMAL'::character varying NOT NULL,
    battery_threshold_percents character varying(64) DEFAULT ''::character varying NOT NULL,
    abnormal_dwelling_enabled boolean DEFAULT true NOT NULL,
    time_anomaly_enabled boolean DEFAULT true NOT NULL,
    caregiver_member_key character varying(255) NOT NULL,
    danger_zone_entry_enabled boolean DEFAULT true NOT NULL,
    danger_zone_exit_enabled boolean DEFAULT true NOT NULL
);

CREATE SEQUENCE public.notification_settings_notification_setting_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.notification_settings_notification_setting_id_seq OWNED BY public.notification_settings.notification_setting_id;

CREATE TABLE public.notifications (
    notification_id bigint NOT NULL,
    notification_key character varying(255) NOT NULL,
    recipient_member_key character varying(255) NOT NULL,
    event_type character varying(100) NOT NULL,
    title character varying(200) NOT NULL,
    body character varying(1000) NOT NULL,
    data_payload jsonb,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone
);

CREATE SEQUENCE public.notifications_notification_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.notifications_notification_id_seq OWNED BY public.notifications.notification_id;

CREATE TABLE public.oauth (
    oauth_id bigint NOT NULL,
    member_key character varying(255) NOT NULL,
    provider character varying(10) NOT NULL,
    provider_member_id character varying(255) NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);

CREATE SEQUENCE public.oauth_oauth_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.oauth_oauth_id_seq OWNED BY public.oauth.oauth_id;

CREATE TABLE public.refresh_token (
    refresh_token_id bigint NOT NULL,
    member_key character varying(36) NOT NULL,
    token text NOT NULL,
    expired_at timestamp without time zone NOT NULL,
    created_at timestamp without time zone NOT NULL
);

CREATE SEQUENCE public.refresh_token_refresh_token_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.refresh_token_refresh_token_id_seq OWNED BY public.refresh_token.refresh_token_id;

CREATE TABLE public.safe_zone_states (
    safe_zone_state_id bigint NOT NULL,
    ward_member_key character varying(255) NOT NULL,
    safe_zone_keys character varying(2000) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone
)
WITH (fillfactor='85');

CREATE SEQUENCE public.safe_zone_states_safe_zone_state_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.safe_zone_states_safe_zone_state_id_seq OWNED BY public.safe_zone_states.safe_zone_state_id;

CREATE TABLE public.safe_zones (
    safe_zone_id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    safe_zone_key character varying(255) NOT NULL,
    ward_member_key character varying(255) NOT NULL,
    name character varying(50) NOT NULL,
    address character varying(255) NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    type character varying(10) NOT NULL,
    radius_meters integer NOT NULL
);

CREATE SEQUENCE public.safe_zones_safe_zone_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.safe_zones_safe_zone_id_seq OWNED BY public.safe_zones.safe_zone_id;

CREATE TABLE public.site_inquiries (
    site_inquiry_id bigint NOT NULL,
    inquiry_key character varying(255) NOT NULL,
    category character varying(30) NOT NULL,
    title character varying(50) NOT NULL,
    content character varying(1000) NOT NULL,
    email character varying(100) NOT NULL,
    phone character varying(20),
    is_secret boolean NOT NULL,
    password_hash character varying(100),
    status character varying(20) NOT NULL,
    answer character varying(2000),
    answered_at timestamp without time zone,
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);

CREATE SEQUENCE public.site_inquiries_site_inquiry_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.site_inquiries_site_inquiry_id_seq OWNED BY public.site_inquiries.site_inquiry_id;

CREATE TABLE public.ward_device_tokens (
    id bigint NOT NULL,
    ward_key character varying(255) NOT NULL,
    token character varying(255) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    expires_at timestamp without time zone
);

CREATE SEQUENCE public.ward_device_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.ward_device_tokens_id_seq OWNED BY public.ward_device_tokens.id;

ALTER TABLE ONLY public.gps_histories ATTACH PARTITION public.gps_histories_2026_08 FOR VALUES FROM ('2026-08-01 00:00:00') TO ('2026-09-01 00:00:00');

ALTER TABLE ONLY public.gps_histories ATTACH PARTITION public.gps_histories_2026_09 FOR VALUES FROM ('2026-09-01 00:00:00') TO ('2026-10-01 00:00:00');

ALTER TABLE ONLY public.gps_histories ATTACH PARTITION public.gps_histories_2026_10 FOR VALUES FROM ('2026-10-01 00:00:00') TO ('2026-11-01 00:00:00');

ALTER TABLE ONLY public.gps_histories ATTACH PARTITION public.gps_histories_2026_11 FOR VALUES FROM ('2026-11-01 00:00:00') TO ('2026-12-01 00:00:00');

ALTER TABLE ONLY public.gps_histories ATTACH PARTITION public.gps_histories_default DEFAULT;

ALTER TABLE ONLY public.anomaly_detections ALTER COLUMN anomaly_detection_id SET DEFAULT nextval('public.anomaly_detections_anomaly_detection_id_seq'::regclass);

ALTER TABLE ONLY public.care_invitation ALTER COLUMN care_invitation_id SET DEFAULT nextval('public.care_invitation_care_invitation_id_seq'::regclass);

ALTER TABLE ONLY public.care_relationships ALTER COLUMN care_relationship_id SET DEFAULT nextval('public.care_relationships_care_relationship_id_seq'::regclass);

ALTER TABLE ONLY public.designated_avatars ALTER COLUMN designated_avatar_id SET DEFAULT nextval('public.designated_avatars_designated_avatar_id_seq'::regclass);

ALTER TABLE ONLY public.fcm_device_tokens ALTER COLUMN fcm_device_token_id SET DEFAULT nextval('public.fcm_device_tokens_fcm_device_token_id_seq'::regclass);

ALTER TABLE ONLY public.gps_histories ALTER COLUMN gps_history_id SET DEFAULT nextval('public.gps_histories_gps_history_id_seq'::regclass);

ALTER TABLE ONLY public.inquiries ALTER COLUMN inquiry_id SET DEFAULT nextval('public.inquiries_inquiry_id_seq'::regclass);

ALTER TABLE ONLY public.local_auth ALTER COLUMN local_auth_id SET DEFAULT nextval('public.local_auth_local_auth_id_seq'::regclass);

ALTER TABLE ONLY public.location_settings ALTER COLUMN location_setting_id SET DEFAULT nextval('public.location_settings_location_setting_id_seq'::regclass);

ALTER TABLE ONLY public.login_history ALTER COLUMN login_history_id SET DEFAULT nextval('public.login_history_login_history_id_seq'::regclass);

ALTER TABLE ONLY public.member_withdrawal ALTER COLUMN member_withdrawal_id SET DEFAULT nextval('public.member_withdrawal_member_withdrawal_id_seq'::regclass);

ALTER TABLE ONLY public.members ALTER COLUMN member_id SET DEFAULT nextval('public.members_member_id_seq'::regclass);

ALTER TABLE ONLY public.members_terms_agreements ALTER COLUMN terms_agreement_id SET DEFAULT nextval('public.members_terms_agreements_terms_agreement_id_seq'::regclass);

ALTER TABLE ONLY public.notification_feedbacks ALTER COLUMN notification_feedback_id SET DEFAULT nextval('public.notification_feedbacks_notification_feedback_id_seq'::regclass);

ALTER TABLE ONLY public.notification_settings ALTER COLUMN notification_setting_id SET DEFAULT nextval('public.notification_settings_notification_setting_id_seq'::regclass);

ALTER TABLE ONLY public.notifications ALTER COLUMN notification_id SET DEFAULT nextval('public.notifications_notification_id_seq'::regclass);

ALTER TABLE ONLY public.oauth ALTER COLUMN oauth_id SET DEFAULT nextval('public.oauth_oauth_id_seq'::regclass);

ALTER TABLE ONLY public.refresh_token ALTER COLUMN refresh_token_id SET DEFAULT nextval('public.refresh_token_refresh_token_id_seq'::regclass);

ALTER TABLE ONLY public.safe_zone_states ALTER COLUMN safe_zone_state_id SET DEFAULT nextval('public.safe_zone_states_safe_zone_state_id_seq'::regclass);

ALTER TABLE ONLY public.safe_zones ALTER COLUMN safe_zone_id SET DEFAULT nextval('public.safe_zones_safe_zone_id_seq'::regclass);

ALTER TABLE ONLY public.site_inquiries ALTER COLUMN site_inquiry_id SET DEFAULT nextval('public.site_inquiries_site_inquiry_id_seq'::regclass);

ALTER TABLE ONLY public.ward_device_tokens ALTER COLUMN id SET DEFAULT nextval('public.ward_device_tokens_id_seq'::regclass);

ALTER TABLE ONLY public.anomaly_detections
    ADD CONSTRAINT anomaly_detections_pkey PRIMARY KEY (anomaly_detection_id);

ALTER TABLE ONLY public.care_invitation
    ADD CONSTRAINT care_invitation_pkey PRIMARY KEY (care_invitation_id);

ALTER TABLE ONLY public.care_invitation
    ADD CONSTRAINT care_invitation_request_key_key UNIQUE (request_key);

ALTER TABLE ONLY public.care_relationships
    ADD CONSTRAINT care_relationships_pkey PRIMARY KEY (care_relationship_id);

ALTER TABLE ONLY public.care_relationships
    ADD CONSTRAINT care_relationships_ward_member_key_caregiver_member_key_key UNIQUE (ward_member_key, caregiver_member_key);

ALTER TABLE ONLY public.designated_avatars
    ADD CONSTRAINT designated_avatars_owner_member_key_ward_member_key_target__key UNIQUE (owner_member_key, ward_member_key, target_member_key);

ALTER TABLE ONLY public.designated_avatars
    ADD CONSTRAINT designated_avatars_pkey PRIMARY KEY (designated_avatar_id);

ALTER TABLE ONLY public.fcm_device_tokens
    ADD CONSTRAINT fcm_device_tokens_pkey PRIMARY KEY (fcm_device_token_id);

ALTER TABLE ONLY public.fcm_device_tokens
    ADD CONSTRAINT fcm_device_tokens_token_key UNIQUE (token);

ALTER TABLE ONLY public.gps_histories
    ADD CONSTRAINT gps_histories_new_pkey PRIMARY KEY (gps_history_id, recorded_at);

ALTER TABLE ONLY public.gps_histories_2026_08
    ADD CONSTRAINT gps_histories_2026_08_pkey PRIMARY KEY (gps_history_id, recorded_at);

ALTER TABLE ONLY public.gps_histories_2026_09
    ADD CONSTRAINT gps_histories_2026_09_pkey PRIMARY KEY (gps_history_id, recorded_at);

ALTER TABLE ONLY public.gps_histories_2026_10
    ADD CONSTRAINT gps_histories_2026_10_pkey PRIMARY KEY (gps_history_id, recorded_at);

ALTER TABLE ONLY public.gps_histories_2026_11
    ADD CONSTRAINT gps_histories_2026_11_pkey PRIMARY KEY (gps_history_id, recorded_at);

ALTER TABLE ONLY public.gps_histories_default
    ADD CONSTRAINT gps_histories_default_pkey PRIMARY KEY (gps_history_id, recorded_at);

ALTER TABLE ONLY public.inquiries
    ADD CONSTRAINT inquiries_inquiry_key_key UNIQUE (inquiry_key);

ALTER TABLE ONLY public.inquiries
    ADD CONSTRAINT inquiries_pkey PRIMARY KEY (inquiry_id);

ALTER TABLE ONLY public.local_auth
    ADD CONSTRAINT local_auth_member_key_key UNIQUE (member_key);

ALTER TABLE ONLY public.local_auth
    ADD CONSTRAINT local_auth_pkey PRIMARY KEY (local_auth_id);

ALTER TABLE ONLY public.location_settings
    ADD CONSTRAINT location_settings_pkey PRIMARY KEY (location_setting_id);

ALTER TABLE ONLY public.login_history
    ADD CONSTRAINT login_history_pkey PRIMARY KEY (login_history_id);

ALTER TABLE ONLY public.member_withdrawal
    ADD CONSTRAINT member_withdrawal_pkey PRIMARY KEY (member_withdrawal_id);

ALTER TABLE ONLY public.members
    ADD CONSTRAINT members_member_key_key UNIQUE (member_key);

ALTER TABLE ONLY public.members
    ADD CONSTRAINT members_phone_key UNIQUE (phone);

ALTER TABLE ONLY public.members
    ADD CONSTRAINT members_pkey PRIMARY KEY (member_id);

ALTER TABLE ONLY public.members_terms_agreements
    ADD CONSTRAINT members_terms_agreements_member_key_key UNIQUE (member_key);

ALTER TABLE ONLY public.members_terms_agreements
    ADD CONSTRAINT members_terms_agreements_pkey PRIMARY KEY (terms_agreement_id);

ALTER TABLE ONLY public.notification_feedbacks
    ADD CONSTRAINT notification_feedbacks_pkey PRIMARY KEY (notification_feedback_id);

ALTER TABLE ONLY public.notification_settings
    ADD CONSTRAINT notification_settings_pkey PRIMARY KEY (notification_setting_id);

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_notification_key_key UNIQUE (notification_key);

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (notification_id);

ALTER TABLE ONLY public.oauth
    ADD CONSTRAINT oauth_pkey PRIMARY KEY (oauth_id);

ALTER TABLE ONLY public.oauth
    ADD CONSTRAINT oauth_provider_provider_member_id_key UNIQUE (provider, provider_member_id);

ALTER TABLE ONLY public.refresh_token
    ADD CONSTRAINT refresh_token_pkey PRIMARY KEY (refresh_token_id);

ALTER TABLE ONLY public.refresh_token
    ADD CONSTRAINT refresh_token_token_key UNIQUE (token);

ALTER TABLE ONLY public.safe_zone_states
    ADD CONSTRAINT safe_zone_states_pkey PRIMARY KEY (safe_zone_state_id);

ALTER TABLE ONLY public.safe_zone_states
    ADD CONSTRAINT safe_zone_states_ward_member_key_key UNIQUE (ward_member_key);

ALTER TABLE ONLY public.safe_zones
    ADD CONSTRAINT safe_zones_pkey PRIMARY KEY (safe_zone_id);

ALTER TABLE ONLY public.safe_zones
    ADD CONSTRAINT safe_zones_safe_zone_key_key UNIQUE (safe_zone_key);

ALTER TABLE ONLY public.site_inquiries
    ADD CONSTRAINT site_inquiries_inquiry_key_key UNIQUE (inquiry_key);

ALTER TABLE ONLY public.site_inquiries
    ADD CONSTRAINT site_inquiries_pkey PRIMARY KEY (site_inquiry_id);

ALTER TABLE ONLY public.anomaly_detections
    ADD CONSTRAINT uk_anomaly_detections_ward_type_recorded_kind UNIQUE NULLS NOT DISTINCT (ward_member_key, detection_type, recorded_at, alert_kind);

ALTER TABLE ONLY public.location_settings
    ADD CONSTRAINT uk_location_settings_ward_caregiver UNIQUE (ward_member_key, caregiver_member_key);

ALTER TABLE ONLY public.notification_feedbacks
    ADD CONSTRAINT uk_notification_feedbacks_notification UNIQUE (notification_id);

ALTER TABLE ONLY public.notification_settings
    ADD CONSTRAINT uk_notification_settings_ward_caregiver UNIQUE (ward_member_key, caregiver_member_key);

ALTER TABLE ONLY public.ward_device_tokens
    ADD CONSTRAINT ward_device_tokens_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.ward_device_tokens
    ADD CONSTRAINT ward_device_tokens_token_key UNIQUE (token);

ALTER TABLE ONLY public.ward_device_tokens
    ADD CONSTRAINT ward_device_tokens_ward_key_key UNIQUE (ward_key);

CREATE INDEX idx_gps_ward_recorded ON ONLY public.gps_histories USING btree (ward_member_key, recorded_at);

CREATE INDEX gps_histories_2026_08_ward_member_key_recorded_at_idx ON public.gps_histories_2026_08 USING btree (ward_member_key, recorded_at);

CREATE INDEX gps_histories_2026_09_ward_member_key_recorded_at_idx ON public.gps_histories_2026_09 USING btree (ward_member_key, recorded_at);

CREATE INDEX gps_histories_2026_10_ward_member_key_recorded_at_idx ON public.gps_histories_2026_10 USING btree (ward_member_key, recorded_at);

CREATE INDEX gps_histories_2026_11_ward_member_key_recorded_at_idx ON public.gps_histories_2026_11 USING btree (ward_member_key, recorded_at);

CREATE INDEX gps_histories_default_ward_member_key_recorded_at_idx ON public.gps_histories_default USING btree (ward_member_key, recorded_at);

CREATE INDEX idx_member_withdrawal_member_key ON public.member_withdrawal USING btree (member_key);

ALTER INDEX public.gps_histories_new_pkey ATTACH PARTITION public.gps_histories_2026_08_pkey;

ALTER INDEX public.idx_gps_ward_recorded ATTACH PARTITION public.gps_histories_2026_08_ward_member_key_recorded_at_idx;

ALTER INDEX public.gps_histories_new_pkey ATTACH PARTITION public.gps_histories_2026_09_pkey;

ALTER INDEX public.idx_gps_ward_recorded ATTACH PARTITION public.gps_histories_2026_09_ward_member_key_recorded_at_idx;

ALTER INDEX public.gps_histories_new_pkey ATTACH PARTITION public.gps_histories_2026_10_pkey;

ALTER INDEX public.idx_gps_ward_recorded ATTACH PARTITION public.gps_histories_2026_10_ward_member_key_recorded_at_idx;

ALTER INDEX public.gps_histories_new_pkey ATTACH PARTITION public.gps_histories_2026_11_pkey;

ALTER INDEX public.idx_gps_ward_recorded ATTACH PARTITION public.gps_histories_2026_11_ward_member_key_recorded_at_idx;

ALTER INDEX public.gps_histories_new_pkey ATTACH PARTITION public.gps_histories_default_pkey;

ALTER INDEX public.idx_gps_ward_recorded ATTACH PARTITION public.gps_histories_default_ward_member_key_recorded_at_idx;
