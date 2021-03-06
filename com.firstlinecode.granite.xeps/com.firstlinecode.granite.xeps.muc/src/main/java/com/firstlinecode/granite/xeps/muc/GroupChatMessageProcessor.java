package com.firstlinecode.granite.xeps.muc;

import com.firstlinecode.basalt.protocol.core.ProtocolException;
import com.firstlinecode.basalt.protocol.core.stanza.error.BadRequest;
import com.firstlinecode.basalt.protocol.core.stanza.error.ItemNotFound;
import com.firstlinecode.basalt.protocol.im.stanza.Message;
import com.firstlinecode.granite.framework.core.annotations.Dependency;
import com.firstlinecode.granite.framework.processing.IMessageProcessor;
import com.firstlinecode.granite.framework.processing.IProcessingContext;

public class GroupChatMessageProcessor implements IMessageProcessor {
	@Dependency("muc.protocols.processor")
	private MucProtocolsProcessor delegate;
	
	@Dependency("room.service")
	private IRoomService roomService;

	@Override
	public boolean process(IProcessingContext context, Message message) {
		if (message.getType() == Message.Type.GROUPCHAT) {
			if (message.getTo() == null) {
				throw new ProtocolException(new BadRequest("Null room jid."));
			}
			
			if (!message.getTo().isBareId()) {
				throw new ProtocolException(new BadRequest("Not a valid room JID."));
			}
			
			if (!roomService.exists(message.getTo())) {
				throw new ProtocolException(new ItemNotFound());
			}
			
			delegate.processRoomSubjectOrGroupChatMessage(context, message);
			return true;
		} else if (isGroupChatPrivateMessage(message)) {
			delegate.processGroupChatPrivateMessage(context, message);
			return true;
		} else {
			return false;
		}
	}

	private boolean isGroupChatPrivateMessage(Message message) {
		return (message.getType() == null || message.getType() == Message.Type.CHAT) &&
				(message.getTo() != null && !message.getTo().isBareId()) &&
				roomService.exists(message.getTo().getBareId());
	}

}
