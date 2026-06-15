package tbh.chat_app.dto.message;


/**
 * @param senderId the ID of the user sending the message
 * @param content the content of the message
 * @param attachmentUrl the URL of the attachment (if any)
 * @param attachmentName the name of the attachment (if any)
 * @param replyToMessageId the ID of the message this message is replying to (if any)
 */
public record MessageRequest(
    String senderId, 
    String content, 
    String attachmentUrl,
    String attachmentName,
    String replyToMessageId
) {}