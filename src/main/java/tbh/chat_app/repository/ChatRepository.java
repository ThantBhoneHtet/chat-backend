package tbh.chat_app.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;

import tbh.chat_app.model.Chat;
import reactor.core.publisher.Flux;

public interface ChatRepository extends FirestoreReactiveRepository<Chat> {
    
    Flux<Chat> findByParticipantsContaining(String userId);
    
} 