# Entity & DTO Reference (chat-app)

This document explains the project's data transfer objects (DTOs) and domain models used across the backend API and WebSocket messaging.

---

## DTOs

These DTOs are located under `src/main/java/tbh/chat_app/dto/` and its subpackages.

### UserCreationDto (dto/user/UserCreationDto.java)
- Purpose: Request payload for registering new users (signup and API create user)
- Fields:
	- email: String (must be a valid email)
	- password: String (not blank)
	- firstName: String (not blank)
	- lastName: String (not blank)
	- role: List<Role> (role list, e.g., [ROLE_USER])
- Example request:
```json
{
	"email": "alice@example.com",
	"password": "supersecret",
	"firstName": "Alice",
	"lastName": "Anderson",
	"role": ["ROLE_USER"]
}
```

### LoginRequest (dto/user/LoginRequest.java)
- Purpose: Sign-in request for username/email + password flow
- Fields:
	- email: String
	- password: String
- Example:
```json
{ "email": "alice@example.com", "password": "supersecret" }
```

### JwtResponse (dto/user/JwtResponse.java)
- Purpose: Authentication responses that return a JWT+message
- Fields:
	- token: String (JWT)
	- message: String
- Example:
```json
{ "token":"eyJ...", "message":"Login successful." }
```

### UserUpdateDto (dto/user/UserUpdateDto.java)
- Purpose: Update user details (sent as `dto` part in multipart requests for profile update)
- Fields:
	- password: String (optional)
	- firstName: String
	- lastName: String
	- avatarUrl: String
	- location: { country: String, city: String }
	- bio: String
	- privacySettings: { profileVisibility: String, searchVisibility: Boolean }
	- availability: List<String>
	- phoneNumber: String
	- gender: String
- Example: (JSON body inside multipart part named `dto`)
```json
{
	"firstName":"Alice",
	"lastName":"Anderson",
	"avatarUrl":"https://...",
	"location":{"country":"USA","city":"NY"},
	"bio":"Developer",
	"privacySettings":{"profileVisibility":"public","searchVisibility":true}
}
```

### ChatRequest (dto/chat/ChatRequest.java)
- Purpose: Create / describe chat creation requests
- Fields:
	- type: String — "DIRECT" or "GROUP"
	- name: String — required for GROUP
	- participantIds: List<String>
- Example:
```json
{ "type":"DIRECT", "participantIds":["userA","userB"] }
```

### ChatParticipantsDto (dto/chat/ChatParticipantsDto.java)
- Purpose: Returned summary for chat participants
- Fields:
	- id: String
	- firstName: String
	- lastName: String
	- avatarUrl: String
	- isOnline: Boolean
	- lastActive: Timestamp (Firestore timestamp)
- Example:
```json
{
 "id":"u1",
 "firstName":"Alice",
 "lastName":"Anderson",
 "avatarUrl":"https://...",
 "isOnline":true,
 "lastActive":"2025-11-01T...Z"
}
```

### MessageRequest (dto/message/MessageRequest.java)
- Purpose: Message payload used when sending messages over WebSocket and HTTP
- Fields:
	- senderId: String
	- content: String
	- attachmentUrl: String (optional)
	- attachmentName: String (optional)
- Example:
```json
{ "senderId":"u1","content":"Hello","attachmentUrl":null }
```

### MessageEditDto (dto/message/MessageEditDto.java)
- Purpose: Edit an existing message
- Fields:
	- content: String
	- attachmentUrl: String
	- attachmentName: String

### MessageResponse (dto/message/MessageResponse.java)
- Purpose: (present but commented) Intended response DTO for messages; currently the project returns the `Message` domain object.

---

## Models (domain objects saved in Firestore)

Models are located under `src/main/java/tbh/chat_app/model`.

