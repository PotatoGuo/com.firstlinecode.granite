package com.firstlinecode.granite.framework.im;

import com.firstlinecode.basalt.protocol.core.JabberId;
import com.firstlinecode.granite.framework.core.event.IEvent;

public class ResourceAvailabledEvent implements IEvent {
	private JabberId jid;
	
	public ResourceAvailabledEvent(JabberId jid) {
		this.jid = jid;
	}
	
	public JabberId getJid() {
		return jid;
	}
}
