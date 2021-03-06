package com.firstlinecode.granite.lite.xeps.muc;

import java.util.ArrayList;
import java.util.List;

import com.firstlinecode.basalt.protocol.core.JabberId;
import com.firstlinecode.basalt.protocol.core.ProtocolException;
import com.firstlinecode.basalt.protocol.core.stanza.Iq;
import com.firstlinecode.basalt.protocol.core.stanza.error.FeatureNotImplemented;
import com.firstlinecode.basalt.protocol.core.stanza.error.ItemNotFound;
import com.firstlinecode.basalt.protocol.im.stanza.Message;
import com.firstlinecode.basalt.xeps.muc.Affiliation;
import com.firstlinecode.basalt.xeps.muc.RoomConfig;
import com.firstlinecode.basalt.xeps.muc.RoomConfig.WhoIs;
import com.firstlinecode.basalt.xeps.xdata.Field;
import com.firstlinecode.basalt.xeps.xdata.XData;
import com.firstlinecode.granite.framework.core.annotations.Dependency;
import com.firstlinecode.granite.framework.core.auth.IAuthenticator;
import com.firstlinecode.granite.framework.core.config.IApplicationConfiguration;
import com.firstlinecode.granite.framework.core.config.IApplicationConfigurationAware;
import com.firstlinecode.granite.framework.core.config.IConfiguration;
import com.firstlinecode.granite.framework.core.supports.IApplicationComponentConfigurations;
import com.firstlinecode.granite.framework.core.repository.IInitializable;
import com.firstlinecode.granite.framework.processing.IProcessingContext;
import com.firstlinecode.granite.xeps.disco.IDiscoProvider;
import com.firstlinecode.granite.xeps.muc.AffiliatedUser;
import com.firstlinecode.granite.xeps.muc.IRoomService;
import com.firstlinecode.granite.xeps.muc.IRoomSession;
import com.firstlinecode.granite.xeps.muc.Occupant;
import com.firstlinecode.granite.xeps.muc.Room;
import com.firstlinecode.granite.xeps.muc.RoomItem;
import com.firstlinecode.basalt.xeps.disco.DiscoInfo;
import com.firstlinecode.basalt.xeps.disco.DiscoItems;
import com.firstlinecode.basalt.xeps.disco.Feature;
import com.firstlinecode.basalt.xeps.disco.Identity;
import com.firstlinecode.basalt.xeps.disco.Item;
import com.firstlinecode.basalt.xeps.rsm.Set;

public class MucDiscoProvider implements IDiscoProvider, IApplicationConfigurationAware, IInitializable {
	private static final String CONFIGURATION_KEY_MUC_IDENTITY_NAME = "muc.identity.name";
	private static final String CONFIGURATION_KEY_MUC_DOMAIN_NAME = "muc.domain.name";
	
	@Dependency("application.component.configurations")
	private IApplicationComponentConfigurations appComponentConfigurations;
	
	@Dependency("room.service")
	private IRoomService roomService;
	
	@Dependency("authenticator")
	private IAuthenticator authenticator;
	
	private String domainName;
	private String mucDomainName; 
	private String mucIdentityName;
	
	@Override
	public DiscoInfo discoInfo(IProcessingContext context, Iq iq, JabberId jid, String node) {
		if (!mucDomainName.equals(jid.getDomain()))
			return null;
		
		if (isDiscoServiceInfoRequest(jid, node)) {
			return discoServiceInfo(jid, node);
		}
		
		if (isDiscoRoomInfoRequest(jid, node)) {
			return discoRoomInfo(jid, node);
		}
		
		if (isDiscoXRoomUserItemRequest(jid, node)) {
			return discoXRoomUserItem(context, jid);
		}
		
		return null;
	}

	private DiscoInfo discoXRoomUserItem(IProcessingContext context, JabberId jid) {
		if (!roomService.exists(jid)) {
			throw new ProtocolException(new ItemNotFound());
		}
		
		IRoomSession roomSession = roomService.getRoomSession(jid);
		AffiliatedUser affiliatedUser = roomSession.getRoom().getAffiliatedUser(context.getJid());
		
		DiscoInfo discoInfo = new DiscoInfo();
		if (affiliatedUser != null && affiliatedUser.getNick() != null) {
			Identity identity = new Identity();
			identity.setCategory("conference");
			identity.setType("text");
			identity.setName(affiliatedUser.getNick());
			discoInfo.getIdentities().add(identity);
		}
		
		return discoInfo;
	}

	private boolean isDiscoXRoomUserItemRequest(JabberId jid, String node) {
		return "x-roomuser-item".equals(node);
	}

