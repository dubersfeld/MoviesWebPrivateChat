package com.dub.spring.site.chat;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.dub.spring.exceptions.ChatRoomNotFoundException;

@Service
public class DefaultChatRoomService implements ChatRoomService
{
	
	private Map<Integer, ChatRoom> chatRooms = new HashMap<>();
	
	@PostConstruct
	public void init()
	{
		
		ChatRoom chatRoom = new ChatRoom();
		chatRoom.setName("Woody Allen");
		chatRoom.setChatRoomId(1);
		this.chatRooms.put(chatRoom.getChatRoomId(), chatRoom);
		
		ChatRoom chatRoom2 = new ChatRoom();
		chatRoom2.setName("Brad Pitt");
		chatRoom2.setChatRoomId(2);
		this.chatRooms.put(chatRoom2.getChatRoomId(), chatRoom2);
	}

	@Override
	public Map<Integer, ChatRoom> getChatRooms() {
		return this.chatRooms;
	}


	@Override
	public ChatRoom getChatRoom(int id) {
		return this.chatRooms.get(id);
	}

	@Override
	public boolean isConnected(String username, int chatRoomId) {
			
		return this.getChatRoom(chatRoomId).getConnectedUsers()
								.containsValue(username);
	}

	@Override
	public Collection<String> getConnectedUsers(int chatRoomId) {
		return this.getChatRoom(chatRoomId).getConnectedUsers().values();
	}


	@Override
	public String getChatRoomName(int chatRoomId) {
		if (this.chatRooms.containsKey(chatRoomId)) {
			return this.chatRooms.get(chatRoomId).getName();
		} else {
			throw new ChatRoomNotFoundException();
		}
	}

}
