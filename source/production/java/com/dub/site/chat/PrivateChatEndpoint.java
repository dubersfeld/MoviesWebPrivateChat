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

/**
 * The actual ChatSession is created by second user accept. 
 * Then the first user joins.
 * 
 */

@ServerEndpoint(value = "/privateChatEndpoint/{firstUser}/{secondUser}",
        encoders = ChatMessageCodec.class,
        decoders = ChatMessageCodec.class,
        configurator = PrivateChatEndpoint.EndpointConfigurator.class)
public class PrivateChatEndpoint
{
    private static final Logger log = LogManager.getLogger();
    private static final byte[] pongData =
            "This is PONG country.".getBytes(StandardCharsets.UTF_8);

    private final Consumer<SessionDestroyedEvent> callback =
            this::httpSessionRemoved;

    private boolean closed = false;
    private Session wsSession;
    private Session otherWsSession;
    private HttpSession httpSession;
    private PrivateChatSession chatSession = null;
    private SecurityContext securityContext;
    private UserPrincipal principal;
    private ScheduledFuture<?> pingFuture;
    private Locale locale;
    private Locale otherLocale;

    @Inject SessionDestroyedListener sessionDestroyedListener;
    @Inject PrivateChatService chatService;
    @Inject MessageSource messageSource;
    @Inject TaskScheduler taskScheduler;

    @OnOpen
    public void onOpen(Session session, 
    		@PathParam("firstUser") String firstUser, 
    		@PathParam("secondUser") String secondUser)
    {
        
        this.httpSession = EndpointConfigurator.getExposedSession(session);
        this.securityContext =
                EndpointConfigurator.getExposedSecurityContext(session);
        this.principal = (UserPrincipal)this.securityContext
                .getAuthentication().getPrincipal();
        this.locale = EndpointConfigurator.getExposedLocale(session);

        this.doSecured(() -> {
            try {
                if(this.principal == null) {
                    log.warn("Unauthorized attempt to access chat server.");
                    session.close(new CloseReason(
                            CloseReason.CloseCodes.VIOLATED_POLICY,
                            this.messageSource.getMessage(
                                    "error.chat.not.logged.in", null, this.locale)
                    ));
                    return;
                }
                boolean exists = this.chatService
                			.chatSessionExists(firstUser, secondUser);
                
                if(!exists) {
                	// create with secondUser
                	// secondUser connects first
                	// no chat session yet, create it
                	PrivateCreateResult result 
                		= this.chatService.createSession(firstUser, secondUser);
                	
                    this.chatSession = result.getChatSession();
                    this.chatSession.setSecondUser(session);
                    this.chatSession.setOnFirstUserJoin(
                            s -> this.otherWsSession = s
                    );
                    
                    session.getBasicRemote().sendObject(this.cloneAndLocalize(
                            result.getCreateMessage(), this.locale
                    ));
            
                    exists = this.chatService
                			.chatSessionExists(firstUser, secondUser);                  
                } else {
                	// join with firstUser
                    PrivateJoinResult result = this.chatService.joinSession(firstUser, secondUser,
                            this.principal.getUsername());
                    if(result == null) {
                        log.warn("Attempted to join non-existent chat session.");
                        
                        session.close(new CloseReason(
                                CloseReason.CloseCodes.UNEXPECTED_CONDITION,
                                this.messageSource.getMessage(
                                        "error.chat.no.session", null, this.locale)
                        ));
                        return;
                    }// if
                    
                    this.chatSession = result.getPrivateChatSession();
                    
                    this.chatSession.setFirstUser(session);
                    this.otherWsSession = this.chatSession.getSecondUser();
                    
                    this.otherLocale = EndpointConfigurator
                            .getExposedLocale(this.otherWsSession);

                    session.getBasicRemote().sendObject(this.cloneAndLocalize(
                            result.getJoinMessage(), this.locale
                    ));
                                        
                    this.otherWsSession.getBasicRemote()
                            .sendObject(this.cloneAndLocalize(
                                    result.getJoinMessage(), this.otherLocale
                            ));
                                
                }// if join

                this.wsSession = session;
                
                exists = this.chatService
            			.chatSessionExists(firstUser, secondUser);    
            } catch(IOException | EncodeException e) {
                log.info("onOpen Exception: " + e);
            } finally {
                log.exit();
            }
        });
    }

    @OnMessage
    public void onMessage(ChatMessage message)
    {
        this.doSecured(() -> {
            if(this.closed) {
            	log.info("Chat message received after connection closed.");
                return;
            }
    
            message.setUser(this.principal.getUsername());
         
            try {
                this.wsSession.getBasicRemote().sendObject(message);
                this.otherWsSession.getBasicRemote().sendObject(message);
            } catch(IOException | EncodeException e) {
                this.onError(e);
            }
            log.exit();
        });
    }

