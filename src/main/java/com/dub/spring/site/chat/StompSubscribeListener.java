package com.dub.spring.site.chat;


import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import com.dub.spring.site.chat.ChatRoomService;


@Component
public class StompSubscribeListener 
	implements ApplicationListener<SessionSubscribeEvent> {
	 
    private static final Logger log = LoggerFactory.getLogger(StompSubscribeListener.class);
 
	@Autowired
    private SimpMessagingTemplate template;
    
    @Autowired 
    private ChatRoomService chatRoomService;
    
    @Autowired 
    private PrivateSessionRegistry privateSessionRegistry;
   
	
    
	@Override
    public void onApplicationEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
 
        String username =  sha.getUser().getName();
        String destination = sha.getDestination();
      
        String time = new SimpleDateFormat("HH:mm").format(new Date());
    	
        ChatMessage connMessage 
			= new ChatMessage(username, "", time, ChatMessage.Code.JOINED);
        
        String[] parts = destination.split("/");
        String chatRoomId = parts[3];
        
        if (parts[1].equals("topic")) {    
        	ChatRoom chatRoom 
				= chatRoomService.getChatRoom(Integer.valueOf(chatRoomId));
            // add new subscriber username
            chatRoom.addUser(sha.getSessionId(), username);

            chatRoom.display();
        	
            log.info("New user has joined: " + username);
                
            template.convertAndSend(destination, connMessage);
        	           	 	
        }
          
        if (parts[1].equals("user") && parts[3].equals("private")) {    
        	 privateSessionRegistry.addSession(sha.getSessionId(), parts[4]);
        	 
        	 String privateUsersStr = privateSessionRegistry.getUsers(sha.getSessionId());
        	 privateSessionRegistry.display();
        	 
        	 String[] privateUsers = privateUsersStr.split("#");
        	 
        	 for (String privateUser : privateUsers) {
        		 String dest = "/user/" + privateUser + "/queue/private/" + privateUsersStr;
        		 System.out.println("JOIN dest " + dest);
        		 template.convertAndSend(dest, connMessage);        
        	 }
        	 
        }
        
    }
}