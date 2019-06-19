package com.dub.spring.site.chat;

import java.util.HashMap;
import java.util.Map;


public class ChatRoom {
	
	private int chatRoomId;
	private String name;
		
	// key: sessionId
	// value username
	private Map<String, String> connectedUsers = new HashMap<>();
	
	public int getChatRoomId() {
		return chatRoomId;
	}
	public void setChatRoomId(int chatRoomId) {
		this.chatRoomId = chatRoomId;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
		
	public Map<String, String> getConnectedUsers() {
		return connectedUsers;
	}
	public void setConnectedUsers(Map<String, String> connectedUsers) {
		this.connectedUsers = connectedUsers;
	}
	
	public void addUser(String sessionId, String username) {
		connectedUsers.put(sessionId, username);
	}
	
	public void removeUserBySessionId(String sessionId) {
		connectedUsers.remove(sessionId);
	} 
	
	// for debugging only
	public void display() {
		for (String key : connectedUsers.keySet()) {
			System.out.println(key + ": " + connectedUsers.get(key));
		}
	} 
  
}
