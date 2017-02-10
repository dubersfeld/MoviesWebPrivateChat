package com.dub.site.chat;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
public class DefaultPrivateChatService implements PrivateChatService
{
  
    private static final Logger log = LogManager.getLogger();
    
    private final Map<String,PrivateChatSession> activeSessions = new HashMap<>();
    
    @Inject ObjectMapper objectMapper;

    
    @Override
    public ChatMessage leaveSession(PrivateChatSession session, String user,
                                    ReasonForLeaving reason)
    {
        ChatMessage message = new ChatMessage();
        message.setUser(user);
        message.setTimestamp(Instant.now());
        if(reason == ReasonForLeaving.ERROR)
            message.setType(ChatMessage.Type.ERROR);
        message.setType(ChatMessage.Type.LEFT);
        if(reason == ReasonForLeaving.ERROR)
            message.setContentCode("message.chat.left.chat.error");
        else if(reason == ReasonForLeaving.LOGGED_OUT)
            message.setContentCode("message.chat.logged.out");
        else
            message.setContentCode("message.chat.left.chat.normal");
        message.setContentArguments(user);
       
        return message;
    }

   
	@Override
	public PrivateCreateResult createSession(String firstUser, String secondUser) { 
		ChatMessage message = new ChatMessage();   
		message.setUser(secondUser);
		message.setTimestamp(Instant.now());
		message.setType(ChatMessage.Type.STARTED);
		message.setContentCode("message.chat.started.session");
		message.setContentArguments(secondUser);
	        
		// the chatSession is created by secondUser, not firstUser
	    PrivateChatSession session = new PrivateChatSession(); 
	    session.setFirstUsername(firstUser);
	    session.setSecondUsername(secondUser);
	    session.setCreationMessage(message);

	    synchronized(this) {
	        	String pair = firstUser + "#" + secondUser;
	        	this.activeSessions.put(pair, session);
	    }
	        
	    boolean exists = this
        			.chatSessionExists(firstUser, secondUser);
        
        return new PrivateCreateResult(session, message);
	}


    @PostConstruct
    public void initialize()
    {
        this.objectMapper.addMixInAnnotations(ChatMessage.class,
                ChatMessage.MixInForLogWrite.class);
    }

	@Override
	public PrivateJoinResult joinSession(
								String firstUser, 
								String secondUser, 
								String user) {
		// user == firstUser required
		// check existence
		String pair = firstUser + "#" +  secondUser;
		boolean exists = this.activeSessions.containsKey(pair); 
	    if(!exists)
	        {
	            log.info("Attempt to join non-existent session");
	            return null;
	        }
	    PrivateChatSession session = this.activeSessions.get(pair);
	    session.setFirstUsername(firstUser);
	    	    
	    ChatMessage message = new ChatMessage();
	    message.setUser(user);
        message.setTimestamp(Instant.now());
        message.setType(ChatMessage.Type.JOINED);
        message.setContentCode("message.chat.joined.session");
        message.setContentArguments(user);
   
		return new PrivateJoinResult(session, message);
	}
	
	// convenience methods
	public Collection<PrivateChatSession> getActivePrivateChatSessions() {
		return this.activeSessions.values();
	}

	@Override
	public boolean chatSessionExists(String firstUser, String secondUser) {
		return (activeSessions.containsKey(firstUser + "#" + secondUser)
				|| activeSessions.containsKey(secondUser + "#" + firstUser));
	
	}


	@Override
	public void removeSession(PrivateChatSession session) {
		this.activeSessions.remove(session.getFirstUsername() 
									+ "#" + session.getSecondUsername());
		
	}
		
}
