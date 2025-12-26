package tbh.chat_app.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.WriteResult;

import tbh.chat_app.dto.chat.ChatParticipantsDto;
import tbh.chat_app.dto.chat.ChatRequest;
import tbh.chat_app.model.Chat;
import tbh.chat_app.model.Message;
import tbh.chat_app.model.User;
import tbh.chat_app.repository.ChatRepository;
import tbh.chat_app.utils.ChatCustomRepo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ChatService {
    
    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private ChatCustomRepo chatCustomRepo;

    @Autowired
    private UserService userService;

    @Autowired
    private MessageService messageService;

    public Mono<Chat> createChat(ChatRequest chatRequest) {
        System.out.println("chat request ===================\n"+chatRequest);
        Chat chat = new Chat();
        chat.setCreatedAt(Timestamp.now());
        chat.setType(chatRequest.type());
        chat.setParticipants(chatRequest.participantIds());

        List<String> participants = new ArrayList<>(chatRequest.participantIds());
        Collections.sort(participants);

        String key;

        // Direct chat
        if ("DIRECT".equalsIgnoreCase(chatRequest.type())) {
            key = "direct_" + String.join("_", participants);
        } 
        // Group chat
        else {
            String groupName = chatRequest.name() != null ? chatRequest.name() : "";
            key = "group_" + groupName + "_" + String.join("_", participants);
        }

        String hashedChatId = DigestUtils.sha256Hex(key);
        chat.setChatId(hashedChatId);

        // unread counts
        Map<String, Integer> unreadCounts = new HashMap<>();
        participants.forEach(p -> unreadCounts.put(p, 0));
        chat.setUnreadCounts(unreadCounts);

        if (chatRequest.name() != null)
            chat.setGroupName(chatRequest.name());

        if (chatRequest.owner() != null) 
            chat.setOwner(chatRequest.owner());

        if (chatRequest.gpImageUrl() != null) 
            chat.setGpImageUrl(chatRequest.gpImageUrl());

        return chatRepository.save(chat);
    }


    public Mono<Chat> getChat(String id) {
        return chatRepository.findById(id);
    }

    public Flux<Chat> getChatsByUserId(String userId) {
        return chatRepository.findByParticipantsContaining(userId);
    }

    public Chat getChatIdBetweenParticipants(List<String> participantIds) {
        System.out.println("Participants ids: "+participantIds);
        List<String> sorted = new ArrayList<>(participantIds);
        Collections.sort(sorted);
        String key = "direct_" + String.join("_", sorted);
        String chatId = DigestUtils.sha256Hex(key);
        return chatRepository.findById(chatId).block();
        // return chatCustomRepo.getChatIdBetweenParticipants(participantIds);
    }

    public List<ChatParticipantsDto> getChatParticipantsDetails(String chatId) {
        Chat chat = chatRepository.findById(chatId).block();
        if (chat == null) return null;

        List<ChatParticipantsDto> participants = chat.getParticipants().stream()
        .map(id -> {
            User user = userService.getUser(id);
            if (user != null) {
                return new ChatParticipantsDto(
                    user.getUserId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getAvatarUrl(),
                    user.getIsOnline(),
                    user.getLastActive()
                );
            }

            return null; // participant not found in both
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

        return participants;
    }

    public ChatParticipantsDto getChatUserStatus(String userId) {
        User user = userService.getUser(userId);
        if (user == null) return null;

        return new ChatParticipantsDto(
            user.getUserId(),
            user.getFirstName(),
            user.getLastName(),
            user.getAvatarUrl(),
            user.getIsOnline(),
            user.getLastActive()
        );
    }

    public void updateChat(Chat chat) {
        chatCustomRepo.updateChat(chat);
    }

    public void deleteChat(String id) {
        chatRepository.deleteById(id).block();
        List<Message> messagesInChat = messageService.fetchMessagesInChat(id);
        messagesInChat.forEach(msg -> {
            messageService.deleteMessage(msg.getMessageId());
        });
    }

    public int getOnlineUsersCount(List<String> participantIds) {
            if (participantIds == null || participantIds.isEmpty()) {
                return 0;
            }

            return participantIds.stream()
                    .mapToInt(id -> {
                        User user = userService.getUser(id);
                        if (user != null && Boolean.TRUE.equals(user.getIsOnline())) {
                            return 1;
                        };
                        return 0;
                    })
                    .sum();
        }
    }