### User (model/User.java)
- Purpose: Persisted user object stored in Firestore `users` collection
- Fields:
	- userId: String (document id)
	- email: String
	- password: String (hashed when using BCrypt in the app)
	- firstName: String
	- lastName: String
	- avatarUrl: String
	- location: { coutry, city }
	- bio: String
	- gender: String
	- phoneNumber: String
	- status: String (e.g., "active" | "suspended")
	- createdAt: Timestamp
	- lastLogin: Timestamp
	- privacySettings: { profileVisibility, searchVisibility }
	- lastActive: Timestamp
	- isOnline: Boolean
	- roles: List<Role>
    - savedMessages: List<String> (references to Message ids)
	- provider: AuthProvider (OAuth provider enum)
	- providerId: String

Example (simplified):
```json
{
	"userId":"u1",
	"email":"alice@example.com",
	"firstName":"Alice",
	"lastName":"Anderson",
	"avatarUrl":"https://...",
	"status":"active",
	"roles":["ROLE_USER"]
}
```

Notes:
- `provider` & `providerId` are used when users sign in via OAuth providers; `provider` is an enum (e.g. GOOGLE, GITHUB).

### Chat (model/Chat.java)
- Purpose: Chat metadata object saved in Firestore `chats` collection
- Fields:
	- chatId: String (document id)
	- participants: List<String> (user ids)
	- lastMessage: String
	- lastMessageTime: Timestamp
	- unreadCounts: Map<String,Integer> (unread counts per user)
	- createdAt: Timestamp
	- type: String ("group" | "direct" | "global")
	- groupName: String (for group chats)

Example:
```json
{
	"chatId":"c1",
	"participants":["u1","u2"],
	"lastMessage":"Hey",
	"type":"DIRECT"
}
```

### Message (model/Message.java)
- Purpose: Chat message document in `messages` collection
- Fields:
	- messageId: String
	- chatId: String
	- senderId: String
	- content: String
	- attachment: String (URL)
	- attachmentName: String
	- sentAt: Timestamp
	- editedAt: Timestamp
	- readBy: List<String> (user ids)

Example:
```json
{
	"messageId":"m1",
	"chatId":"c1",
	"senderId":"u1",
	"content":"Hello",
	"sentAt":"2025-11-30T...Z",
	"readBy":[]
}
```

### UserPrincipal (config/jwt/UserPrincipal.java)
- Purpose: Adapter implementing `UserDetails` and `OAuth2User` so a single principal type can be used for both JWT-based and OAuth principals in security flows.
- Fields:
	- id: String (userId)
	- email: String
	- password: String
	- authorities: Collection<GrantedAuthority>
	- attributes: Map<String,Object> (OAuth attributes when present)

Example:
```json
{
	"id":"u1",
	"email":"alice@example.com",
	"authorities":[{"authority":"ROLE_USER"}],
	"attributes":{...}
}
```

---

## OAuth2 abstraction

To make provider integrations consistent, the project includes an abstraction layer that normalizes provider attributes across OIDC and OAuth2 providers. These are located under `src/main/java/tbh/chat_app/config/oauth/user/`.

### OAuth2UserInfo (interface)
- Purpose: A provider-agnostic accessor interface that normalizes common fields (id, email, name, firstName, lastName, imageUrl) from provider-specific attribute maps.

### GoogleOAuth2UserInfo (Google implementation)
- Purpose: Maps OpenID/Google claim names (sub -> id, email, name, given_name, family_name, picture)

### GithubOAuth2UserInfo (GitHub implementation)
- Purpose: Maps GitHub profile attribute names to the normalized interface. Note: GitHub profile objects frequently have `email: null` — GitHub requires the `user:email` scope and an extra call to `GET /user/emails` to return the primary/verified email. The service code may need to call the email endpoint when email is missing from the main profile.

### OAuth2UserInfoFactory
- Purpose: Simple factory that returns the correct provider implementation for a given registrationId (e.g. "google" or "github"). This keeps onboarding logic provider-agnostic in the application OAuth2 services.

Where used:
- `OAuth2UserService` (for generic OAuth2 providers) calls the factory to normalize attributes and then persists/updates `User` entities.
- `CustomOidcUserService` uses the same normalized form for OIDC providers like Google.


---


