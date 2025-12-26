package tbh.chat_app.repository;

import org.springframework.stereotype.Repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;

import tbh.chat_app.model.Message;
import reactor.core.publisher.Flux;
import java.util.List;


@Repository
public interface MessageRepository extends FirestoreReactiveRepository<Message> {
    Flux<Message> findByChatIdOrderBySentAtAsc(String chatId);
    Flux<Message> findByChatId(String chatId);
} 