    @OnClose
    public void onClose(CloseReason reason)
    {
    	/** to do: improve the close() method
    	 * get rid of illegal states
    	 * properly close the WS
    	 */
        this.doSecured(() -> {
            if(reason.getCloseCode() != CloseReason.CloseCodes.NORMAL_CLOSURE)
            {
                log.warn("Abnormal closure {} for reason [{}].",
                        reason.getCloseCode(), reason.getReasonPhrase());
            }

            synchronized(this) {
                if(this.closed) {               
                    return;
                }
                try {
                	this.close(PrivateChatService.ReasonForLeaving.NORMAL, null);
                } catch(IllegalStateException e) {
                	log.info("OnClose " + e);
                }
            }// synch
        });
    }

    @OnError
    public void onError(Throwable e)
    {	
        this.doSecured(() -> {
            log.warn("Error received in WebSocket session.", e);

            synchronized(this) {
                if(this.closed) {
                    return;
                }
                this.close(PrivateChatService.ReasonForLeaving.ERROR,
                        "error.chat.closed.exception");
            }
        });
    }

    private void sendPing()
    {
        if(!this.wsSession.isOpen())
            return;
        log.debug("Sending ping to WebSocket client.");
        try {
            this.wsSession.getBasicRemote()
                    .sendPing(ByteBuffer.wrap(PrivateChatEndpoint.pongData));
        } catch(IOException e) {
            log.warn("Failed to send ping message to WebSocket client.", e);
        }
    }

    @OnMessage
    public void onPong(PongMessage message)
    {
        ByteBuffer data = message.getApplicationData();
        if(!Arrays.equals(PrivateChatEndpoint.pongData, data.array()))
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
                new Date(System.currentTimeMillis() + 25_000L),
                25_000L
        );
    }

    public void httpSessionRemoved(SessionDestroyedEvent event)
    {
        String sessionId = event.getId();
        if(sessionId.equals(this.httpSession.getId())) {
            synchronized(this) {
                if(this.closed)
                    return;
                log.info("Chat session ended abruptly by {} logging out.",
                        this.principal.getUsername());
                this.close(PrivateChatService.ReasonForLeaving.LOGGED_OUT, null);
            }
        }
    }

    private void close(PrivateChatService.ReasonForLeaving reason, String unexpected)
    {
    
    	if(!this.pingFuture.isCancelled())
    			this.pingFuture.cancel(true);
     	this.sessionDestroyedListener.deregisterOnRemoveCallback(this.callback);
    	ChatMessage message = null;
    	if(this.chatSession != null)
    		message = this.chatService.leaveSession(this.chatSession,
    				this.principal.getUsername(), reason);
	
    	if(message != null) {    
    		CloseReason.CloseCode closeCode;
    		String reasonCode;
    		if(reason == PrivateChatService.ReasonForLeaving.ERROR) {
    			log.info("close UNEXPECTED");
    			closeCode = CloseReason.CloseCodes.UNEXPECTED_CONDITION;
    			reasonCode = unexpected;
    		} else {
    			closeCode = CloseReason.CloseCodes.NORMAL_CLOSURE;
    			reasonCode = "message.chat.ended";
    		}
	
    		synchronized(this.wsSession) {
    			if(this.wsSession.isOpen()) {				
    				try {
    					this.wsSession.close(new CloseReason(
    						closeCode, this.messageSource.getMessage(
    								reasonCode, null, this.locale))
    					);
    				} catch(IOException e) {
    					log.info("IOException " + e);
        
    				} catch(IllegalStateException e) {
    					log.info("Illegal caught " + e);
    				} catch(Exception e) {
    					e.printStackTrace();
    				}
        
    			}// if
    		}// synchronized
    	    	
    		if(this.otherWsSession != null) {
    	 		synchronized(this.otherWsSession) {
    	 			if(this.otherWsSession.isOpen()) {  
    	 				try {            
    	 					this.otherWsSession.close(new CloseReason(
                                    closeCode, this.messageSource.getMessage(
                                    reasonCode, null, this.otherLocale))
    	 					);

    	 					this.closed = true;    	 				
    	 				} catch (IOException e) {
    	 					log.info("IOException: " + e);    
    	 				} catch (IllegalStateException e) {
    	 					log.info("Illegal caught " + e);
    	 				} catch(Exception e) {
    	 					e.printStackTrace();
    	 				}
    	 			}// if
    	 		}// synchronized
    		}// if
    	}
    	
    	this.chatService.removeSession(this.chatSession);			
        	
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
        } catch(Exception e) {
        	log.info("Exception " + e);
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

        private static Locale getExposedLocale(Session session)
        {
            return (Locale)session.getUserProperties().get(LOCALE_KEY);
        }
    }
}
