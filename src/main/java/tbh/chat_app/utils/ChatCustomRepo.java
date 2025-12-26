package tbh.chat_app.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;

import tbh.chat_app.model.Chat;

@Repository
public class ChatCustomRepo {

    private final Firestore firestore;
    private final String COLLECTION_NAME = "chats";

   

    public ChatCustomRepo(Firestore firestore) {
        this.firestore = firestore;
    }

    public String getChatIdBetweenParticipants(List<String> participantIds) {

        List<String> sorted = new ArrayList<>(participantIds);
        Collections.sort(sorted);
        String key = String.join("_", sorted);
        String chatId = DigestUtils.sha256Hex(key);

        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
            .whereEqualTo("chatId", chatId)
            .get();
        try {
            QuerySnapshot querySnapshot = future.get();
            if (querySnapshot.isEmpty()) {
                return null;
            } else {
                DocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);
                return documentSnapshot.getId();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public void updateChat(Chat chat) {
        firestore.collection(COLLECTION_NAME).document(chat.getChatId()).set(chat);
    }
 
}
