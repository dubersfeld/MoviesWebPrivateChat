package com.dub.site.chat;

// POJO
public class PrivateJoinResult
{
    private final PrivateChatSession PrivateChatSession;
    private final ChatMessage joinMessage;

    public PrivateJoinResult(PrivateChatSession PrivateChatSession, 
    											ChatMessage joinMessage)
    {
        this.PrivateChatSession = PrivateChatSession;
        this.joinMessage = joinMessage;
    }

    public PrivateChatSession getPrivateChatSession()
    {
        return PrivateChatSession;
    }

    public ChatMessage getJoinMessage()
    {
        return joinMessage;
    }
}
