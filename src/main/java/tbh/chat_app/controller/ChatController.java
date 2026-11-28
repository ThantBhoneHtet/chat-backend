package tbh.chat_app.controller;

import java.lang.module.ResolutionException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tbh.chat_app.dto.chat.ChatParticipantsDto;
import tbh.chat_app.dto.chat.ChatRequest;
import tbh.chat_app.dto.chat.GpChatEditDto;
import tbh.chat_app.config.jwt.UserPrincipal;
import tbh.chat_app.model.Chat;
import tbh.chat_app.repository.ChatRepository;
import tbh.chat_app.service.ChatService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatRepository chatRepository;

    @Autowired
    private ChatService chatService;

    ChatController(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    @PostMapping
    public ResponseEntity<?> createChat(
            @RequestBody ChatRequest chatRequest,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null || chatRequest == null || chatRequest.participantIds() == null) {
            return ResponseEntity.badRequest().body("Invalid chat request.");
        }
        if (chatRequest.participantIds().stream().anyMatch(id -> id == null || id.isBlank())) {
            return ResponseEntity.badRequest().body("Invalid participant list.");
        }
        List<String> participantIds = chatRequest.participantIds().stream().distinct().toList();
        if (!participantIds.contains(principal.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if ("GROUP".equalsIgnoreCase(chatRequest.type())) {
            if (participantIds.size() < 2 || !StringUtils.hasText(chatRequest.name())) {
                return ResponseEntity.badRequest().body("A group needs a name and at least two members.");
            }
            ChatRequest authorizedRequest = new ChatRequest(
                    chatRequest.type(),
                    chatRequest.name().trim(),
                    principal.getId(),
                    chatRequest.gpImageUrl(),
                    participantIds);
            return new ResponseEntity<>(
                    chatService.createChat(authorizedRequest).block(),
                    HttpStatus.CREATED);
        }
        ChatRequest authorizedRequest = new ChatRequest(
                chatRequest.type(),
                chatRequest.name(),
                chatRequest.owner(),
                chatRequest.gpImageUrl(),
                participantIds);
        return new ResponseEntity<>(
                chatService.createChat(authorizedRequest).block(),
                HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateGroupChat(
            @PathVariable String id,
            @RequestBody GpChatEditDto gpChatEditDto,
            @AuthenticationPrincipal UserPrincipal principal) {
        Chat chat = chatService.getChat(id).block();
        if (chat == null) {
            return ResponseEntity.notFound().build();
        }
        if (principal == null || !Objects.equals(chat.getOwner(), principal.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!"GROUP".equalsIgnoreCase(chat.getType())) {
            return ResponseEntity.badRequest().body("Only groups can be edited.");
        }

        if (gpChatEditDto == null) {
            return ResponseEntity.badRequest().body("No data to update.");
        }
        
        if (gpChatEditDto.name() != null) {
            if (!StringUtils.hasText(gpChatEditDto.name())) {
                return ResponseEntity.badRequest().body("Group name cannot be empty.");
            }
            chat.setGroupName(gpChatEditDto.name().trim());
        }

        if (gpChatEditDto.gpImageUrl() != null) {
            chat.setGpImageUrl(gpChatEditDto.gpImageUrl());
        }

        chatService.updateChat(chat);
        return ResponseEntity.ok().body("Updated.");
    }

    @GetMapping("/{id}")
    public Chat getChatById(@PathVariable String id) {
        return chatService.getChat(id).block();
    }

    // @GetMapping("/isExisted/{participant1}/{participant2}")
    // public String getChatIdBetweenParticipants(@PathVariable String participant1, @PathVariable String participant2) {
    //     Chat chat = chatService.getChatIdBetweenParticipants(List.of(participant1, participant2));
    //     if(chat != null) return chat.getChatId();
    //     return null;
    // }

    @PostMapping("/isExisted")
    public ResponseEntity<String> getChatIdBetweenParticipants(@RequestBody List<String> participantIds) {
        Chat chat = chatService.getChatIdBetweenParticipants(participantIds);
        if (chat == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(chat.getChatId());
    }


    @GetMapping("/user/{userId}")
    public List<Chat> getChatsByUserId(@PathVariable String userId) {
        return chatService.getChatsByUserId(userId).collectList().block();
    }

    @GetMapping("/{chatId}/participants")
    public List<ChatParticipantsDto> getChatParticipantsDetails(@PathVariable String chatId) {
        return chatService.getChatParticipantsDetails(chatId);
    }

    // This endpoint is not currently used in React(I commented out)
    @GetMapping("/user/{userId}/status")
    public ChatParticipantsDto getChatUserStatus(@PathVariable String userId) {
        return chatService.getChatUserStatus(userId);
    }

    @PostMapping("/online-users-count")
    public Integer getOnlineUsersCount(@RequestBody(required = false) List<String> participantIds) {
        if (participantIds != null) {
            int count = chatService.getOnlineUsersCount(participantIds);
            return count;
        } 
        return 0;
    }

    @GetMapping("/online-users-count")
    public Integer getOnlineUsersCount() {
        return chatService.getOnlineUsersCount();
    }

    @PutMapping("/{chatId}/update-participants")
    public ResponseEntity<?> updateChatParticipants(
            @PathVariable String chatId, 
            @RequestBody List<String> participantIds,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (chatId == null || participantIds == null || principal == null) {
            return ResponseEntity.badRequest().body("Invalid input.");
        }

        Chat chat = chatService.getChat(chatId).block();

        if (chat == null) {
            return ResponseEntity.notFound().build();
        }
        if (!"GROUP".equalsIgnoreCase(chat.getType()) || chat.getParticipants() == null) {
            return ResponseEntity.badRequest().body("Only group members can be updated.");
        }
        if (!chat.getParticipants().contains(principal.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (participantIds.stream().anyMatch(id -> id == null || id.isBlank())) {
            return ResponseEntity.badRequest().body("Invalid participant list.");
        }

        Set<String> currentParticipants = Set.copyOf(chat.getParticipants());
        Set<String> requestedParticipants = Set.copyOf(participantIds);
        boolean isOwner = Objects.equals(chat.getOwner(), principal.getId());
        if (isOwner) {
            if (!requestedParticipants.contains(chat.getOwner()) || requestedParticipants.size() < 2) {
                return ResponseEntity.badRequest().body("A group owner must remain in a group with at least two members.");
            }
        } else {
            Set<String> expectedParticipants = new java.util.HashSet<>(currentParticipants);
            expectedParticipants.remove(principal.getId());
            if (!requestedParticipants.equals(expectedParticipants)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        chat.setParticipants(participantIds.stream().distinct().toList());

        Map<String, Integer> unread = chat.getUnreadCounts();
        if (unread == null) unread = new HashMap<>();

        Map<String, Integer> updated = new HashMap<>();

        for (String pid : participantIds) {
            updated.put(pid, unread.getOrDefault(pid, 0));
        }

        chat.setUnreadCounts(updated);

        chatService.updateChat(chat);

        return ResponseEntity.ok("Updated.");
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<?> deleteChat(
            @PathVariable String chatId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (chatId == null || principal == null) {
            return ResponseEntity.badRequest().body("ChatId is empty");
        }
        Chat chat = chatService.getChat(chatId).block();
        if (chat == null) return ResponseEntity.notFound().build();
        if (chat.getParticipants() == null || !chat.getParticipants().contains(principal.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if ("GROUP".equalsIgnoreCase(chat.getType())
                && !Objects.equals(chat.getOwner(), principal.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        chatService.deleteChat(chatId);
        return ResponseEntity.ok().body("Chat deleted.");
    }
}
