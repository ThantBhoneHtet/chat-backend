package tbh.chat_app.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import reactor.core.publisher.Mono;
import tbh.chat_app.config.jwt.UserPrincipal;
import tbh.chat_app.dto.chat.GpChatEditDto;
import tbh.chat_app.model.Chat;
import tbh.chat_app.repository.ChatRepository;
import tbh.chat_app.service.ChatService;

class ChatControllerTest {

    private ChatController controller;
    private ChatService chatService;
    private Chat group;
    private UserPrincipal owner;

    @BeforeEach
    void setUp() {
        controller = new ChatController(mock(ChatRepository.class));
        chatService = mock(ChatService.class);
        ReflectionTestUtils.setField(controller, "chatService", chatService);
        group = new Chat();
        group.setChatId("group-1");
        group.setType("GROUP");
        group.setOwner("owner-1");
        group.setParticipants(List.of("owner-1", "member-1"));
        when(chatService.getChat("group-1")).thenReturn(Mono.just(group));
        owner = new UserPrincipal("owner-1", "owner@example.com", "", List.of());
    }

    @Test
    void allowsOwnerToEditGroupDetails() {
        var response = controller.updateGroupChat(
                "group-1",
                new GpChatEditDto("Updated name", null),
                owner);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Updated name", group.getGroupName());
        verify(chatService).updateChat(group);
    }

    @Test
    void rejectsNonOwnerGroupEdits() {
        var member = new UserPrincipal("member-1", "member@example.com", "", List.of());

        var response = controller.updateGroupChat(
                "group-1",
                new GpChatEditDto("Unauthorized name", null),
                member);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(chatService, never()).updateChat(group);
    }

    @Test
    void allowsNonOwnerToRemoveOnlyTheirselfWhenLeaving() {
        var member = new UserPrincipal("member-1", "member@example.com", "", List.of());

        var response = controller.updateChatParticipants(
                "group-1",
                List.of("owner-1"),
                member);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of("owner-1"), group.getParticipants());
        verify(chatService).updateChat(group);
    }

    @Test
    void rejectsNonOwnerChangingOtherGroupMembers() {
        var member = new UserPrincipal("member-1", "member@example.com", "", List.of());

        var response = controller.updateChatParticipants(
                "group-1",
                List.of("owner-1", "new-member"),
                member);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(chatService, never()).updateChat(group);
    }
}
