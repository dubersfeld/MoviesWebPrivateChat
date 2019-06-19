package com.dub.spring.site.chat;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.dub.spring.exceptions.ChatRoomNotFoundException;

@Controller
@RequestMapping("/chatRoom")
public class ChatRoomController {
	
    private static final Logger log = LogManager.getLogger();
    
    @Value("${myapp.chatroom.url}")
    String chatRoomUrl;
    
    @Value("${myapp.privateChatroom.url}")
    String privateChatRoomUrl;
    
    @Autowired
    private SimpMessagingTemplate template;
    
    @Autowired 
    private ChatRoomService chatRoomService;
    
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public String list(Map<String, Object> model, HttpServletRequest request)
    {
    	
    	Map<Integer, ChatRoom> chatRooms = this.chatRoomService.getChatRooms();
    	
     	model.put("chatrooms", chatRooms);
         
     	HttpSession session = request.getSession();
     	session.setAttribute("locale", LocaleContextHolder.getLocale());
     	
        return "chat/chatRoomList";
    }

    
    @RequestMapping(value = "/join/{chatRoomId}", method = RequestMethod.GET)
    public String joinChatGet(Map<String, Object> model, HttpServletResponse response,
                           @PathVariable("chatRoomId") int chatRoomId)
    {   
    	boolean allowed = false;
     
    	Collection<? extends GrantedAuthority> authorities 
    					= SecurityContextHolder.getContext().getAuthentication().getAuthorities();
  
    	List<String> authStrs = authorities.stream()
    									.map(s -> s.getAuthority())
    									.collect(Collectors.toList());
    	   		
    	allowed = authStrs.contains("CHAT");
    
    	if (!allowed) {
    		return "error";
    	}
    	
    	try {
    		this.setNoCacheHeaders(response);
    		String username = SecurityContextHolder.getContext().getAuthentication().getName();
    		model.put("chatRoomId", chatRoomId);
    		model.put("chatRoomName", chatRoomService.getChatRoomName(chatRoomId));
     		model.put("username", username);
        	model.put("chatRoomUrl", chatRoomUrl);
        	model.put("privateChatRoomUrl", privateChatRoomUrl);
        
    		return "chat/chatRoom";
    	} catch (ChatRoomNotFoundException e) {
    		log.info("Chatroom non existing");
    		return "error";
    	}
    }
     
    @MessageMapping("/chat/{chatRoomId}") // like a request mapping with @DestinationVariable instead of @PathVariable
	@SendTo("/topic/messages/{chatRoomId}") // destination URL
	public ChatMessage send(@DestinationVariable("chatRoomId") int chatRoomId, ChatMessage message, 
											StompHeaderAccessor sha) throws Exception {
	    			
    	return message;  	
	}
    
    
    @MessageMapping("/service/{chatRoomId}") // like a request mapping with @DestinationVariable instead of @PathVariable
	public void serve(@DestinationVariable("chatRoomId") int chatRoomId, ChatMessage message,
			StompHeaderAccessor sha) throws Exception {
 
    	switch (message.getCode()) {
    	case PRIVATE_CHAT_REQUEST:
    		// first check if targetUsername is connected
    		if(!chatRoomService.isConnected(message.getText(), chatRoomId)) {
    			// reply to firstUser with a service message "NOT_CONNECTED"
    			ChatMessage reply = new ChatMessage("Service", message.getText(), "", ChatMessage.Code.NOT_CONNECTED);
    			String dest = "/user/" + message.getFrom() + "/queue/messages/" + chatRoomId;  		
    			template.convertAndSend(dest, reply);
    		} else {
    			ChatMessage msg 
    			= new ChatMessage("Service", message.getText(), "", ChatMessage.Code.CONNECTED);
    			String dest = "/user/" + message.getFrom() + "/queue/messages/" + chatRoomId;
    			// if target connected send service message to target only
    			template.convertAndSend(dest, msg);
    			dest = "/user/" + message.getText() + "/queue/messages/" + chatRoomId;
    			template.convertAndSend(dest, message);
    			  
    		}
    		break;
    	case PRIVATE_CHAT_REJECT:
    		// send reject notification to otherUser
    	  	ChatMessage msg 
			= new ChatMessage("Service", message.getFrom(), "", ChatMessage.Code.PRIVATE_CHAT_REJECT);
    	 	String dest = "/user/" + message.getText() + "/queue/messages/" + chatRoomId;
    	   	template.convertAndSend(dest, msg);
    		break;
    	case PRIVATE_CHAT_ACCEPT:
    		msg = new ChatMessage("Service", message.getFrom(), "", ChatMessage.Code.PRIVATE_CHAT_ACCEPT);
    	 	dest = "/user/" + message.getText() + "/queue/messages/" + chatRoomId;
    	   	template.convertAndSend(dest, msg);
    		break;
    	default:
    			
    	}
    } 
	
    
   
    private void setNoCacheHeaders(HttpServletResponse response)
    {
        response.setHeader("Expires", "Thu, 1 Jan 1970 12:00:00 GMT");
        response.setHeader("Cache-Control","max-age=0, must-revalidate, no-cache");
        response.setHeader("Pragma", "no-cache");
    }
   
}
