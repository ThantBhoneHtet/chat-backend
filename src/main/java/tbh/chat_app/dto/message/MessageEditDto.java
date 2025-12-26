package tbh.chat_app.dto.message;

public record MessageEditDto (
    String content,
    String attachmentUrl,
    String attachmentName
) {}
