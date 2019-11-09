package com.firstlinecode.granite.cluster.im;

import org.bson.Document;

import com.firstlinecode.granite.cluster.dba.AbstractDbInitializer;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

public class DbInitializer extends AbstractDbInitializer {

	@Override
	public void initialize(MongoDatabase database) {
		if (collectionExistsInDb(database, "subscriptions"))
			return;
		
		database.createCollection("subscriptions");
		MongoCollection<Document> subscriptions = database.getCollection("subscriptions");
		subscriptions.createIndex(Indexes.compoundIndex(Indexes.ascending("user"), Indexes.ascending("contact")), new IndexOptions().unique(true));
		
		database.createCollection("subscription_notifications");
		MongoCollection<Document> subscriptionNofications = database.getCollection("subscription_notifications");
		subscriptionNofications.createIndex(Indexes.compoundIndex(Indexes.ascending("user")));
	}

}
