# chat-app API reference

This repository exposes REST and WebSocket APIs for a chat application. Below is a concise, developer-friendly reference for every public endpoint in the project — request formats, responses, and important notes about authentication and special behavior.

> Use Authorization: Bearer <jwt> for authenticated endpoints (unless OAuth session is active). OAuth and social login endpoints are handled by Spring Security (Google/GitHub).

---

## Authentication endpoints (/api/auth)

- GET /api/auth/oauth-user
	- Description: Return attributes for an active OAuth2 principal.
	- Response (JSON): provider-specific user attributes (map)

- GET /api/auth/oauth2/failure
	- Description: OAuth failure landing endpoint.
	- Response: 401 JSON { "message": "OAuth2 authentication failed", "status": false }

- POST /api/auth/signup
	- Request (JSON) — UserCreationDto:
		{
			"email": "user@example.com",
			"password": "secret",
			"firstName": "First",
			"lastName": "Last",
			"role": ["ROLE_USER"]
		}
	- Response: 201 Created — JwtResponse { "token": "<jwt>", "message": "Registration successful." }

- POST /api/auth/signin
	- Request (JSON) — LoginRequest:
		{ "email": "user@example.com", "password": "secret" }
	- Response: 200 OK — JwtResponse { "token": "<jwt>", "message": "Login successful." }

- GET /api/auth/user/me
	- Description: Returns current user info
	- If JWT-authenticated (UserDetails): returns {username, authorities, enabled, ...}
	- If OAuth2-authenticated: returns provider attributes map

---

## Users (/api/users)

- POST /api/users
	- Request: UserCreationDto (same as signup)
	- Response: 201 Created -> created User object (fields: userId, email, firstName, lastName, avatarUrl, roles, createdAt, ...)

- GET /api/users/{id}
	- Response: 200 -> User

- GET /api/users
	- Response: 200 -> [ User ]

- PUT /api/users/{id}  (multipart/form-data)
	- Parts: `dto` (JSON string form of UserUpdateDto), optional `image`
	- UserUpdateDto fields: password, firstName, lastName, avatarUrl, location {country, city}, bio, privacySettings {profileVisibility, searchVisibility}, availability, phoneNumber, gender
	- Response: 200 -> updated User

- DELETE /api/users/{id}
	- Response: 200 -> Mono<Void> (empty)

- GET /api/users/{userId}/stats
	- Example response: { "completedTasks": 10, "ongoingTasks": 2, "skills": ["Java","Spring Boot"] }

- PUT /api/users/{userId}/status
	- Request: { "status": "active" }
	- Response: 200 -> { "message": "Status updated successfully" }

- GET /api/users/count/active
	- Response: 200 -> number (Long)

---

## Chats (/api/chats)

- POST /api/chats
	- Request: ChatRequest { type: "DIRECT"|"GROUP", name: (group name), participantIds: ["id1","id2"] }
	- Response: 201 -> Chat object (chatId, participants, lastMessage, lastMessageTime, unreadCounts...)

- GET /api/chats/{id}
	- Response: 200 -> Chat

- POST /api/chats/isExisted
	- Request: ["participant1","participant2", ...]
	- Response: 200 -> String chatId or null

- GET /api/chats/user/{userId}
	- Response: 200 -> [ Chat ]

- GET /api/chats/{chatId}/participants
	- Response: 200 -> [ ChatParticipantsDto {id, firstName, lastName, avatarUrl, isOnline, lastActive} ]

- POST /api/chats/online-users-count  (optional body of participantIds)
	- Response: integer

- GET /api/chats/online-users-count
	- Response: integer (placeholder)

---

## Messages (/api/messages)

HTTP endpoints:

- POST /api/messages/change/{chatId}/{newId}
	- Response: 200 -> [ Message ] (messages re-assigned to new chatId)

- PUT /api/messages/{messageId}
	- Request: MessageEditDto { content, attachmentUrl, attachmentName }
	- Response: 200 -> updated Message

- DELETE /api/messages/{messageId}
	- Response: 200 -> "Message deleted successfully."

- GET /api/messages/chat/{chatId}?lastMsgId={id}&size={n}
	- Response: 200 -> [ Message ]

- POST /api/messages/chat/{chatId}/upload-attachment
	- Request: multipart form — file part `attachment`
	- Response: 200 -> String (S3 URL)

- PUT /api/messages/{chatId}/read
	- Request: { "readerId": "userId" }
	- Response: 200 -> "Messages marked as read successfully."

- GET /api/messages/{chatId}/unreadMsgs?readerId={readerId}
	- Response: 200 -> [ Message ]

Message model (fields): messageId, chatId, senderId, content, attachment, attachmentName, sentAt, editedAt, readBy[]

WebSocket (STOMP):

- Client send to: /app/chat/{chatId}/send (payload: MessageRequest {senderId,content,attachmentUrl,attachmentName})
- Server broadcasts to: /topic/chat/{chatId}

---

## User Status (WebSocket & HTTP)

- POST /users/{userId}/status/{isOnline}
	- Updates user online status and broadcasts to /topic/user-status { userId, isOnline }

---

## OAuth2 / Login (Spring-managed)

- GET /oauth2/authorization/{provider} (e.g. /oauth2/authorization/google, /oauth2/authorization/github)
	- Redirects to provider's authorization page

- Callback: GET /login/oauth2/code/{provider}
	- Provider redirects back with `code`; Spring handles the token exchange.
	- Note: GitHub profile response often does not include `email` or `name`. This project includes an OAuth2 abstraction and Github-specific logic — if `email` is null, the service should call `https://api.github.com/user/emails` with the access token to obtain the primary/verified email. See implementation notes in code.

---



