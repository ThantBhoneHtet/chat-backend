package tbh.chat_app.model;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Document(collectionName = "chats")
public class Chat {
    @DocumentId
    private String chatId;
    
    private List<String> participants; // user references
    private String lastMessage;
    private Timestamp lastMessageTime;
    private Map<String, Integer> unreadCounts; // user references, count
    private Timestamp createdAt;
    private String type; // "group" | "direct" | "global"
    private String groupName;
    private String owner; // for gp chat
    private String gpImageUrl;
}