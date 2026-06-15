package tbh.chat_app.dto.message;

import org.springframework.web.multipart.MultipartFile;

/**
 * @param senderId the ID of the user sending the voice message
 * @param voiceMessage the voice message file
 * @param replyToMessageId the ID of the message this voice message is replying to (if any)
 */
public record VoiceMessageDto(
    String senderId,
    MultipartFile voiceMessage,
    String replyToMessageId
) {}
