package tbh.chat_app.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.Query.Direction;
import com.google.cloud.firestore.QuerySnapshot;


import tbh.chat_app.model.Message;
import reactor.core.publisher.Mono;

@Repository
public class MessageCustomRepo {
    private final Firestore firestore;
    private final String COLLECTION_NAME = "messages";

    public MessageCustomRepo(Firestore firestore) {
        this.firestore = firestore;
    }

    public Message findLatestMessage(String chatId) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
            .whereEqualTo("chatId", chatId)
            .orderBy("sentAt", Direction.DESCENDING)
            .limit(1)
            .get();

        try {
            QuerySnapshot querySnapshot = future.get();
            if (querySnapshot.isEmpty()) {
                return null; // no messages in this chat
            }

            QueryDocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);
            return documentSnapshot.toObject(Message.class);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching latest message", e);
        }
    }


    public List<Message> getUnreadMessagesInChatByReaderId(String chatId, String readerId) {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
            .whereEqualTo("chatId", chatId)
            .get();

        try{
            List<QueryDocumentSnapshot> docs = future.get().getDocuments();
            List<Message> unreadMessages = docs.stream()
            .map(doc -> doc.toObject(Message.class))
            .filter(msg -> !msg.getReadBy().contains(readerId))
            .toList();
            return unreadMessages;
        } catch(Exception e) {
            throw new RuntimeException("Error fetching unread messages", e);
        }
    }

    public List<Message> fetchMessagesInChat(String chatId) {
        try {
            CollectionReference messagesRef = firestore.collection(COLLECTION_NAME);

            Query query = messagesRef
                    .whereEqualTo("chatId", chatId);

            ApiFuture<QuerySnapshot> future = query.get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            List<Message> messages = new ArrayList<>();
            for (QueryDocumentSnapshot doc : documents) {
                Message message = doc.toObject(Message.class);
                message.setMessageId(doc.getId()); // keep Firestore doc ID if needed
                messages.add(message);
            }

            return messages;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching messages", e);
        }
    }

    public List<Message> fetchMessagesByPages(String chatId, int size, String lastMsgId) {
        try {
            CollectionReference messagesRef = firestore.collection(COLLECTION_NAME);

            Query query = messagesRef
                    .whereEqualTo("chatId", chatId)
                    .orderBy("sentAt", Direction.DESCENDING)
                    .limit(size);

            // If we have a last message ID, use it for pagination
            if (lastMsgId != null && !lastMsgId.isEmpty()) {
                DocumentSnapshot lastDoc = messagesRef.document(lastMsgId).get().get();
                if (lastDoc.exists()) {
                    query = query.startAfter(lastDoc);
                }
            }

            ApiFuture<QuerySnapshot> future = query.get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            List<Message> messages = new ArrayList<>();
            for (QueryDocumentSnapshot doc : documents) {
                Message message = doc.toObject(Message.class);
                message.setMessageId(doc.getId()); // keep Firestore doc ID if needed
                messages.add(message);
            }

            return messages;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching messages", e);
        }
    }

    public List<Message> outputMessages(Query query) throws InterruptedException, ExecutionException {
        ApiFuture<QuerySnapshot> future = query.get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            List<Message> messages = new ArrayList<>();
            for (QueryDocumentSnapshot doc : documents) {
                Message message = doc.toObject(Message.class);
                message.setMessageId(doc.getId()); // keep Firestore doc ID if needed
                messages.add(message);
            }
            return messages;
    }

}
