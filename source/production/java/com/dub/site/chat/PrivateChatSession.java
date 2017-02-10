package com.dub.site.chat;

import javax.websocket.Session;
import java.util.function.Consumer;

public class PrivateChatSession
{
    private long sessionId;
    private String firstUsername;
    private Session firstUser;
    private String secondUsername;
    private Session secondUser;
    private Consumer<Session> onFirstUserJoin;
    private ChatMessage creationMessage;

    public long getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(long sessionId)
    {
        this.sessionId = sessionId;
    }

    public String getFirstUsername()
    {
        return firstUsername;
    }

    public void setFirstUsername(String firstUsername)
    {
        this.firstUsername = firstUsername;
    }

    public Session getFirstUser()
    {
        return firstUser;
    }

    public void setFirstUser(Session firstUser)
    {
        this.firstUser = firstUser;
        this.firstUser = firstUser;
        if(this.onFirstUserJoin != null)
            this.onFirstUserJoin.accept(firstUser);
    }

    public String getSecondUsername()
    {
        return secondUsername;
    }

    public void setSecondUsername(String secondUserUsername)
    {
        this.secondUsername = secondUserUsername;
    }

    public Session getSecondUser()
    {
        return secondUser;
    }

    public void setSecondUser(Session secondUser)
    {
        this.secondUser = secondUser;
        if(this.onFirstUserJoin != null)
            this.onFirstUserJoin.accept(secondUser);
    }

    public void setOnFirstUserJoin(Consumer<Session> onFirstUserJoin)
    {
        this.onFirstUserJoin = onFirstUserJoin;
    }

    public ChatMessage getCreationMessage()
    {
        return creationMessage;
    }

    public void setCreationMessage(ChatMessage creationMessage)
    {
        this.creationMessage = creationMessage;
    }
   
}
