package tbh.chat_app.model;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;

import lombok.Data;
import java.util.List;

@Data
@Document(collectionName = "messages")
public class Message {
    @DocumentId
    private String messageId;
    
    private String chatId;
    private String senderId; // user reference
    // private List<String> recipientIds; // user references
    private String content;
    // private List<String> attachments; // URLs
    private  String attachment;
    private String attachmentName;
    private Timestamp sentAt;
    private Timestamp editedAt;
    private List<String> readBy; // user references
    private String replyToMessageId; // message reference
}