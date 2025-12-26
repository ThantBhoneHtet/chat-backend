package tbh.chat_app.controller;

import java.lang.module.ResolutionException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> createChat(@RequestBody ChatRequest chatRequest) {
        return new ResponseEntity<>(chatService.createChat(chatRequest).block(), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateGroupChat(@PathVariable String id, @RequestBody GpChatEditDto gpChatEditDto) {
        Chat chat = chatService.getChat(id).block();
        if (chat == null) {
            return ResponseEntity.notFound().build();
        }

        if (gpChatEditDto == null) {
            return ResponseEntity.badRequest().body("No data to update.");
        }
        
        if (gpChatEditDto.name() != null) {
            chat.setGroupName(gpChatEditDto.name());
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
    public String getChatIdBetweenParticipants(@RequestBody List<String> participantIds) {
        System.out.println("getChatIdbetweenParticipants called ================");
        System.out.println("Participants ids: "+participantIds);
        Chat chat = chatService.getChatIdBetweenParticipants(participantIds);
        System.out.println("Fetched chat : "+ chat);
        return (chat != null) ? chat.getChatId() : null;
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
        // get all online users from db
        return 30;
    }

    @PutMapping("/{chatId}/update-participants")
    public ResponseEntity<?> updateChatParticipants(
            @PathVariable String chatId, 
            @RequestBody List<String> participantIds) {

        if (chatId == null || participantIds == null) {
            return ResponseEntity.badRequest().body("Invalid input.");
        }

        Chat chat = chatService.getChat(chatId).block();

        if (chat == null) {
            return ResponseEntity.notFound().build();
        }

        chat.setParticipants(participantIds);

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
    public ResponseEntity<?> deleteChat(@PathVariable String chatId) {
        if(chatId == null) return ResponseEntity.badRequest().body("ChatId is empty");
        if(chatService.getChat(chatId) == null) return ResponseEntity.badRequest().body("Chat not exists.");
        chatService.deleteChat(chatId);
        return ResponseEntity.ok().body("Chat deleted.");
    }
}
