///////////////////////////////////////////////////////
// AUTHENTICATION & PROFILE
///////////////////////////////////////////////////////

Table user {
id BIGINT [pk, increment]
email VARCHAR(100) [not null, unique]
password_hash VARCHAR(255) [not null]
salt VARCHAR(255)
created_at DATETIME [not null, default: 'now()']
updated_at DATETIME [not null, default: 'now()']
}

Table user_profiles {
user_id BIGINT [pk, ref: > user.id]
username VARCHAR(50) [not null, unique]
nickname VARCHAR(100)
avatar_url VARCHAR(255)
state TINYINT [not null, default: 1]
mobile VARCHAR(20) [unique]
gender TINYINT
birthday DATE
is_private TINYINT(1) [not null, default: 0]
bio_description TEXT
deleted_at DATETIME
created_at DATETIME [not null, default: 'now()']
updated_at DATETIME [not null, default: 'now()']
}

Table user_verifications {
id BIGINT [pk, increment]
user_id BIGINT [not null, ref: > user.id]
otp_code VARCHAR(10) [not null]
otp_hash VARCHAR(255)
type VARCHAR(20) [not null]
is_verified TINYINT(1) [not null, default: 0]
is_deleted TINYINT(1) [not null, default: 0]
expires_at DATETIME [not null]
created_at DATETIME [not null, default: 'now()']
updated_at DATETIME [not null, default: 'now()']

Indexes {
(user_id, is_verified, expires_at)
}
}

Table user_keys {
user_id BIGINT [pk, ref: > user.id]
public_key TEXT [not null]
private_key TEXT [not null]
created_at DATETIME [not null, default: 'now()']
updated_at DATETIME [not null, default: 'now()']
}

Table user_sessions {
session_id CHAR(36) [pk]
user_id BIGINT [not null, ref: > user.id]
jti CHAR(36) [not null]
ip_address VARCHAR(45)
user_agent TEXT
is_revoked TINYINT(1) [not null, default: 0]
created_at DATETIME [not null, default: 'now()']
last_active DATETIME [not null, default: 'now()']
expires_at DATETIME [not null]

Indexes {
user_id
jti [unique]
expires_at
}
}

Table used_refresh_tokens {
id BIGINT [pk, increment]
session_id CHAR(36) [not null, ref: > user_sessions.session_id]
jti CHAR(36) [not null]
used_at DATETIME [not null, default: 'now()']

Indexes {
(session_id)
}
}

Table user_followers {
follower_id BIGINT [not null, ref: > user.id]
following_id BIGINT [not null, ref: > user.id]
relation_type ENUM('FOLLOWER','FRIEND','BEST_FRIEND','PRIORITY') [not null, default: 'FOLLOWER']
created_at DATETIME [not null, default: 'now()']

Indexes {
(follower_id, following_id) [pk, name: 'pk_user_followers']
following_id
}
}

///////////////////////////////////////////////////////
// BLOG
///////////////////////////////////////////////////////

Table posts {
id BIGINT [pk, increment]
user_id BIGINT [not null, ref: > user.id]
content TEXT
visibility ENUM('PUBLIC','FRIENDS','PRIVATE') [not null, default: 'PUBLIC']
maximum_images INT [not null, default: 15]
created_at DATETIME [not null, default: 'now()']
updated_at DATETIME [not null, default: 'now()']

Indexes { (user_id) }
}

Table post_images {
id BIGINT [pk, increment]
post_id BIGINT [not null, ref: > posts.id]
image_url VARCHAR(255) [not null]
display_order INT [not null, default: 0, note: 'slots 0..14; hard cap 15']

Indexes {
(post_id)
(post_id, display_order) [unique, name: 'uq_post_display_order']
}
}

Table reaction_types {
id TINYINT [pk]
name VARCHAR(20) [not null, unique]
icon_url VARCHAR(255)
}

Table post_reactions {
post_id BIGINT [not null, ref: > posts.id]
user_id BIGINT [not null, ref: > user.id]
reaction_type_id TINYINT [not null, ref: > reaction_types.id]
created_at DATETIME [not null, default: 'now()']

Indexes {
(post_id, user_id) [pk, name: 'pk_post_reactions']
(user_id)
}
}

