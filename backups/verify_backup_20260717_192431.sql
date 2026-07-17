--
-- PostgreSQL database dump
--

\restrict 8guGrMddSyS8ZfpbdNLdjh88Vw9D0YiwJV5FWmLPTg7mBerWs8FpRCg6iatAw3d

-- Dumped from database version 16.14 (Debian 16.14-1.pgdg12+1)
-- Dumped by pg_dump version 16.14 (Debian 16.14-1.pgdg12+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

ALTER TABLE IF EXISTS ONLY public.trace_links DROP CONSTRAINT IF EXISTS trace_links_requirement_id_fkey;
ALTER TABLE IF EXISTS ONLY public.review_votes DROP CONSTRAINT IF EXISTS review_votes_flow_id_fkey;
ALTER TABLE IF EXISTS ONLY public.review_flows DROP CONSTRAINT IF EXISTS review_flows_requirement_id_fkey;
ALTER TABLE IF EXISTS ONLY public.requirement_versions DROP CONSTRAINT IF EXISTS requirement_versions_requirement_id_fkey;
ALTER TABLE IF EXISTS ONLY public.requirement_stakeholders DROP CONSTRAINT IF EXISTS requirement_stakeholders_requirement_id_fkey;
ALTER TABLE IF EXISTS ONLY public.requirement_groups DROP CONSTRAINT IF EXISTS requirement_groups_requirement_id_fkey;
ALTER TABLE IF EXISTS ONLY public.message_archives DROP CONSTRAINT IF EXISTS message_archives_requirement_id_fkey;
ALTER TABLE IF EXISTS ONLY public.change_requests DROP CONSTRAINT IF EXISTS change_requests_requirement_id_fkey;
ALTER TABLE IF EXISTS ONLY public.baselines DROP CONSTRAINT IF EXISTS baselines_requirement_id_fkey;
ALTER TABLE IF EXISTS ONLY public.acceptance_criteria DROP CONSTRAINT IF EXISTS acceptance_criteria_requirement_id_fkey;
DROP INDEX IF EXISTS public.idx_ver_req;
DROP INDEX IF EXISTS public.idx_ver_hash;
DROP INDEX IF EXISTS public.idx_users_feishu;
DROP INDEX IF EXISTS public.idx_trace_req;
DROP INDEX IF EXISTS public.idx_stake_user;
DROP INDEX IF EXISTS public.idx_stake_req;
DROP INDEX IF EXISTS public.idx_req_status;
DROP INDEX IF EXISTS public.idx_req_no;
DROP INDEX IF EXISTS public.idx_msg_req;
DROP INDEX IF EXISTS public.idx_msg_idem;
DROP INDEX IF EXISTS public.idx_audit_target;
ALTER TABLE IF EXISTS ONLY public.users DROP CONSTRAINT IF EXISTS users_pkey;
ALTER TABLE IF EXISTS ONLY public.trace_links DROP CONSTRAINT IF EXISTS trace_links_pkey;
ALTER TABLE IF EXISTS ONLY public.templates DROP CONSTRAINT IF EXISTS templates_pkey;
ALTER TABLE IF EXISTS ONLY public.review_votes DROP CONSTRAINT IF EXISTS review_votes_pkey;
ALTER TABLE IF EXISTS ONLY public.review_flows DROP CONSTRAINT IF EXISTS review_flows_pkey;
ALTER TABLE IF EXISTS ONLY public.requirements DROP CONSTRAINT IF EXISTS requirements_req_no_key;
ALTER TABLE IF EXISTS ONLY public.requirements DROP CONSTRAINT IF EXISTS requirements_pkey;
ALTER TABLE IF EXISTS ONLY public.requirement_versions DROP CONSTRAINT IF EXISTS requirement_versions_pkey;
ALTER TABLE IF EXISTS ONLY public.requirement_stakeholders DROP CONSTRAINT IF EXISTS requirement_stakeholders_requirement_id_user_id_stakeholder_key;
ALTER TABLE IF EXISTS ONLY public.requirement_stakeholders DROP CONSTRAINT IF EXISTS requirement_stakeholders_pkey;
ALTER TABLE IF EXISTS ONLY public.requirement_groups DROP CONSTRAINT IF EXISTS requirement_groups_requirement_id_key;
ALTER TABLE IF EXISTS ONLY public.requirement_groups DROP CONSTRAINT IF EXISTS requirement_groups_pkey;
ALTER TABLE IF EXISTS ONLY public.org_units DROP CONSTRAINT IF EXISTS org_units_pkey;
ALTER TABLE IF EXISTS ONLY public.notifications DROP CONSTRAINT IF EXISTS notifications_pkey;
ALTER TABLE IF EXISTS ONLY public.message_archives DROP CONSTRAINT IF EXISTS message_archives_pkey;
ALTER TABLE IF EXISTS ONLY public.change_requests DROP CONSTRAINT IF EXISTS change_requests_pkey;
ALTER TABLE IF EXISTS ONLY public.baselines DROP CONSTRAINT IF EXISTS baselines_pkey;
ALTER TABLE IF EXISTS ONLY public.audit_logs DROP CONSTRAINT IF EXISTS audit_logs_pkey;
ALTER TABLE IF EXISTS ONLY public.acceptance_criteria DROP CONSTRAINT IF EXISTS acceptance_criteria_pkey;
ALTER TABLE IF EXISTS public.message_archives ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.audit_logs ALTER COLUMN id DROP DEFAULT;
DROP TABLE IF EXISTS public.users;
DROP TABLE IF EXISTS public.trace_links;
DROP TABLE IF EXISTS public.templates;
DROP TABLE IF EXISTS public.review_votes;
DROP TABLE IF EXISTS public.review_flows;
DROP TABLE IF EXISTS public.requirements;
DROP TABLE IF EXISTS public.requirement_versions;
DROP TABLE IF EXISTS public.requirement_stakeholders;
DROP TABLE IF EXISTS public.requirement_groups;
DROP TABLE IF EXISTS public.org_units;
DROP TABLE IF EXISTS public.notifications;
DROP SEQUENCE IF EXISTS public.message_archives_id_seq;
DROP TABLE IF EXISTS public.message_archives;
DROP TABLE IF EXISTS public.change_requests;
DROP TABLE IF EXISTS public.baselines;
DROP SEQUENCE IF EXISTS public.audit_logs_id_seq;
DROP TABLE IF EXISTS public.audit_logs;
DROP TABLE IF EXISTS public.acceptance_criteria;
-- *not* dropping schema, since initdb creates it
--
-- Name: public; Type: SCHEMA; Schema: -; Owner: qm
--

-- *not* creating schema, since initdb creates it


ALTER SCHEMA public OWNER TO qm;

--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: qm
--

COMMENT ON SCHEMA public IS '';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: acceptance_criteria; Type: TABLE; Schema: public; Owner: qm
--

CREATE TABLE public.acceptance_criteria (
    id character varying(64) NOT NULL,
    requirement_id character varying(64) NOT NULL,
    version_id character varying(64) NOT NULL,
    criterion_type character varying(32) DEFAULT 'checklist'::character varying NOT NULL,
    content text NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    deleted_at timestamp without time zone
);


ALTER TABLE public.acceptance_criteria OWNER TO qm;

--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: qm
--

CREATE TABLE public.audit_logs (
    id bigint NOT NULL,
    actor_id character varying(64),
    action character varying(128) NOT NULL,
    target_type character varying(64),
    target_id character varying(64),
    detail text,
    ip character varying(64),
    user_agent character varying(512),
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.audit_logs OWNER TO qm;

--
-- Name: TABLE audit_logs; Type: COMMENT; Schema: public; Owner: qm
--

COMMENT ON TABLE public.audit_logs IS '审计日志（只增不改）';


--
-- Name: audit_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: qm
--

CREATE SEQUENCE public.audit_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.audit_logs_id_seq OWNER TO qm;

--
-- Name: audit_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: qm
--

ALTER SEQUENCE public.audit_logs_id_seq OWNED BY public.audit_logs.id;


--
-- Name: baselines; Type: TABLE; Schema: public; Owner: qm
--

CREATE TABLE public.baselines (
    id character varying(64) NOT NULL,
    requirement_id character varying(64) NOT NULL,
    version_id character varying(64) NOT NULL,
    content_hash character varying(64) NOT NULL,
    snapshot text,
    signed_by character varying(64) NOT NULL,
    signed_at timestamp without time zone NOT NULL,
    signature_meta text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    deleted_at timestamp without time zone
);


ALTER TABLE public.baselines OWNER TO qm;

--
-- Name: TABLE baselines; Type: COMMENT; Schema: public; Owner: qm
--

COMMENT ON TABLE public.baselines IS '基线签认快照';


--
-- Name: change_requests; Type: TABLE; Schema: public; Owner: qm
--

CREATE TABLE public.change_requests (
    id character varying(64) NOT NULL,
    requirement_id character varying(64) NOT NULL,
    from_version_id character varying(64),
    to_version_id character varying(64),
    diff text,
    reason text,
    impact_assessment text,
    level character varying(16) DEFAULT 'minor'::character varying NOT NULL,
    status character varying(32) DEFAULT 'pending'::character varying NOT NULL,
    approved_by character varying(64),
    approved_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    deleted_at timestamp without time zone
);


ALTER TABLE public.change_requests OWNER TO qm;

--
-- Name: message_archives; Type: TABLE; Schema: public; Owner: qm
--

CREATE TABLE public.message_archives (
    id bigint NOT NULL,
    requirement_id character varying(64) NOT NULL,
    im_msg_id character varying(128) NOT NULL,
    im_provider character varying(16) DEFAULT 'feishu'::character varying NOT NULL,
    sender_id character varying(64),
    msg_type character varying(32) DEFAULT 'text'::character varying NOT NULL,
    content text,
    content_text text,
    is_key_info boolean DEFAULT false NOT NULL,
    key_info_merged boolean DEFAULT false NOT NULL,
    msg_time timestamp without time zone NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.message_archives OWNER TO qm;

--
-- Name: TABLE message_archives; Type: COMMENT; Schema: public; Owner: qm
--

COMMENT ON TABLE public.message_archives IS '群消息归档（书记员引擎写入）';


--
-- Name: message_archives_id_seq; Type: SEQUENCE; Schema: public; Owner: qm
--

CREATE SEQUENCE public.message_archives_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.message_archives_id_seq OWNER TO qm;

--
-- Name: message_archives_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: qm
--

ALTER SEQUENCE public.message_archives_id_seq OWNED BY public.message_archives.id;


--
-- Name: notifications; Type: TABLE; Schema: public; Owner: qm
--

CREATE TABLE public.notifications (
    id character varying(64) NOT NULL,
    user_id character varying(64) NOT NULL,
    type character varying(64) NOT NULL,
    requirement_id character varying(64),
    payload text,
    channel character varying(32) DEFAULT 'im_msg'::character varying NOT NULL,
    status character varying(16) DEFAULT 'pending'::character varying NOT NULL,
    sent_at timestamp without time zone,
    read_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    deleted_at timestamp without time zone
);


ALTER TABLE public.notifications OWNER TO qm;

--
-- Name: org_units; Type: TABLE; Schema: public; Owner: qm
--

CREATE TABLE public.org_units (
    id character varying(64) NOT NULL,
    name character varying(256) NOT NULL,
    parent_id character varying(64),
    im_dept_id character varying(128),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    deleted_at timestamp without time zone
);


ALTER TABLE public.org_units OWNER TO qm;

--
-- Name: requirement_groups; Type: TABLE; Schema: public; Owner: qm
--

CREATE TABLE public.requirement_groups (
    id character varying(64) NOT NULL,
    requirement_id character varying(64) NOT NULL,
    im_provider character varying(16) DEFAULT 'feishu'::character varying NOT NULL,
    chat_id character varying(128) NOT NULL,
    group_name character varying(256),
    status character varying(32) DEFAULT 'active'::character varying NOT NULL,
    dissolved_at timestamp without time zone,
    archive_exported boolean DEFAULT false NOT NULL,
    archive_path character varying(512),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    deleted_at timestamp without time zone
);


ALTER TABLE public.requirement_groups OWNER TO qm;

--
-- Name: TABLE requirement_groups; Type: COMMENT; Schema: public; Owner: qm
--

COMMENT ON TABLE public.requirement_groups IS '需求与 IM 群的绑定关系';


--
-- Name: requirement_stakeholders; Type: TABLE; Schema: public; Owner: qm
--

CREATE TABLE public.requirement_stakeholders (
    id character varying(64) NOT NULL,
    requirement_id character varying(64) NOT NULL,
    user_id character varying(64) NOT NULL,
    stakeholder_role character varying(32) NOT NULL,
    added_by character varying(64),
    added_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    deleted_at timestamp without time zone
);


ALTER TABLE public.requirement_stakeholders OWNER TO qm;

--
-- Name: requirement_versions; Type: TABLE; Schema: public; Owner: qm
--

CREATE TABLE public.requirement_versions (
    id character varying(64) NOT NULL,
    requirement_id character varying(64) NOT NULL,
    version_no integer NOT NULL,
    content text,
    content_text text,
    content_hash character varying(64),
    fields_data text,
    edited_by character varying(64),
    change_summary character varying(512),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    deleted_at timestamp without time zone
);


ALTER TABLE public.requirement_versions OWNER TO qm;

--
-- Name: TABLE requirement_versions; Type: COMMENT; Schema: public; Owner: qm
--

COMMENT ON TABLE public.requirement_versions IS '需求版本（含内容指纹）';


--
-- Name: requirements; Type: TABLE; Schema: public; Owner: qm
--

CREATE TABLE public.requirements (
    id character varying(64) NOT NULL,
    req_no character varying(32) NOT NULL,
    title character varying(512) NOT NULL,
    req_type character varying(32) NOT NULL,
    product_line character varying(128),
    module character varying(128),
    priority character varying(8) DEFAULT 'P2'::character varying NOT NULL,
    status character varying(32) DEFAULT 'draft'::character varying NOT NULL,
    current_version_id character varying(64),
    owner_id character varying(64),
    is_confidential boolean DEFAULT false NOT NULL,
    source_channel character varying(32) DEFAULT 'web'::character varying NOT NULL,
    created_by character varying(64),
    closed_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    deleted_at timestamp without time zone
);


ALTER TABLE public.requirements OWNER TO qm;

--
-- Name: TABLE requirements; Type: COMMENT; Schema: public; Owner: qm
--

COMMENT ON TABLE public.requirements IS '需求主表';


--
-- Name: review_flows; Type: TABLE; Schema: public; Owner: qm
--

CREATE TABLE public.review_flows (
    id character varying(64) NOT NULL,
    requirement_id character varying(64) NOT NULL,
    round_no integer DEFAULT 1 NOT NULL,
    review_type character varying(32) DEFAULT 'final'::character varying NOT NULL,
    mode character varying(16) DEFAULT 'all'::character varying NOT NULL,
    status character varying(32) DEFAULT 'in_progress'::character varying NOT NULL,
    started_at timestamp without time zone,
    finished_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    deleted_at timestamp without time zone
);


ALTER TABLE public.review_flows OWNER TO qm;

--
-- Name: review_votes; Type: TABLE; Schema: public; Owner: qm
--

CREATE TABLE public.review_votes (
    id character varying(64) NOT NULL,
    flow_id character varying(64) NOT NULL,
    voter_id character varying(64) NOT NULL,
    decision character varying(16) DEFAULT 'pending'::character varying NOT NULL,
    comment text,
    voted_at timestamp without time zone,
    delegated_to character varying(64),
    card_msg_id character varying(128),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    deleted_at timestamp without time zone
);


ALTER TABLE public.review_votes OWNER TO qm;

--
-- Name: templates; Type: TABLE; Schema: public; Owner: qm
--

CREATE TABLE public.templates (
    id character varying(64) NOT NULL,
    name character varying(256) NOT NULL,
    req_type character varying(32) NOT NULL,
    field_schema text,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    deleted_at timestamp without time zone
);


ALTER TABLE public.templates OWNER TO qm;

--
-- Name: trace_links; Type: TABLE; Schema: public; Owner: qm
--

CREATE TABLE public.trace_links (
    id character varying(64) NOT NULL,
    requirement_id character varying(64) NOT NULL,
    link_type character varying(32) NOT NULL,
    external_id character varying(256),
    external_url character varying(1024),
    title character varying(512),
    source character varying(16) DEFAULT 'manual'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    deleted_at timestamp without time zone
);


ALTER TABLE public.trace_links OWNER TO qm;

--
-- Name: users; Type: TABLE; Schema: public; Owner: qm
--

CREATE TABLE public.users (
    id character varying(64) NOT NULL,
    name character varying(128) NOT NULL,
    email character varying(256),
    feishu_open_id character varying(128),
    wecom_user_id character varying(128),
    role character varying(32) DEFAULT 'REQUESTER'::character varying NOT NULL,
    status character varying(16) DEFAULT 'active'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    deleted_at timestamp without time zone
);


ALTER TABLE public.users OWNER TO qm;

--
-- Name: audit_logs id; Type: DEFAULT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.audit_logs ALTER COLUMN id SET DEFAULT nextval('public.audit_logs_id_seq'::regclass);


--
-- Name: message_archives id; Type: DEFAULT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.message_archives ALTER COLUMN id SET DEFAULT nextval('public.message_archives_id_seq'::regclass);


--
-- Data for Name: acceptance_criteria; Type: TABLE DATA; Schema: public; Owner: qm
--

COPY public.acceptance_criteria (id, requirement_id, version_id, criterion_type, content, sort_order, created_at, updated_at, deleted_at) FROM stdin;
\.


--
-- Data for Name: audit_logs; Type: TABLE DATA; Schema: public; Owner: qm
--

COPY public.audit_logs (id, actor_id, action, target_type, target_id, detail, ip, user_agent, created_at) FROM stdin;
\.


--
-- Data for Name: baselines; Type: TABLE DATA; Schema: public; Owner: qm
--

COPY public.baselines (id, requirement_id, version_id, content_hash, snapshot, signed_by, signed_at, signature_meta, created_at, updated_at, deleted_at) FROM stdin;
d8260ac70ea8431b44a93b40a6068692	c04bfc81b304b7250075887fe1f088df	24926bce1fd64b5a2607cb1c92655332	6a19b5efac8fafa26ca813ed9850e1ba9226f197f3d880822a6eedaaada2a60a	\N	test-user-001	2026-07-17 15:33:19.743134	{"ip":"127.0.0.1","userAgent":"Python-urllib/3.11"}	2026-07-17 15:33:19.744472	2026-07-17 15:33:19.744528	\N
4c44c27c967ef9a92f52fba81cd25c25	c5df834ed66e401415b5dd6673daec08	3273a2309771a657d1e080f8c83a3943	b82ea9963f96c0a504bcc8ae3988c39f37281f4fb9590542778259497ed19ea2	# 综合验证\n## 背景\n...	test-user-001	2026-07-17 15:40:21.265488	{"ip":"127.0.0.1","userAgent":"Python-urllib/3.12"}	2026-07-17 15:40:21.266576	2026-07-17 15:40:21.266628	\N
6c3069957db7d89c75104e0d1b6924b5	98863c9d552ff3cb8b9adaef999aaa6c	5a463f9ab488d2c65ddffba02ef7755e	fa2bed470612be0183f50b2268db8a45c00982416174e03cf7f983086d38068d	# P0验收\n## 背景\n...	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	2026-07-17 16:07:18.505772	{"ip":"127.0.0.1","userAgent":"Python-urllib/3.11"}	2026-07-17 16:07:18.507714	2026-07-17 16:07:18.50778	\N
ebea6f86023a2aa5c7320456e08e78db	9fb87b94485b0046e01b88df6d36267d	91306bce71b18658c9a7114fa530cc1c	a2eb952f89ff0677be26c36dd83073343f6b8a2596d6dc9e5bff0674d24d4131	\N	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	2026-07-17 17:35:20.674514	{"ip":"card-callback","userAgent":"feishu-card"}	2026-07-17 17:35:20.675783	2026-07-17 17:35:20.675869	\N
\.


--
-- Data for Name: change_requests; Type: TABLE DATA; Schema: public; Owner: qm
--

COPY public.change_requests (id, requirement_id, from_version_id, to_version_id, diff, reason, impact_assessment, level, status, approved_by, approved_at, created_at, updated_at, deleted_at) FROM stdin;
\.


--
-- Data for Name: message_archives; Type: TABLE DATA; Schema: public; Owner: qm
--

COPY public.message_archives (id, requirement_id, im_msg_id, im_provider, sender_id, msg_type, content, content_text, is_key_info, key_info_merged, msg_time, created_at) FROM stdin;
1	ad78f52bc1b94cd2e98de45bc58c22e6	test-msg-004	feishu	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	text	这是一条测试消息	这是一条测试消息	f	f	2026-07-17 15:40:00	2026-07-17 15:48:11.488663
2	98863c9d552ff3cb8b9adaef999aaa6c	p0-test-1784275640	feishu	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	text	P0验收测试消息	P0验收测试消息	f	f	2026-07-17 15:50:00	2026-07-17 16:07:20.350836
\.


--
-- Data for Name: notifications; Type: TABLE DATA; Schema: public; Owner: qm
--

COPY public.notifications (id, user_id, type, requirement_id, payload, channel, status, sent_at, read_at, created_at, updated_at, deleted_at) FROM stdin;
a4d1730ea5e843e8b3c907fa2a83ac69	test-user-001	system	\N	{"title":"测试通知","content":"这是一条测试"}	web	read	\N	2026-07-17 15:52:21.700486	2026-07-17 15:52:21.588212	2026-07-17 15:52:21.589769	\N
9da8ebb5a91ff9d6bc7f5413cddefca9	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	baseline_sign	\N	{"title":"基线签认完成","content":"需求REQ-2026-0010已完成基线签认"}	web	pending	\N	\N	2026-07-17 16:07:22.344038	2026-07-17 16:07:22.344081	\N
a528a4ffa78f956c5510d3d9326fb87a	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	review_vote	\N	{"title":"收到评审投票","content":"ou_b8e60596a12d61e0a23a8e9c4a9b1234 approve了评审 REQ-2026-0017"}	im_card	pending	\N	\N	2026-07-17 17:35:19.974808	2026-07-17 17:35:19.974875	\N
6be9b9d84d36d5d0200365d18be99214	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	review_vote	\N	{"title":"收到评审投票","content":"ou_c9f71607b23e72f1b34b9fad5b0c2345 approve了评审 REQ-2026-0017"}	im_card	pending	\N	\N	2026-07-17 17:35:20.64405	2026-07-17 17:35:20.644115	\N
\.


--
-- Data for Name: org_units; Type: TABLE DATA; Schema: public; Owner: qm
--

COPY public.org_units (id, name, parent_id, im_dept_id, created_at, updated_at, deleted_at) FROM stdin;
\.


--
-- Data for Name: requirement_groups; Type: TABLE DATA; Schema: public; Owner: qm
--

COPY public.requirement_groups (id, requirement_id, im_provider, chat_id, group_name, status, dissolved_at, archive_exported, archive_path, created_at, updated_at, deleted_at) FROM stdin;
4e89c840f59bf7a7849b3497b42280ae	f345c824e29906516fbe0189465ffce1	feishu	oc_3cc229e53b005e2f90f4b1dc3135e646	[REQ-2026-0004] 群引擎测试需求	dissolved	2026-07-17 15:38:47.532185	f	\N	2026-07-17 15:38:47.197524	2026-07-17 15:38:47.197628	\N
1022d239c1e05b6a384270b57f9f52b3	3a4b1a9a951330795c86931c903bf2ee	feishu	oc_08ab418d34c2f407a6e3f143d76121af	[REQ-2026-0006] 群引擎验证需求	dissolved	2026-07-17 15:40:22.778613	f	\N	2026-07-17 15:40:22.452894	2026-07-17 15:40:22.452956	\N
be9cec38e122f9d3cd2182c11ccfb4dc	7302ee6eba95301ad6763b3699d1beeb	feishu	oc_a603c1c7736b67cb1caa90c914bb9c18	[REQ-2026-0007] 书记员验证需求	dissolved	2026-07-17 15:43:37.777064	f	\N	2026-07-17 15:43:35.390596	2026-07-17 15:43:35.39069	\N
0a198fa7e7612ef6058ec64ee2154b66	4349e71ea8f05b93454b5c4a4e942e4e	feishu	oc_f2a187664dda9b154eeba41e4fccd735	[REQ-2026-0008] 书记员验证需求	dissolved	2026-07-17 15:45:42.087425	f	\N	2026-07-17 15:45:38.705234	2026-07-17 15:45:38.705402	\N
a3122a2eaa9de573aa4ed1a348832ba8	ad78f52bc1b94cd2e98de45bc58c22e6	feishu	oc_5a6f9f8d758085495c99f87295dac116	[REQ-2026-0009] 书记员验证需求	dissolved	2026-07-17 15:48:14.861097	f	\N	2026-07-17 15:48:11.425382	2026-07-17 15:48:11.425527	\N
96a2d78bd0dfca5821ea093669cff5e2	98863c9d552ff3cb8b9adaef999aaa6c	feishu	oc_8d45fcc815cf19d2f46756202145678e	[REQ-2026-0010] P0联调验收需求	dissolved	2026-07-17 16:07:22.63131	f	\N	2026-07-17 16:07:20.290069	2026-07-17 16:07:20.290137	\N
3f297a5ea06e6dce229faa1a56217f1b	bc9516b876776fef675ed22cad25b568	feishu	oc_22424fc1448ea87d5faeda0df27f7f83	[REQ-2026-0012] A1拉人入群验证	dissolved	2026-07-17 17:25:57.650167	f	\N	2026-07-17 17:25:56.50778	2026-07-17 17:25:56.507874	\N
471059ee481c897908dbacb2e8690c33	2f643c53ed2fb49f0ba83681596285da	feishu	oc_5e3d4778823873d5d8c09f55edd7e02e	[REQ-2026-0014] 卡片回调验证	dissolved	2026-07-17 17:30:44.12752	f	\N	2026-07-17 17:30:41.848938	2026-07-17 17:30:41.849084	\N
19baaea92e12c40acb1840f4504cfa07	d69a718e61312614f02be6039f8a28ee	feishu	oc_419e8be4c14fd9c2bf3131ecdf6a0e12	[REQ-2026-0015] 卡片回调验证v2	dissolved	2026-07-17 17:32:00.886875	f	\N	2026-07-17 17:31:58.581213	2026-07-17 17:31:58.581341	\N
9ccce150af408eb17524072a31624aec	3b1ea8c7428c4366ac79e327ad3cae92	feishu	oc_b54796a14e9aea5f45e62f7f2f500051	[REQ-2026-0016] 卡片回调v3	dissolved	2026-07-17 17:33:38.424795	f	\N	2026-07-17 17:33:36.097232	2026-07-17 17:33:36.097389	\N
6b1f1c58d22ca536997f28f78bae776d	9fb87b94485b0046e01b88df6d36267d	feishu	oc_d71c5f13a1d183c349dfd5a8b2150a3e	[REQ-2026-0017] 卡片回调v4	dissolved	2026-07-17 17:35:20.988325	f	\N	2026-07-17 17:35:18.077829	2026-07-17 17:35:18.077931	\N
\.


--
-- Data for Name: requirement_stakeholders; Type: TABLE DATA; Schema: public; Owner: qm
--

COPY public.requirement_stakeholders (id, requirement_id, user_id, stakeholder_role, added_by, added_at, created_at, updated_at, deleted_at) FROM stdin;
81f0b1bb744c5e276dcbf525815c392e	a65b064cd7f67c8907db041e4bb50cc6	test-user-001	requester	test-user-001	\N	2026-07-17 15:21:15.850434	2026-07-17 15:21:15.850498	\N
7c45da7ad8a834ff3a5ff63319ef44d9	70abb1cf820a8e043845c8c4b00aa618	test-user-001	requester	test-user-001	\N	2026-07-17 15:22:49.899563	2026-07-17 15:22:49.899602	\N
8f5afbcf01fbf8d2eee0a7ddca6d48a9	c04bfc81b304b7250075887fe1f088df	test-user-001	requester	test-user-001	\N	2026-07-17 15:32:27.611863	2026-07-17 15:32:27.611905	\N
a764b33b2c658e1c967053a5501d8ca0	f345c824e29906516fbe0189465ffce1	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	requester	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 15:38:45.573115	2026-07-17 15:38:45.573155	\N
e3379cf77cded28963c9e7a19177b41a	c5df834ed66e401415b5dd6673daec08	test-user-001	requester	test-user-001	\N	2026-07-17 15:40:21.118764	2026-07-17 15:40:21.118801	\N
00764693263fbba74c79567a8b6b509c	3a4b1a9a951330795c86931c903bf2ee	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	requester	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 15:40:21.292572	2026-07-17 15:40:21.2926	\N
138263325b464eb8344987d89cb2dfc3	7302ee6eba95301ad6763b3699d1beeb	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	requester	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 15:43:33.785157	2026-07-17 15:43:33.785216	\N
d90360c2b8c5a424f2c499484087cc8a	4349e71ea8f05b93454b5c4a4e942e4e	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	requester	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 15:45:37.648835	2026-07-17 15:45:37.648872	\N
034b398b95e1b68108d4f769875ad769	ad78f52bc1b94cd2e98de45bc58c22e6	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	requester	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 15:48:09.921945	2026-07-17 15:48:09.921984	\N
f345f0eb11adca329b8bcca43b6031d3	98863c9d552ff3cb8b9adaef999aaa6c	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	requester	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 16:07:18.206813	2026-07-17 16:07:18.206871	\N
0338692320a2e773909d04ee2d0bebf4	aa86cf1edfa10b010c7832363541d4bf	test-user-001	requester	test-user-001	\N	2026-07-17 16:56:43.123704	2026-07-17 16:56:43.123736	\N
7fa0a7a29cec6c9b83c1955efe05c89a	bc9516b876776fef675ed22cad25b568	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	requester	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 17:25:54.948864	2026-07-17 17:25:54.948903	\N
44d2bc2fcac7a7eec7711fc7490d08e0	bc9516b876776fef675ed22cad25b568	ou_b8e60596a12d61e0a23a8e9c4a9b1234	developer	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	2026-07-17 17:25:54.97214	2026-07-17 17:25:54.972649	2026-07-17 17:25:54.972689	\N
368c89de848a1370ee48c0e2133c9ccc	bc9516b876776fef675ed22cad25b568	ou_c9f71607b23e72f1b34b9fad5b0c2345	tester	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	2026-07-17 17:25:54.980528	2026-07-17 17:25:54.980922	2026-07-17 17:25:54.980955	\N
77ca3d21a23be4d1f28bcccfc5a6855e	70a26b04993012a23f83e6e66b789b27	ou_creator_test	requester	ou_creator_test	\N	2026-07-17 17:30:09.769953	2026-07-17 17:30:09.769993	\N
ace1dbb95999cd5e8dbc8c3566ed38cc	70a26b04993012a23f83e6e66b789b27	ou_reviewer_0	reviewer	ou_creator_test	2026-07-17 17:30:09.794498	2026-07-17 17:30:09.795013	2026-07-17 17:30:09.795067	\N
1f3ed1e0a2095fda0789a343e801873a	70a26b04993012a23f83e6e66b789b27	ou_reviewer_1	reviewer	ou_creator_test	2026-07-17 17:30:09.803531	2026-07-17 17:30:09.803926	2026-07-17 17:30:09.803959	\N
18209ff972d414181072fc998dd31a74	2f643c53ed2fb49f0ba83681596285da	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	requester	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 17:30:40.691675	2026-07-17 17:30:40.691726	\N
2761d2d540587225a3e47e764419ef61	d69a718e61312614f02be6039f8a28ee	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	requester	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 17:31:57.132567	2026-07-17 17:31:57.132607	\N
24df63470dd7fdb7c5ce458b927367d8	3b1ea8c7428c4366ac79e327ad3cae92	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	requester	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 17:33:34.700918	2026-07-17 17:33:34.700981	\N
b01bbf7c78e7a17e028f748bb8ce98d6	9fb87b94485b0046e01b88df6d36267d	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	requester	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 17:35:16.68554	2026-07-17 17:35:16.685579	\N
\.


--
-- Data for Name: requirement_versions; Type: TABLE DATA; Schema: public; Owner: qm
--

COPY public.requirement_versions (id, requirement_id, version_no, content, content_text, content_hash, fields_data, edited_by, change_summary, created_at, updated_at, deleted_at) FROM stdin;
8c683685c939dbe871bc5010be2d57a6	a65b064cd7f67c8907db041e4bb50cc6	1	\N	库存预警功能	a1449aa1ebb3753e53c0a16aeff8dfb6a95ec75fa73c97c97684c6ba209552ce	\N	test-user-001	\N	2026-07-17 15:21:15.821856	2026-07-17 15:21:15.821856	\N
cd1ac30bcf39e78c80618f7c1a161cfa	70abb1cf820a8e043845c8c4b00aa618	1	\N	库存预警功能	a1449aa1ebb3753e53c0a16aeff8dfb6a95ec75fa73c97c97684c6ba209552ce	\N	test-user-001	\N	2026-07-17 15:22:49.872318	2026-07-17 15:22:49.872318	\N
57515cb52e39ea01592d21ff437f9a91	70abb1cf820a8e043845c8c4b00aa618	2	# 库存预警功能\n## 背景\n...	# 库存预警功能\n## 背景\n...	121d61e0bebcc048865741c7f5f177566a42ba447e084135554b8fef58617787	\N	test-user-001	补充背景描述	2026-07-17 15:22:49.943054	2026-07-17 15:22:49.943054	\N
24926bce1fd64b5a2607cb1c92655332	c04bfc81b304b7250075887fe1f088df	1	\N	流程域测试需求	6a19b5efac8fafa26ca813ed9850e1ba9226f197f3d880822a6eedaaada2a60a	\N	test-user-001	\N	2026-07-17 15:32:27.552509	2026-07-17 15:32:27.552509	\N
522ec088d52e4090f7930064e8e5e9d0	f345c824e29906516fbe0189465ffce1	1	\N	群引擎测试需求	61c7d50cf3669325bbfacb43fb343c292a12e565b74a602b3353c4ee7e92e308	\N	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 15:38:45.515041	2026-07-17 15:38:45.515041	\N
f1f0592a81c89169eb4be74e62bd425d	c5df834ed66e401415b5dd6673daec08	1	\N	综合验证需求	fe9d50c8c8b431a04132e7f2b74364d096b9e7591b3b41467b5428ec58e121cb	\N	test-user-001	\N	2026-07-17 15:40:21.111064	2026-07-17 15:40:21.111064	\N
3273a2309771a657d1e080f8c83a3943	c5df834ed66e401415b5dd6673daec08	2	# 综合验证\n## 背景\n...	# 综合验证\n## 背景\n...	b82ea9963f96c0a504bcc8ae3988c39f37281f4fb9590542778259497ed19ea2	\N	test-user-001	v2	2026-07-17 15:40:21.143538	2026-07-17 15:40:21.143538	\N
3d30217ae00247b64fb4ba371c28aee3	3a4b1a9a951330795c86931c903bf2ee	1	\N	群引擎验证需求	53fd441b6827fcce5eaa75e2da250f73c6aae13227cfb9da4c61bc9c9209ce69	\N	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 15:40:21.28627	2026-07-17 15:40:21.28627	\N
4898028b173a008cfd31e8e739506436	7302ee6eba95301ad6763b3699d1beeb	1	\N	书记员验证需求	341e4a0cfc035eb0d5863ad99d86c02733a84d1b1a805b8de20699735f7b7919	\N	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 15:43:33.726343	2026-07-17 15:43:33.726343	\N
184df8e5766f6ced4daa9f81e1a168b8	4349e71ea8f05b93454b5c4a4e942e4e	1	\N	书记员验证需求	341e4a0cfc035eb0d5863ad99d86c02733a84d1b1a805b8de20699735f7b7919	\N	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 15:45:37.640269	2026-07-17 15:45:37.640269	\N
87a3dffd0b1a52a39719a3def7a9e2a1	ad78f52bc1b94cd2e98de45bc58c22e6	1	\N	书记员验证需求	341e4a0cfc035eb0d5863ad99d86c02733a84d1b1a805b8de20699735f7b7919	\N	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 15:48:09.861683	2026-07-17 15:48:09.861683	\N
85690664cbc4db18cf9a41a53640a22b	98863c9d552ff3cb8b9adaef999aaa6c	1	\N	P0联调验收需求	4a097b952af0d85627cfc02e6820980d01066a667cc8fe5aee92715f993895f7	\N	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 16:07:18.171025	2026-07-17 16:07:18.171025	\N
5a463f9ab488d2c65ddffba02ef7755e	98863c9d552ff3cb8b9adaef999aaa6c	2	# P0验收\n## 背景\n...	# P0验收\n## 背景\n...	fa2bed470612be0183f50b2268db8a45c00982416174e03cf7f983086d38068d	\N	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	补充需求背景	2026-07-17 16:07:18.227354	2026-07-17 16:07:18.227354	\N
c17f94a043817790b3c2a7c9caed2213	aa86cf1edfa10b010c7832363541d4bf	1	\N	增加LLM支持功能	a068ac73175f8a6925589b1d4a9ab7d980d58784db32715e9aa9b6f3230a88ea	\N	test-user-001	\N	2026-07-17 16:56:43.103516	2026-07-17 16:56:43.103516	\N
66c573b60303fbe99328c0efd4fd5c58	bc9516b876776fef675ed22cad25b568	1	\N	A1拉人入群验证	3fe2f99959bd1bfb97bcd6079772b5cea5eef8377f8c68ae4a8a6932214501c2	\N	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 17:25:54.888479	2026-07-17 17:25:54.888479	\N
d557f6c7e96740fdcdf0d3ac618114a4	70a26b04993012a23f83e6e66b789b27	1	\N	卡片回调验证	4e340a07e40c4ff4b6397dd0139732c50b20cfe7ab89046b5c83c4e1cb2e5a4f	\N	ou_creator_test	\N	2026-07-17 17:30:09.71167	2026-07-17 17:30:09.71167	\N
6c013efa5c425039cef8d63acf280cd7	2f643c53ed2fb49f0ba83681596285da	1	\N	卡片回调验证	4e340a07e40c4ff4b6397dd0139732c50b20cfe7ab89046b5c83c4e1cb2e5a4f	\N	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 17:30:40.681961	2026-07-17 17:30:40.681961	\N
10e50cf8a9a7daa61ca0d08f9f110afa	d69a718e61312614f02be6039f8a28ee	1	\N	卡片回调验证v2	9115d95bff7d6c71d288fc2b311994e5dfd850b03f414a7b6cbdf7c30ba41948	\N	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 17:31:57.072257	2026-07-17 17:31:57.072257	\N
920ac5628eb387a45c9fea6df2b4c701	3b1ea8c7428c4366ac79e327ad3cae92	1	\N	卡片回调v3	38dda8e3eb0db6480951ea069fe9f64d7595d507a5320ef33b6bd5dd75cd561f	\N	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 17:33:34.639931	2026-07-17 17:33:34.639931	\N
91306bce71b18658c9a7114fa530cc1c	9fb87b94485b0046e01b88df6d36267d	1	\N	卡片回调v4	a2eb952f89ff0677be26c36dd83073343f6b8a2596d6dc9e5bff0674d24d4131	\N	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 17:35:16.627194	2026-07-17 17:35:16.627194	\N
\.


--
-- Data for Name: requirements; Type: TABLE DATA; Schema: public; Owner: qm
--

COPY public.requirements (id, req_no, title, req_type, product_line, module, priority, status, current_version_id, owner_id, is_confidential, source_channel, created_by, closed_at, created_at, updated_at, deleted_at) FROM stdin;
a65b064cd7f67c8907db041e4bb50cc6	REQ-2026-0001	库存预警功能	feature	QM	\N	P1	clarifying	8c683685c939dbe871bc5010be2d57a6	test-user-001	f	web	test-user-001	\N	2026-07-17 15:21:15.828639	2026-07-17 15:21:15.830018	\N
98863c9d552ff3cb8b9adaef999aaa6c	REQ-2026-0010	P0联调验收需求	feature	core	\N	P1	baselined	5a463f9ab488d2c65ddffba02ef7755e	\N	f	web	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 16:07:18.180864	2026-07-17 16:07:18.180934	\N
70abb1cf820a8e043845c8c4b00aa618	REQ-2026-0002	库存预警功能	feature	QM	\N	P1	clarifying	57515cb52e39ea01592d21ff437f9a91	test-user-001	f	web	test-user-001	\N	2026-07-17 15:22:49.878533	2026-07-17 15:22:49.879866	\N
c04bfc81b304b7250075887fe1f088df	REQ-2026-0003	流程域测试需求	feature	\N	\N	P1	baselined	24926bce1fd64b5a2607cb1c92655332	\N	f	web	test-user-001	\N	2026-07-17 15:32:27.584289	2026-07-17 15:32:27.585732	\N
f345c824e29906516fbe0189465ffce1	REQ-2026-0004	群引擎测试需求	feature	\N	\N	P1	draft	522ec088d52e4090f7930064e8e5e9d0	\N	f	web	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 15:38:45.546332	2026-07-17 15:38:45.547666	\N
aa86cf1edfa10b010c7832363541d4bf	REQ-2026-0011	增加LLM支持功能	feature	产品1	\N	P1	draft	c17f94a043817790b3c2a7c9caed2213	\N	f	web	test-user-001	\N	2026-07-17 16:56:43.108095	2026-07-17 16:56:43.109433	\N
c5df834ed66e401415b5dd6673daec08	REQ-2026-0005	综合验证需求	feature	\N	\N	P1	baselined	3273a2309771a657d1e080f8c83a3943	\N	f	web	test-user-001	\N	2026-07-17 15:40:21.112847	2026-07-17 15:40:21.112891	\N
3a4b1a9a951330795c86931c903bf2ee	REQ-2026-0006	群引擎验证需求	feature	\N	\N	P1	draft	3d30217ae00247b64fb4ba371c28aee3	\N	f	web	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 15:40:21.287463	2026-07-17 15:40:21.287529	\N
7302ee6eba95301ad6763b3699d1beeb	REQ-2026-0007	书记员验证需求	feature	\N	\N	P1	draft	4898028b173a008cfd31e8e739506436	\N	f	web	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 15:43:33.758581	2026-07-17 15:43:33.75991	\N
4349e71ea8f05b93454b5c4a4e942e4e	REQ-2026-0008	书记员验证需求	feature	\N	\N	P1	draft	184df8e5766f6ced4daa9f81e1a168b8	\N	f	web	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 15:45:37.641981	2026-07-17 15:45:37.64202	\N
ad78f52bc1b94cd2e98de45bc58c22e6	REQ-2026-0009	书记员验证需求	feature	\N	\N	P1	draft	87a3dffd0b1a52a39719a3def7a9e2a1	\N	f	web	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 15:48:09.894317	2026-07-17 15:48:09.895682	\N
bc9516b876776fef675ed22cad25b568	REQ-2026-0012	A1拉人入群验证	feature	\N	\N	P1	draft	66c573b60303fbe99328c0efd4fd5c58	\N	f	web	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 17:25:54.920271	2026-07-17 17:25:54.921651	\N
70a26b04993012a23f83e6e66b789b27	REQ-2026-0013	卡片回调验证	feature	\N	\N	P1	draft	d557f6c7e96740fdcdf0d3ac618114a4	\N	f	web	ou_creator_test	\N	2026-07-17 17:30:09.743507	2026-07-17 17:30:09.744891	\N
2f643c53ed2fb49f0ba83681596285da	REQ-2026-0014	卡片回调验证	feature	\N	\N	P1	reviewing	6c013efa5c425039cef8d63acf280cd7	\N	f	web	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 17:30:40.683918	2026-07-17 17:30:40.683985	\N
d69a718e61312614f02be6039f8a28ee	REQ-2026-0015	卡片回调验证v2	feature	\N	\N	P1	reviewing	10e50cf8a9a7daa61ca0d08f9f110afa	\N	f	web	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 17:31:57.105837	2026-07-17 17:31:57.107185	\N
3b1ea8c7428c4366ac79e327ad3cae92	REQ-2026-0016	卡片回调v3	feature	\N	\N	P1	reviewing	920ac5628eb387a45c9fea6df2b4c701	\N	f	web	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 17:33:34.672728	2026-07-17 17:33:34.674123	\N
9fb87b94485b0046e01b88df6d36267d	REQ-2026-0017	卡片回调v4	feature	\N	\N	P1	baselined	91306bce71b18658c9a7114fa530cc1c	\N	f	web	ou_468e8d1b320ec3cb25bbe2ccb14b28c7	\N	2026-07-17 17:35:16.659229	2026-07-17 17:35:16.660636	\N
\.


--
-- Data for Name: review_flows; Type: TABLE DATA; Schema: public; Owner: qm
--

COPY public.review_flows (id, requirement_id, round_no, review_type, mode, status, started_at, finished_at, created_at, updated_at, deleted_at) FROM stdin;
bbf509043738124ad784277aa5fcd52d	c04bfc81b304b7250075887fe1f088df	1	final	all	passed	2026-07-17 15:33:19.649876	2026-07-17 15:33:19.703043	2026-07-17 15:33:19.651078	2026-07-17 15:33:19.651143	\N
014b425d3c07c51944ca475f32f65813	c5df834ed66e401415b5dd6673daec08	1	final	all	passed	2026-07-17 15:40:21.19084	2026-07-17 15:40:21.237578	2026-07-17 15:40:21.191643	2026-07-17 15:40:21.19168	\N
8ed8c2a40642ee4a9437fa15ddf788f9	98863c9d552ff3cb8b9adaef999aaa6c	1	final	all	passed	2026-07-17 16:07:18.33043	2026-07-17 16:07:18.431495	2026-07-17 16:07:18.333216	2026-07-17 16:07:18.333301	\N
4cfa0b2338f5e7e43a6c655c151ba42c	2f643c53ed2fb49f0ba83681596285da	1	final	all	in_progress	2026-07-17 17:30:43.190679	\N	2026-07-17 17:30:43.191533	2026-07-17 17:30:43.191569	\N
5c773dc27c9a105e8e4a25decd19f696	d69a718e61312614f02be6039f8a28ee	1	final	all	in_progress	2026-07-17 17:31:59.918624	\N	2026-07-17 17:31:59.920405	2026-07-17 17:31:59.920698	\N
045e5b9f96ff91f23924bef1e8c6b79e	3b1ea8c7428c4366ac79e327ad3cae92	1	final	all	in_progress	2026-07-17 17:33:37.358439	\N	2026-07-17 17:33:37.359653	2026-07-17 17:33:37.359943	\N
6c1a716dbeba633eed311db307f6e7d6	9fb87b94485b0046e01b88df6d36267d	1	final	all	passed	2026-07-17 17:35:19.329434	2026-07-17 17:35:20.021159	2026-07-17 17:35:19.330846	2026-07-17 17:35:19.331101	\N
\.


--
-- Data for Name: review_votes; Type: TABLE DATA; Schema: public; Owner: qm
--

COPY public.review_votes (id, flow_id, voter_id, decision, comment, voted_at, delegated_to, card_msg_id, created_at, updated_at, deleted_at) FROM stdin;
0a4229f71167d6989581470f198b9e5f	bbf509043738124ad784277aa5fcd52d	voter-001	approve	同意	2026-07-17 15:33:19.681484	\N	\N	2026-07-17 15:33:19.654327	2026-07-17 15:33:19.654369	\N
9f08cceffdf93d3458a7f7509c28a24d	bbf509043738124ad784277aa5fcd52d	voter-002	approve	同意	2026-07-17 15:33:19.697965	\N	\N	2026-07-17 15:33:19.65733	2026-07-17 15:33:19.657517	\N
238a37c6ded35b2b97305f4bd8edf879	014b425d3c07c51944ca475f32f65813	voter-001	approve	\N	2026-07-17 15:40:21.213235	\N	\N	2026-07-17 15:40:21.194305	2026-07-17 15:40:21.194346	\N
94d95efee00c51626eae4b05fe66e6f1	014b425d3c07c51944ca475f32f65813	voter-002	approve	\N	2026-07-17 15:40:21.23237	\N	\N	2026-07-17 15:40:21.195936	2026-07-17 15:40:21.195972	\N
f07db69ae0423f74486c947d6cb62b37	8ed8c2a40642ee4a9437fa15ddf788f9	voter-001	approve	\N	2026-07-17 16:07:18.374621	\N	\N	2026-07-17 16:07:18.338918	2026-07-17 16:07:18.339187	\N
f35b70c3c8bb636900b07d4b9e984c59	8ed8c2a40642ee4a9437fa15ddf788f9	voter-002	approve	\N	2026-07-17 16:07:18.402192	\N	\N	2026-07-17 16:07:18.342068	2026-07-17 16:07:18.342134	\N
18caed4f1f57220de8b06925cb3f68ef	8ed8c2a40642ee4a9437fa15ddf788f9	voter-003	approve	\N	2026-07-17 16:07:18.424864	\N	\N	2026-07-17 16:07:18.344129	2026-07-17 16:07:18.344191	\N
de06c8e0bf3199c73565beba2fbf7812	4cfa0b2338f5e7e43a6c655c151ba42c	ou_b8e60596a12d61e0a23a8e9c4a9b1234	pending	\N	\N	\N	\N	2026-07-17 17:30:43.193964	2026-07-17 17:30:43.194005	\N
18bc97e5585227d5f24b962fac750ff7	4cfa0b2338f5e7e43a6c655c151ba42c	ou_c9f71607b23e72f1b34b9fad5b0c2345	pending	\N	\N	\N	\N	2026-07-17 17:30:43.195834	2026-07-17 17:30:43.195872	\N
a1fa56ba7ff8274c501fe521028b2328	5c773dc27c9a105e8e4a25decd19f696	ou_b8e60596a12d61e0a23a8e9c4a9b1234	pending	\N	\N	\N	\N	2026-07-17 17:31:59.924897	2026-07-17 17:31:59.924959	\N
98503f843e1bd0c7975117237e8eca71	5c773dc27c9a105e8e4a25decd19f696	ou_c9f71607b23e72f1b34b9fad5b0c2345	pending	\N	\N	\N	\N	2026-07-17 17:31:59.928403	2026-07-17 17:31:59.928452	\N
2f8a3172dd0ac4730b246fbe7bf7d324	045e5b9f96ff91f23924bef1e8c6b79e	ou_b8e60596a12d61e0a23a8e9c4a9b1234	pending	\N	\N	\N	\N	2026-07-17 17:33:37.364185	2026-07-17 17:33:37.364278	\N
8baf03d740fc51688e30e44c3232df26	045e5b9f96ff91f23924bef1e8c6b79e	ou_c9f71607b23e72f1b34b9fad5b0c2345	pending	\N	\N	\N	\N	2026-07-17 17:33:37.368364	2026-07-17 17:33:37.368424	\N
921f6fd0cdc60b304fdf8cfd0003b88a	6c1a716dbeba633eed311db307f6e7d6	ou_b8e60596a12d61e0a23a8e9c4a9b1234	approve	\N	2026-07-17 17:35:19.952007	\N	\N	2026-07-17 17:35:19.334185	2026-07-17 17:35:19.334231	\N
dfcd874b9845532d77073c36198c5c99	6c1a716dbeba633eed311db307f6e7d6	ou_c9f71607b23e72f1b34b9fad5b0c2345	approve	\N	2026-07-17 17:35:20.015792	\N	\N	2026-07-17 17:35:19.337423	2026-07-17 17:35:19.337462	\N
\.


--
-- Data for Name: templates; Type: TABLE DATA; Schema: public; Owner: qm
--

COPY public.templates (id, name, req_type, field_schema, is_active, created_at, updated_at, deleted_at) FROM stdin;
\.


--
-- Data for Name: trace_links; Type: TABLE DATA; Schema: public; Owner: qm
--

COPY public.trace_links (id, requirement_id, link_type, external_id, external_url, title, source, created_at, updated_at, deleted_at) FROM stdin;
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: qm
--

COPY public.users (id, name, email, feishu_open_id, wecom_user_id, role, status, created_at, updated_at, deleted_at) FROM stdin;
\.


--
-- Name: audit_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: qm
--

SELECT pg_catalog.setval('public.audit_logs_id_seq', 1, false);


--
-- Name: message_archives_id_seq; Type: SEQUENCE SET; Schema: public; Owner: qm
--

SELECT pg_catalog.setval('public.message_archives_id_seq', 2, true);


--
-- Name: acceptance_criteria acceptance_criteria_pkey; Type: CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.acceptance_criteria
    ADD CONSTRAINT acceptance_criteria_pkey PRIMARY KEY (id);


--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- Name: baselines baselines_pkey; Type: CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.baselines
    ADD CONSTRAINT baselines_pkey PRIMARY KEY (id);


--
-- Name: change_requests change_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.change_requests
    ADD CONSTRAINT change_requests_pkey PRIMARY KEY (id);


--
-- Name: message_archives message_archives_pkey; Type: CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.message_archives
    ADD CONSTRAINT message_archives_pkey PRIMARY KEY (id);


--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- Name: org_units org_units_pkey; Type: CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.org_units
    ADD CONSTRAINT org_units_pkey PRIMARY KEY (id);


--
-- Name: requirement_groups requirement_groups_pkey; Type: CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.requirement_groups
    ADD CONSTRAINT requirement_groups_pkey PRIMARY KEY (id);


--
-- Name: requirement_groups requirement_groups_requirement_id_key; Type: CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.requirement_groups
    ADD CONSTRAINT requirement_groups_requirement_id_key UNIQUE (requirement_id);


--
-- Name: requirement_stakeholders requirement_stakeholders_pkey; Type: CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.requirement_stakeholders
    ADD CONSTRAINT requirement_stakeholders_pkey PRIMARY KEY (id);


--
-- Name: requirement_stakeholders requirement_stakeholders_requirement_id_user_id_stakeholder_key; Type: CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.requirement_stakeholders
    ADD CONSTRAINT requirement_stakeholders_requirement_id_user_id_stakeholder_key UNIQUE (requirement_id, user_id, stakeholder_role);


--
-- Name: requirement_versions requirement_versions_pkey; Type: CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.requirement_versions
    ADD CONSTRAINT requirement_versions_pkey PRIMARY KEY (id);


--
-- Name: requirements requirements_pkey; Type: CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.requirements
    ADD CONSTRAINT requirements_pkey PRIMARY KEY (id);


--
-- Name: requirements requirements_req_no_key; Type: CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.requirements
    ADD CONSTRAINT requirements_req_no_key UNIQUE (req_no);


--
-- Name: review_flows review_flows_pkey; Type: CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.review_flows
    ADD CONSTRAINT review_flows_pkey PRIMARY KEY (id);


--
-- Name: review_votes review_votes_pkey; Type: CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.review_votes
    ADD CONSTRAINT review_votes_pkey PRIMARY KEY (id);


--
-- Name: templates templates_pkey; Type: CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.templates
    ADD CONSTRAINT templates_pkey PRIMARY KEY (id);


--
-- Name: trace_links trace_links_pkey; Type: CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.trace_links
    ADD CONSTRAINT trace_links_pkey PRIMARY KEY (id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: idx_audit_target; Type: INDEX; Schema: public; Owner: qm
--

CREATE INDEX idx_audit_target ON public.audit_logs USING btree (target_type, target_id, created_at DESC);


--
-- Name: idx_msg_idem; Type: INDEX; Schema: public; Owner: qm
--

CREATE UNIQUE INDEX idx_msg_idem ON public.message_archives USING btree (im_provider, im_msg_id);


--
-- Name: idx_msg_req; Type: INDEX; Schema: public; Owner: qm
--

CREATE INDEX idx_msg_req ON public.message_archives USING btree (requirement_id, msg_time DESC);


--
-- Name: idx_req_no; Type: INDEX; Schema: public; Owner: qm
--

CREATE INDEX idx_req_no ON public.requirements USING btree (req_no);


--
-- Name: idx_req_status; Type: INDEX; Schema: public; Owner: qm
--

CREATE INDEX idx_req_status ON public.requirements USING btree (status, product_line);


--
-- Name: idx_stake_req; Type: INDEX; Schema: public; Owner: qm
--

CREATE INDEX idx_stake_req ON public.requirement_stakeholders USING btree (requirement_id);


--
-- Name: idx_stake_user; Type: INDEX; Schema: public; Owner: qm
--

CREATE INDEX idx_stake_user ON public.requirement_stakeholders USING btree (user_id);


--
-- Name: idx_trace_req; Type: INDEX; Schema: public; Owner: qm
--

CREATE INDEX idx_trace_req ON public.trace_links USING btree (requirement_id, link_type);


--
-- Name: idx_users_feishu; Type: INDEX; Schema: public; Owner: qm
--

CREATE INDEX idx_users_feishu ON public.users USING btree (feishu_open_id);


--
-- Name: idx_ver_hash; Type: INDEX; Schema: public; Owner: qm
--

CREATE INDEX idx_ver_hash ON public.requirement_versions USING btree (content_hash);


--
-- Name: idx_ver_req; Type: INDEX; Schema: public; Owner: qm
--

CREATE INDEX idx_ver_req ON public.requirement_versions USING btree (requirement_id, version_no DESC);


--
-- Name: acceptance_criteria acceptance_criteria_requirement_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.acceptance_criteria
    ADD CONSTRAINT acceptance_criteria_requirement_id_fkey FOREIGN KEY (requirement_id) REFERENCES public.requirements(id);


--
-- Name: baselines baselines_requirement_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.baselines
    ADD CONSTRAINT baselines_requirement_id_fkey FOREIGN KEY (requirement_id) REFERENCES public.requirements(id);


--
-- Name: change_requests change_requests_requirement_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.change_requests
    ADD CONSTRAINT change_requests_requirement_id_fkey FOREIGN KEY (requirement_id) REFERENCES public.requirements(id);


--
-- Name: message_archives message_archives_requirement_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.message_archives
    ADD CONSTRAINT message_archives_requirement_id_fkey FOREIGN KEY (requirement_id) REFERENCES public.requirements(id);


--
-- Name: requirement_groups requirement_groups_requirement_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.requirement_groups
    ADD CONSTRAINT requirement_groups_requirement_id_fkey FOREIGN KEY (requirement_id) REFERENCES public.requirements(id);


--
-- Name: requirement_stakeholders requirement_stakeholders_requirement_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.requirement_stakeholders
    ADD CONSTRAINT requirement_stakeholders_requirement_id_fkey FOREIGN KEY (requirement_id) REFERENCES public.requirements(id);


--
-- Name: requirement_versions requirement_versions_requirement_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.requirement_versions
    ADD CONSTRAINT requirement_versions_requirement_id_fkey FOREIGN KEY (requirement_id) REFERENCES public.requirements(id);


--
-- Name: review_flows review_flows_requirement_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.review_flows
    ADD CONSTRAINT review_flows_requirement_id_fkey FOREIGN KEY (requirement_id) REFERENCES public.requirements(id);


--
-- Name: review_votes review_votes_flow_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.review_votes
    ADD CONSTRAINT review_votes_flow_id_fkey FOREIGN KEY (flow_id) REFERENCES public.review_flows(id);


--
-- Name: trace_links trace_links_requirement_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: qm
--

ALTER TABLE ONLY public.trace_links
    ADD CONSTRAINT trace_links_requirement_id_fkey FOREIGN KEY (requirement_id) REFERENCES public.requirements(id);


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: qm
--

REVOKE USAGE ON SCHEMA public FROM PUBLIC;


--
-- PostgreSQL database dump complete
--

\unrestrict 8guGrMddSyS8ZfpbdNLdjh88Vw9D0YiwJV5FWmLPTg7mBerWs8FpRCg6iatAw3d

