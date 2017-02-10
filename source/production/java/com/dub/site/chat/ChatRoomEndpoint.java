package com.dub.site.chat;

import com.dub.entities.UserPrincipal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.web.socket.server.standard.SpringConfigurator;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.EncodeException;
import javax.websocket.HandshakeResponse;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.PongMessage;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;


@ServerEndpoint(value = "/chatRoomEndpoint/{chatRoomId}",
        encoders = ChatMessageCodec.class,
        decoders = ChatMessageCodec.class,
        configurator = ChatRoomEndpoint.EndpointConfigurator.class)
public class ChatRoomEndpoint
{
    private static final Logger log = LogManager.getLogger();
    private static final byte[] pongData =
            "This is PONG country.".getBytes(StandardCharsets.UTF_8);

    private final Consumer<SessionDestroyedEvent> callback =
            this::httpSessionRemoved;

    private boolean closed = false;
    private Session wsSession;
    private HttpSession httpSession;
    private long chatRoomId;
    private SecurityContext securityContext;
    private UserPrincipal principal;
    private ScheduledFuture<?> pingFuture;
    private Locale locale;
    
    @Inject SessionDestroyedListener sessionDestroyedListener;
    @Inject ChatRoomService chatRoomService; 
    @Inject MessageSource messageSource;
    @Inject TaskScheduler taskScheduler;
    
    @OnOpen
    public void onOpen(Session session, @PathParam("chatRoomId") long chatRoomId)
    {
        this.httpSession = EndpointConfigurator.getExposedSession(session);
        this.securityContext =
        				EndpointConfigurator.getExposedSecurityContext(session);
        this.principal = (UserPrincipal)this.securityContext
        						.getAuthentication().getPrincipal();
        
        this.locale = (Locale)this.httpSession.getAttribute("locale");
        
        this.doSecured(() -> {	
        	try {
        	
        		// check presence of user
        		boolean present = chatRoomService
        				.isConnected(this.principal.getUsername(), chatRoomId);
        		
        		if (present) {
        			session.close(new CloseReason(
        					CloseReason.CloseCodes.UNEXPECTED_CONDITION,
        					this.messageSource.getMessage(
        							"error.chat.already.connected", 
        							new Object[] {chatRoomService.getChatRoomName(chatRoomId)}, 
        							this.locale)
    				));      
        			return;
        		}
        		
        		MultiJoinResult result = 
        				chatRoomService.joinChatRoom(
        						chatRoomId,
        						this.principal.getUsername()
        				);
        		if(result == null) {
        			log.warn("Attempted to join non-existent chat room {}.",
        					chatRoomId);
        			session.close(new CloseReason(
        					CloseReason.CloseCodes.UNEXPECTED_CONDITION,
        					this.messageSource.getMessage(
        							"error.chat.no.session", null, this.locale)
    				));
        			return;
        		} 
        	   		
        		this.chatRoomId = chatRoomId;               		
        		chatRoomService.addSession(
        								this.principal.getUsername(), 
        								session, 
        								chatRoomId);
        		chatRoomService.addLocale(
        								this.principal.getUsername(), 
        								this.locale, 
        								chatRoomId);
        				
        		for (String username : chatRoomService.
        							getConnectedUsers(chatRoomId)) {
        			Session s = chatRoomService
        							.getSession(username, chatRoomId);
        			Locale loc = chatRoomService
        							.getLocale(username, chatRoomId);
        			        			
        			s.getBasicRemote().sendObject(
							this.cloneAndLocalize(
											result.getJoinMessage(), loc
					));
        		}
        	        
        		this.wsSession = session;      	
        	} catch(IOException | EncodeException e) {
        		log.info("Exception caught in onOpen: " + e);
        	}
        });
    }

    
    @OnMessage
    public void onMessage(Session session, ChatMessage message)
    {
        this.doSecured(() -> {
            if(this.closed)
            {
                log.warn("Chat message received after connection closed.");
                return;
            }

            message.setUser(this.principal.getUsername());
   
            if (message.getType() == ChatMessage.Type.PRIVATE_CHAT_REQUEST) {
                String targetUsername = message.getUserContent();
                // first check if targetUsername is connected
                if(!chatRoomService.isConnected(targetUsername, this.chatRoomId)) {
                	return;// do nothing
                }
                Session targetSession = chatRoomService
                						.getSession(targetUsername, this.chatRoomId);
                
                try {
                	targetSession.getBasicRemote().sendObject(message);
                } catch (IOException | EncodeException e) {
                	   log.info("Exception caught " + e);
                }
            } else if (message.getType() == ChatMessage.Type.PRIVATE_CHAT_ACCEPT) {
            	String firstUsername = message.getUserContent();
            	// check if firstUser is connected
            	if(!chatRoomService.isConnected(firstUsername, this.chatRoomId)) {
            		return;// do nothing
            	}
            	Session firstSession = chatRoomService
            					.getSession(firstUsername, this.chatRoomId);
            	try {
            		firstSession.getBasicRemote().sendObject(message);
            	} catch(IOException | EncodeException e) {
            		log.info("Exception caught " + e);
            	} 
                     
            } else {
            
            	try { 
            		for (Session s : chatRoomService.getConnectedSessions(this.chatRoomId)) {
            		s.getBasicRemote().sendObject(message);
            		}
            	
            	} catch(IOException | EncodeException e) {
            		this.onError(e);
            		e.printStackTrace();
            		log.info("Exception caught in onMessage " + e);
            	}
            }
           
        });
       
    }