Table user_comments {
id BIGINT [pk, increment]
post_id BIGINT [not null, ref: > posts.id]
user_id BIGINT [not null, ref: > user.id]
parent_id BIGINT [ref: > user_comments.id]
content TEXT [not null]
created_at DATETIME [not null, default: 'now()']
updated_at DATETIME [not null, default: 'now()']

Indexes {
(post_id)
(user_id)
(parent_id)
}
}

///////////////////////////////////////////////////////
// CHAT
///////////////////////////////////////////////////////

Table chat_conversations {
id BIGINT [pk, increment]
type ENUM('DIRECT','GROUP') [not null, default: 'DIRECT']
title VARCHAR(120)
photo_url VARCHAR(255)
created_by BIGINT [not null, ref: > user.id]
last_message_id BIGINT  // pointer only (no FK in SQL to avoid circular ref)
created_at DATETIME(3) [not null, default: 'CURRENT_TIMESTAMP(3)']
updated_at DATETIME(3) [not null, default: 'CURRENT_TIMESTAMP(3)']
}

Table chat_participants {
conversation_id BIGINT [not null, ref: > chat_conversations.id]
user_id BIGINT [not null, ref: > user.id]
role ENUM('OWNER','ADMIN','MEMBER') [not null, default: 'MEMBER']
is_muted TINYINT(1) [not null, default: 0]
joined_at DATETIME(3) [not null, default: 'CURRENT_TIMESTAMP(3)']

Indexes {
(conversation_id, user_id) [pk, name: 'pk_chat_participants']
(user_id) [name: 'idx_cp_user']
}
}

Table chat_direct_pairs {
conversation_id BIGINT [not null, ref: > chat_conversations.id]
user_id_small BIGINT [not null, ref: > user.id]
user_id_large BIGINT [not null, ref: > user.id]
created_at DATETIME(3) [not null, default: 'CURRENT_TIMESTAMP(3)']

Indexes {
(user_id_small, user_id_large) [unique, name: 'uq_direct_pair']
}
}

Table chat_messages {
id BIGINT [pk, increment]
conversation_id BIGINT [not null, ref: > chat_conversations.id]
sender_id BIGINT [not null, ref: > user.id]
content TEXT [not null]
content_type ENUM('TEXT','IMAGE','FILE','AUDIO','VIDEO','SYSTEM') [not null, default: 'TEXT']
reply_to_id BIGINT [ref: > chat_messages.id]
created_at DATETIME(3) [not null, default: 'CURRENT_TIMESTAMP(3)']

Indexes {
(conversation_id, id) [name: 'idx_cm_conv_seq']
(sender_id, id) [name: 'idx_cm_sender']
}
}

Table chat_message_attachments {
id BIGINT [pk, increment]
message_id BIGINT [not null, ref: > chat_messages.id]
file_url VARCHAR(500) [not null]
file_name VARCHAR(255)
mime_type VARCHAR(100)
file_size BIGINT
width INT
height INT
duration_ms INT

Indexes { (message_id) [name: 'idx_cma_message'] }
}

Table chat_read_progress {
user_id BIGINT [not null, ref: > user.id]
conversation_id BIGINT [not null, ref: > chat_conversations.id]
last_read_msg_id BIGINT [not null, ref: > chat_messages.id]
updated_at DATETIME(3) [not null, default: 'CURRENT_TIMESTAMP(3)']

Indexes {
(user_id, conversation_id) [pk, name: 'pk_chat_read_progress']
(conversation_id, last_read_msg_id) [name: 'idx_crp_conv']
}
}

Table chat_message_reactions {
message_id BIGINT [not null, ref: > chat_messages.id]
user_id BIGINT [not null, ref: > user.id]
reaction_type_id TINYINT [not null, ref: > reaction_types.id]
created_at DATETIME [not null, default: 'now()']

Indexes {
(message_id, user_id) [pk, name: 'pk_chat_message_reactions']
(user_id) [name: 'idx_cmr_user']
}
}

Table chat_pins {
conversation_id BIGINT [not null, ref: > chat_conversations.id]
message_id BIGINT [not null, ref: > chat_messages.id]
pinned_by BIGINT [not null, ref: > user.id]
pinned_at DATETIME [not null, default: 'now()']

Indexes { (conversation_id, message_id) [pk, name: 'pk_chat_pins'] }
}
