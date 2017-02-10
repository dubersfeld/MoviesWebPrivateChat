package com.dub.site.chat;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.dub.config.annotation.WebController;

import javax.servlet.http.HttpServletResponse;

import java.util.Map;


@WebController
@RequestMapping("privateChat")
public class PrivateChatController
{  
	
	@RequestMapping(value = "join", method = RequestMethod.POST)
	public String join(
				@ModelAttribute("privateForm") PrivateForm form,
	    		Map<String, Object> model, HttpServletResponse response)
	{
	    this.setNoCacheHeaders(response);
	    model.put("firstUser", form.getFirstUser());
	    model.put("secondUser", form.getSecondUser());
	    return "chat/privateChat";
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
