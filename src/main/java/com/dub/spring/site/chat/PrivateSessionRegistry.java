package com.dub.spring.site.chat;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class PrivateSessionRegistry {
	
	private Map<String, String> privateSessions;
	
	public PrivateSessionRegistry() {
		privateSessions = new HashMap<>();
	}

	public Map<String, String> getPrivateSessions() {
		return privateSessions;
	}

	public void setPrivateSessions(Map<String, String> privateSessions) {
		this.privateSessions = privateSessions;
	}
	
	public void addSession(String SessionId, String privateUsers) {
		privateSessions.put(SessionId, privateUsers);
	}
	
	public void removeSession(String SessionId) {
		privateSessions.remove(SessionId);
	}
	
	public String getUsers(String sessionId) {
		return privateSessions.get(sessionId);
	}
	
	// for debug only
	public void display() {
		for (String sessionId : privateSessions.keySet()) {
			System.out.println(sessionId + ": " 
						+ privateSessions.get(sessionId));
		}
		
	}

}
