package com.dub.site.chat;

// POJO
public class PrivateCreateResult
{
    private final PrivateChatSession chatSession;
    private final ChatMessage createMessage;

    public PrivateCreateResult(PrivateChatSession chatSession, ChatMessage createMessage)
    {
        this.chatSession = chatSession;
        this.createMessage = createMessage;
    }

    public PrivateChatSession getChatSession()
    {
        return chatSession;
    }

    public ChatMessage getCreateMessage()
    {
        return createMessage;
    }
}