    @OnClose
    public void onClose(CloseReason reason)
    {
        this.doSecured(() -> {
            
        	if(reason.getCloseCode() != CloseReason.CloseCodes.NORMAL_CLOSURE)
            {
                log.warn("Abnormal closure {} for reason [{}].",
                        reason.getCloseCode(), reason.getReasonPhrase());
            }

            synchronized(this)
            {
                if(this.closed) {                  
                    return;
                }
               
                this.close(ChatRoomService.ReasonForLeaving.NORMAL, null);
             
            }
      
        });
    }

    @OnError
    public void onError(Throwable e)
    {
        this.doSecured(() -> {
            log.warn("Error received in WebSocket session.", e);

            synchronized(this)
            {
                if(this.closed)
                    return;
                this.close(ChatRoomService.ReasonForLeaving.ERROR,
                        "error.chat.closed.exception");
            }
        });
        
    }

    private void sendPing()
    {	
        if(!this.wsSession.isOpen())
            return;
        log.debug("Sending ping to WebSocket client.");
        try
        {
            this.wsSession.getBasicRemote()
                    .sendPing(ByteBuffer.wrap(ChatRoomEndpoint.pongData));
        }
        catch(IOException e)
        {
            log.warn("Failed to send ping message to WebSocket client.", e);
        }
    }

    @OnMessage
    public void onPong(PongMessage message)
    {
        ByteBuffer data = message.getApplicationData();
        if(!Arrays.equals(ChatRoomEndpoint.pongData, data.array()))
            log.warn("Received pong message with incorrect payload.");
        else
            log.debug("Received good pong message.");
    }

    @PostConstruct
    public void initialize()
    {
        this.sessionDestroyedListener.registerOnRemoveCallback(this.callback);

        this.pingFuture = this.taskScheduler.scheduleWithFixedDelay(
                this::sendPing,
                new Date(System.currentTimeMillis() + 250_000L),
                25_000L
        );       
    }
    
    public void httpSessionRemoved(SessionDestroyedEvent event)
    {
        String sessionId = event.getId();
        if(sessionId.equals(this.httpSession.getId()))
        {
            synchronized(this)
            {
                if(this.closed)
                    return;
                log.info("Chat session ended abruptly by {} logging out.",
                        this.principal.getUsername());
                this.close(ChatRoomService.ReasonForLeaving.LOGGED_OUT, null);
            }
        }
    }

