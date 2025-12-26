package tbh.chat_app.dto.message;

public record MessageRequest(
    String senderId, 
    String content, 
    String attachmentUrl,
    String attachmentName,
    String replyToMessageId
) {}