package com.dub.spring.site.chat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
//import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
public class StompDisconnectListener implements ApplicationListener<SessionDisconnectEvent> {
	 
	private static final Logger log = LoggerFactory.getLogger(StompDisconnectListener.class);
	 
    @Autowired 
    private PrivateSessionRegistry privateSessionRegistry;
	
	@Autowired
	ChatRoomService chatRoomService;
	
	@Autowired
    private SimpMessagingTemplate template;
	 
	@Override
	public void onApplicationEvent(SessionDisconnectEvent event) {
		
		/**
		 * All public sessions are registered to ChatRoom.connectedUsers
		 * All private sessions are registered to PrivateSessionRegistry
		 * */
		try {
			StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
			 	
			String time = new SimpleDateFormat("HH:mm").format(new Date());
		
			String sessionId = sha.getSessionId();
		
			for (ChatRoom chatRoom : chatRoomService.getChatRooms().values()) {// for each chat group
	        	Map<String, String> connectedUsers = chatRoom.getConnectedUsers();
	        	 		 
	        	// send disconnect notification to all chat group subscribers 
	        	if (connectedUsers.containsKey(sessionId)) {
	        		String username = connectedUsers.get(sessionId);
	        		ChatMessage discMessage 
	        		 			= new ChatMessage(username, "", time, ChatMessage.Code.LEFT);
	        	       
	        		// remove subscriber username
	        		connectedUsers.remove(sessionId);
	        		 
	        		template.convertAndSend("/topic/messages/" + chatRoom.getChatRoomId(), discMessage);   	 
	        	} 
	        }
	    
			if (privateSessionRegistry.getPrivateSessions().containsKey(sessionId)) {
			
				privateSessionRegistry.display();
				
				String privateUsersStr =  privateSessionRegistry.getUsers(sessionId);
				String[] privateUsers 
					= privateSessionRegistry.getUsers(sessionId).split("#");	
				String leftUser = sha.getUser().getName();
				for (String username : privateUsers) {
					ChatMessage discMessage 
		 			= new ChatMessage(leftUser, "", time, ChatMessage.Code.LEFT);
					String dest = "/user/" + username + "/queue/private/" + privateUsersStr;
	 
					template.convertAndSend(dest, discMessage);
					privateSessionRegistry.removeSession(sessionId);
					privateSessionRegistry.display();
				}
			
			}
	    
		} catch (Exception e) {
			  System.out.println("Exception caught: " + e);	 
		} 
	    
	}
}