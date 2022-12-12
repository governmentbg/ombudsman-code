package com.ib.omb.notifications;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import javax.inject.Inject;
import javax.websocket.CloseReason.CloseCode;

import org.omnifaces.cdi.push.SocketEvent;
import org.omnifaces.cdi.push.SocketEvent.Closed;
import org.omnifaces.cdi.push.SocketEvent.Opened;
import org.omnifaces.cdi.push.SocketEvent.Switched;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Обсървър, който следи за логването на всеки потребител (WebSocket) и поддържа
 * списък в PushBEan
 * 
 * @author krasi
 *
 */
@ApplicationScoped
public class WebSocketObserver {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketObserver.class);

	@Inject
	PushBean pb;

	public void onOpen(@Observes @Opened SocketEvent event) {
		String channel = event.getChannel(); // Returns <o:socket channel>.
		LOGGER.debug("Socket channel opened:" + channel + " User=" + event.getUser());
		if (event.getUser() != null) {
			pb.addToLoggedIn(event.getUser());
			pb.refreshUsers();
		}
//        Long userId = event.getUser(); // Returns <o:socket user>, if any.
		// Do your thing with it. E.g. collecting them in a concurrent/synchronized
		// collection.
		// Do note that a single person can open multiple sockets on same channel/user.
	}

    public void onSwitch(@Observes @Switched SocketEvent event) {
        String channel = event.getChannel(); // Returns <o:socket channel>.
        LOGGER.debug("Socket channel switched:"+channel);
//        Long currentUserId = event.getUser(); // Returns current <o:socket user>, if any.
//        Long previousUserId = event.getPreviousUser(); // Returns previous <o:socket user>, if any.
        // Do your thing with it. E.g. updating in a concurrent/synchronized collection.
    }

	 public void onClose(@Observes @Closed SocketEvent event) {
		String channel = event.getChannel(); // Returns <o:socket channel>.
		LOGGER.debug("Socket channel closed:" + channel + " User=" + event.getUser());
//        Long userId = event.getUser(); // Returns <o:socket user>, if any.
		CloseCode code = event.getCloseCode(); // Returns close reason code.
		LOGGER.debug("close code:" + code);
		// Do your thing with it. E.g. removing them from collection.
		if (event.getUser() != null) {
			pb.removeFromLoggedIn(event.getUser());
			pb.refreshUsers();
		}

	}
}