	private DiscoInfo discoRoomInfo(JabberId jid, String node) {
		if (!roomService.exists(jid)) {
			throw new ProtocolException(new ItemNotFound());
		}
		
		Room room = roomService.getRoomSession(jid).getRoom();
		DiscoInfo discoInfo = new DiscoInfo();
		
		Identity identity = new Identity("conference", "text");
		identity.setName(room.getRoomConfig().getRoomName());
		discoInfo.getIdentities().add(identity);
		
		discoInfo.getFeatures().add(new Feature("http://jabber.org/protocol/muc#register"));
		discoInfo.getFeatures().add(new Feature("http://jabber.org/protocol/muc#roomconfig"));
		discoInfo.getFeatures().add(new Feature("http://jabber.org/protocol/muc#roominfo"));
		
		RoomConfig roomConfig = room.getRoomConfig();
		if (roomConfig.isPublicRoom()) {
			discoInfo.getFeatures().add(new Feature("muc_public"));
		} else {
			discoInfo.getFeatures().add(new Feature("muc_hidden"));
		}
		
		if (roomConfig.isMembersOnly()) {
			discoInfo.getFeatures().add(new Feature("muc_membersonly"));
		} else {
			discoInfo.getFeatures().add(new Feature("muc_open"));
		}
		
		if (roomConfig.isModeratedRoom()) {
			discoInfo.getFeatures().add(new Feature("muc_moderated"));
		} else {
			discoInfo.getFeatures().add(new Feature("muc_unmoderated"));
		}
		
		if (roomConfig.isPersistentRoom()) {
			discoInfo.getFeatures().add(new Feature("muc_persistent"));
		} else {
			discoInfo.getFeatures().add(new Feature("muc_temporary"));
		}
		
		if (roomConfig.isPasswordProtectedRoom()) {
			discoInfo.getFeatures().add(new Feature("muc_passwordprotected"));
		} else {
			discoInfo.getFeatures().add(new Feature("muc_unsecured"));
		}
		
		if (roomConfig.getWhoIs() == WhoIs.ANYONE) {
			discoInfo.getFeatures().add(new Feature("muc_nonanonymous"));
		} else if (roomConfig.getWhoIs() == WhoIs.MODERATORS) {
			discoInfo.getFeatures().add(new Feature("muc_semianonymous"));
		} else {
			// fully anonymous
			// do nothing
		}
		
		// i18n support
		XData xData = new XData(XData.Type.RESULT);
		xData.getFields().add(createFormType());
		
		xData.getFields().add(createStringField("Maximum Number of History Messages Returned by Room",
				"muc#maxhistoryfetch", String.valueOf(roomConfig.getMaxHistoryFetch())));
		
		xData.getFields().add(createContactJidField(jid));
		
		if (roomConfig.getRoomDesc() != null) {
			xData.getFields().add(createStringField("Description", "muc#roominfo_description", roomConfig.getRoomDesc()));
		}
		
		if (roomConfig.getLang() != null) {
			xData.getFields().add(createStringField("Natural Language for Room Discussions", "muc#roominfo_lang",
					roomConfig.getLang()));
		}
		
		xData.getFields().add(createOccupantsField(jid));
		
		String subject = getRoomSubject(jid);
		if (subject != null) {
			xData.getFields().add(createStringField("Current Discussion Topic", "muc#roominfo_subject", subject));
		}
		
		xData.getFields().add(createStringField("The room subject can be modified by participants", "muc#roominfo_subjectmod",
				roomConfig.isChangeSubject() ? "1" : "0"));
		
		discoInfo.setXData(xData);
		
		return discoInfo;
	}

	private String getRoomSubject(JabberId roomJid) {
		Message subject = roomService.getRoomSession(roomJid).getSubject();
		if (subject == null)
			return null;
		
		return subject.getSubject();
	}

	private Field createContactJidField(JabberId roomJid) {
		Field field = new Field();
		field.setLabel("Contact Addresses");
		field.setVar("muc#roominfo_contactjid");
		
		for (JabberId owner : getOwners(roomJid)) {
			field.getValues().add(owner.toString());
		}
		
		return field;
	}
	
	private List<JabberId> getOwners(JabberId roomJid) {
		Room room = roomService.getRoomSession(roomJid).getRoom();
		
		List<JabberId> owners = new ArrayList<>();
		for (AffiliatedUser affiliatedUser : room.getAffiliatedUsers()) {
			if (affiliatedUser.getAffiliation() == Affiliation.OWNER) {
				owners.add(affiliatedUser.getJid());
			}
		}
		
		return owners;
	}

	private Field createOccupantsField(JabberId roomJid) {
		Field field = new Field();
		field.setLabel("Number of occupants");
		field.setVar("muc#roominfo_occupants");
		field.getValues().add(String.valueOf(getOccupants(roomJid)));
		
		return field;
	}

