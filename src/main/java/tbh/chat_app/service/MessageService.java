package tbh.chat_app.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tbh.chat_app.model.Message;
import tbh.chat_app.repository.MessageRepository;
import tbh.chat_app.utils.MessageCustomRepo;
import reactor.core.publisher.Flux;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageCustomRepo messageCustomRepo;

    public Flux<Message> changeChat(String oldId, String newId) {
    return messageRepository.findByChatId(oldId)
        .flatMap(message -> {
            message.setChatId(newId);
            System.out.println("Message"+message);
            return messageRepository.save(message); // persist change
        });
}


    public Message saveMessage(Message message) {
        return messageRepository.save(message).block();
    }

    public void deleteMessage(String id) {
        messageRepository.deleteById(id).block();
    }

    public Message getLatestMessage(String chatId) {
        return messageCustomRepo.findLatestMessage(chatId);
    }

    public Message findById(String id) {
        return messageRepository.findById(id).block();
    }

    public List<Message> fetchMessages(String chatId, String lastMsgId, int size) {
        return messageCustomRepo.fetchMessagesByPages(chatId, size, lastMsgId);
        // return messageRepository.findByChatIdOrderBySentAtAsc(chatId).collectList().block();
    }

    public List<Message> fetchMessagesInChat(String chatId) {
        return messageCustomRepo.fetchMessagesInChat(chatId);
    }

    public List<Message> getUnreadMessagesInChatByReaderId(String chatId, String string) {
        return messageCustomRepo.getUnreadMessagesInChatByReaderId(chatId, string);
    }
}
