package com.dub.spring.site.chat;
  
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


@Controller
@RequestMapping("/privateChat")
public class PrivateChatController { 
	 
    @Autowired
    private SimpMessagingTemplate template;
    
	@Value("${myapp.chatroom.url}")
    String chatRoomUrl;
	
	@RequestMapping(value = "/join", method = RequestMethod.POST)
	public String join(
			@ModelAttribute("privateForm") PrivateForm form,  
	    		Map<String, Object> model, HttpServletResponse response)
	{
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
    	String otherUser = username.equals(form.getFirstUser()) ? form.getSecondUser() : form.getFirstUser();
	    this.setNoCacheHeaders(response);
		model.put("username", username);
		model.put("otherUser", otherUser);
	    model.put("firstUser", form.getFirstUser());
	    model.put("secondUser", form.getSecondUser());
	    model.put("chatRoomUrl", chatRoomUrl);
	    return "chat/privateChat";
	}
	
	@MessageMapping("/private/{privateUsers}") // like a request mapping with @DestinationVariable instead of @PathVariable
	public void servePrivate(@DestinationVariable("privateUsers") String privateUsers, ChatMessage message,
				StompHeaderAccessor sha) throws Exception {
		
		// send to privateUsers only
	    String[] users = privateUsers.split("#");
	    System.out.println(users[0] + " " + users[1]);
	    String dest;
	    for (int i = 0; i < 2; i++) {
	    	dest = "/user/" + users[i] + "/queue/private/" + privateUsers;
	  	    template.convertAndSend(dest, message);	
	    }	
	}
	
	
		
    
    private void setNoCacheHeaders(HttpServletResponse response)
    {
        response.setHeader("Expires", "Thu, 1 Jan 1970 12:00:00 GMT");
        response.setHeader("Cache-Control","max-age=0, must-revalidate, no-cache");
        response.setHeader("Pragma", "no-cache");
    }
    
    public static class PrivateForm {
    	private String firstUser;
    	private String secondUser;
    	
		public String getFirstUser() {
			return firstUser;
		}
		public void setFirstUser(String firstUser) {
			this.firstUser = firstUser;
		}
		public String getSecondUser() {
			return secondUser;
		}
		public void setSecondUser(String secondUser) {
			this.secondUser = secondUser;
		} 	
    }
   
}