    private void close(ChatRoomService.ReasonForLeaving reason, 
    												String unexpected)
    {   
        if(!this.pingFuture.isCancelled())
            this.pingFuture.cancel(true);
          
       	this.sessionDestroyedListener.deregisterOnRemoveCallback(this.callback);
    	  	
        ChatMessage message = null;
        
        if (chatRoomService.getChatRoom(this.chatRoomId) != null) { 
        	
        	message = chatRoomService.leaveChatRoom(								
        									this.principal.getUsername(), 
        									reason
        	);
         
        	if(message != null) {
        		CloseReason.CloseCode closeCode;
        		String reasonCode;
        		if(reason == ChatRoomService.ReasonForLeaving.ERROR) {
        			closeCode = CloseReason.CloseCodes.UNEXPECTED_CONDITION;
        			reasonCode = unexpected;
        		} else {
        			closeCode = CloseReason.CloseCodes.NORMAL_CLOSURE;
        			reasonCode = "message.chat.ended";
        		}
        		
        		synchronized(this.wsSession) {
        		
        			chatRoomService.removeSession(
        								this.principal.getUsername(), 
        								this.chatRoomId);
        			
        			chatRoomService.removeLocale(
							this.principal.getUsername(), 
							this.chatRoomId);
					
        			try {
        				if(this.wsSession.isOpen()) {    					                           				
        					this.wsSession.close(new CloseReason(
                                  closeCode, this.messageSource.getMessage(
                                  reasonCode, null, this.locale)
                          ));
        				}// if
        				
        			} catch(IOException e) {
        				log.info("Exception caught: " + e);
        			} catch(IllegalStateException e) {
        		     	log.info("IllegalStateException on close: " + e);
        			} 
            	} // synchronized
        		      		
        		for (String username : chatRoomService.getConnectedUsers(this.chatRoomId)) {
        	 	
        			Session s = chatRoomService.getSession(username, chatRoomId);
        			Locale loc = chatRoomService.getLocale(username, chatRoomId);
        	 		try {		
        				s.getBasicRemote()
                            	.sendObject(this.cloneAndLocalize(
                                    message, loc
                       			));	
        			} catch(IOException | EncodeException e) {
        			
        				log.info("Exception caught in close for loop" );
        			}
        		}
        	} 
        }
        this.closed = true;
    }

    private ChatMessage cloneAndLocalize(ChatMessage message, Locale locale)
    {    	
        message = message.clone();
      	
        message.setLocalizedContent(this.messageSource.getMessage(
                message.getContentCode(), message.getContentArguments(), locale
        ));
    	
        return message;
    }

    private void doSecured(SecuredAction secureAction)
    {   
    	SecurityContextHolder.setContext(this.securityContext);
        try
        {
            secureAction.execute();
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }
    }

    @FunctionalInterface
    private static interface SecuredAction
    {
        void execute();
    }

    public static class EndpointConfigurator extends SpringConfigurator
    {
        private static final String HTTP_SESSION_KEY = "com.dub.ws.http.session";
        private static final String SECURITY_CONTEXT_KEY = "com.dub.ws.security.context";
        private static final String LOCALE_KEY = "com.dub.ws.user.locale";

        @Override
        public void modifyHandshake(ServerEndpointConfig config,
                                    HandshakeRequest request,
                                    HandshakeResponse response)
        {
            log.entry();
            super.modifyHandshake(config, request, response);
            
            HttpSession httpSession = (HttpSession)request.getHttpSession();
            config.getUserProperties().put(HTTP_SESSION_KEY, httpSession);
            config.getUserProperties().put(SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());
            config.getUserProperties().put(LOCALE_KEY,
                    LocaleContextHolder.getLocale());

            log.exit();
        }

        private static HttpSession getExposedSession(Session session)
        {
            return (HttpSession)session.getUserProperties().get(HTTP_SESSION_KEY);
        }

        private static SecurityContext getExposedSecurityContext(Session session)
        {
            return (SecurityContext)session.getUserProperties().get(SECURITY_CONTEXT_KEY);
        } 
    }
}
