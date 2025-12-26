package tbh.chat_app.repository;

import org.springframework.stereotype.Repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import tbh.chat_app.model.User;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends FirestoreReactiveRepository<User> {

    Mono<Boolean> existsByEmail(String email);
    Mono<User> findByEmail(String email);
}
