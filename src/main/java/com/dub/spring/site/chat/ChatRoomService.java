package com.dub.spring.site.chat;

import java.util.Collection;
import java.util.Map;

public interface ChatRoomService {

	Map<Integer, ChatRoom> getChatRooms();
	ChatRoom getChatRoom(int chatRoomId);
	String getChatRoomName(int chatRoomId);
	 
	boolean isConnected(String username, int chatRoomId);
	  
	Collection<String> getConnectedUsers(int chatRoomId);
	 

    public static enum ReasonForLeaving
    {
        NORMAL,

        LOGGED_OUT,

        ERROR
    }
}
