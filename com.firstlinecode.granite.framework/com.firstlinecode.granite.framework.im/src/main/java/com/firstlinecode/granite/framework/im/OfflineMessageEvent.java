package com.firstlinecode.granite.framework.im;

import com.firstlinecode.basalt.protocol.core.JabberId;
import com.firstlinecode.basalt.protocol.im.stanza.Message;
import com.firstlinecode.granite.framework.core.event.IEvent;

public class OfflineMessageEvent implements IEvent {
	private JabberId user;
	private JabberId contact;
	private Message message;
	
	public OfflineMessageEvent(JabberId user, JabberId contact, Message message) {
		this.user = user;
		this.contact = contact;
		this.message = message;
	}

	public JabberId getUser() {
		return user;
	}

	public void setUser(JabberId user) {
		this.user = user;
	}

	public JabberId getContact() {
		return contact;
	}

	public void setContact(JabberId contact) {
		this.contact = contact;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}
	
}
