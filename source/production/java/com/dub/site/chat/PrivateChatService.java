package com.dub.site.chat;

import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Collection;

public interface PrivateChatService
{
    
	@PreAuthorize("authentication.principal.username.equals(#secondUser) and " +
            "hasAuthority('CHAT')")
    PrivateCreateResult createSession(String firstUser, @P("secondUser") String secondUser);
    
	@PreAuthorize("authentication.principal.username.equals(#firstUser) and " +
            "hasAuthority('CHAT')")
    PrivateJoinResult joinSession(@P("firstUser") String firstUser, String secondUser, String user);
    
	@PreAuthorize("authentication.principal.username.equals(#user) and " +
            "hasAuthority('CHAT')")
    ChatMessage leaveSession(@P("session") PrivateChatSession session,
            @P("user") String user, ReasonForLeaving reason);
    
    void removeSession(PrivateChatSession session);
       
    // convenience methods
 	public Collection<PrivateChatSession> getActivePrivateChatSessions();
 	
 	public boolean chatSessionExists(String firstUser, String secondUser);
 	
    public static enum ReasonForLeaving
    {
        NORMAL,

        LOGGED_OUT,

        ERROR
    }
}