	private int getOccupants(JabberId roomJid) {
		int numberOfOccupants = 0;
		for (Occupant occupant : roomService.getRoomSession(roomJid).getOccupants()) {
			numberOfOccupants += occupant.getJids().length;
		}
		
		return numberOfOccupants;
	}

	private Field createStringField(String label, String var, String value) {
		Field field = new Field();
		field.setLabel(label);
		field.setVar(var);
		field.getValues().add(value);
		
		return field;
	}

	private Field createFormType() {
		Field formType = new Field();
		formType.setType(Field.Type.HIDDEN);
		formType.setVar("FORM_TYPE");
		formType.getValues().add("http://jabber.org/protocol/muc#roominfo");
		return formType;
	}

	private boolean isDiscoRoomInfoRequest(JabberId jid, String node) {
		return node == null;
	}

	private DiscoInfo discoServiceInfo(JabberId jid, String node) {
		DiscoInfo discoInfo = new DiscoInfo();
		
		Identity identity = new Identity("conference", "text");
		identity.setName(mucIdentityName);
		discoInfo.getIdentities().add(identity);
		
		discoInfo.getFeatures().add(new Feature("http://jabber.org/protocol/muc"));
		discoInfo.getFeatures().add(new Feature("http://jabber.org/protocol/muc#admin"));
		discoInfo.getFeatures().add(new Feature("http://jabber.org/protocol/muc#owner"));
		discoInfo.getFeatures().add(new Feature("http://jabber.org/protocol/muc#user"));
		discoInfo.getFeatures().add(new Feature("muc_rooms"));
		
		return discoInfo;
	}

	private boolean isDiscoServiceInfoRequest(JabberId jid, String node) {
		return mucDomainName.equals(jid.toString()) && node == null;
	}

	@Override
	public DiscoItems discoItems(IProcessingContext context, Iq iq, JabberId jid, String node) {
		if (domainName.equals(jid.toString()) && node == null) {
			return discoverMucService();
		} else if (mucDomainName.equals(jid.toString()) && node == null) {
			DiscoItems discoItems = iq.getObject();
			
			if (discoItems.getSet() != null) {
				return discoverRoomsUsingResultSet(discoItems.getSet());
			}
			
			return discoverRooms();
		} else {
			return null;
		}
	}

	private DiscoItems discoverRoomsUsingResultSet(Set set) {
		Integer max = 0;
		if (set != null) {
			max = set.getMax();
		}
		
		if (max != null && max == 0) {
			return discoverTotalNumberOfRooms();
		}
		
		return discoverParitialListOfRooms(set);
	}

	private DiscoItems discoverParitialListOfRooms(Set set) {
		// TODO Auto-generated method stub
		throw new ProtocolException(new FeatureNotImplemented("Disco rooms using result set isn't implemented yet."));
	}

	private DiscoItems discoverTotalNumberOfRooms() {
		int totalNumber = roomService.getTotalNumberOfRooms();
		
		DiscoItems discoItems = new DiscoItems();
		Set set = new Set();
		set.setCount(totalNumber);
		discoItems.setSet(set);
		
		return discoItems;
	}

	private DiscoItems discoverRooms() {
		if (isFullListOfRoomsLarge()) {
			return discoverParitialListOfRooms();
		} else {
			return discoverAllRooms();
		}
	}
	
	private DiscoItems discoverParitialListOfRooms() {
		// TODO return partial list of rooms
		return null;
	}

	private DiscoItems discoverAllRooms() {
		List<RoomItem> roomItems = roomService.getRoomItems();
		DiscoItems items = new DiscoItems();
		for (RoomItem roomItem : roomItems) {
			Item item = new Item();
			item.setJid(roomItem.getJid());
			item.setName(roomItem.getName());
			
			items.getItems().add(item);
		}
		
		return items;
	}

	private boolean isFullListOfRoomsLarge() {
		// TODO Auto-generated method stub
		return false;
	}

	private DiscoItems discoverMucService() {
		DiscoItems discoItems = new DiscoItems();
		discoItems.getItems().add(new Item(JabberId.parse(mucDomainName)));
		
		return discoItems;
	}

	@Override
	public void init() {
		IConfiguration configuration = appComponentConfigurations.getConfiguration(
				"com.firstlinecode.granite.xeps.muc");
		mucDomainName = configuration.getString(CONFIGURATION_KEY_MUC_DOMAIN_NAME, "muc." + domainName);
		mucIdentityName = configuration.getString(CONFIGURATION_KEY_MUC_IDENTITY_NAME, "muc.service");
	}

	@Override
	public void setApplicationConfiguration(IApplicationConfiguration appConfiguration) {
		domainName = appConfiguration.getDomainName();
	}

}
