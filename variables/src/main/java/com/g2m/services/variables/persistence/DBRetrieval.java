package com.g2m.services.variables.persistence;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.experimental.theories.ParametersSuppliedBy;
import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.enums.BarSize;
import com.g2m.services.variables.entities.Variable;
import com.ib.controller.Bar;
import com.ib.controller.NewComboLeg;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

@Component
public class DBRetrieval {

	String mongoDB;
	String mongoVariableCollection;
	String mongoBarCollection;

	public DBRetrieval() {
		mongoDB = "test";
		mongoVariableCollection = "variable";
		mongoBarCollection = "bars";
	}

	// Need to have the DB dynamically populated once Mike adds this in
	public List<DBObject> getVariablesFromMongo(Variable parameters){

		BasicDBObject query = createInitialVariableRetrievalQuery(parameters);

		try {

			// Get the most recent date for a given variable, if the date is
			// Not returned then its assumed the variables wasn't previously 
			// Calculated and we don't make a second call to Mongo
			Date date = getLastVariableCalculatedDate(query);
			if (date != null)
				return getVariablesWithMatchingDate(query, date);

		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;		
	}


	private List<DBObject> getVariablesWithMatchingDate(BasicDBObject query, Date date) throws UnknownHostException {

		MongoClient mongoClient;

		// Append the most recent date to the query so that we only return the items
		// That are current and have not expired
		query.append("calculatedOnDateTime", date);

		mongoClient = new MongoClient();
		DB db = mongoClient.getDB(mongoDB);
		DBCollection coll = db.getCollection(mongoVariableCollection);
		return coll.find(query).toArray();	

	}

	private Date getLastVariableCalculatedDate(BasicDBObject query) throws UnknownHostException {
		MongoClient mongoClient;

		// Only return the date time field for the object, we'll use this to
		// retrieve the most recent date and pass that back to the parent 
		// method where it will be used to retrieve all of the variables
		// with that date
		BasicDBObject fields = new BasicDBObject();
		fields.put("calculatedOnDateTime", 1);

		// setup the Mongo connection
		mongoClient = new MongoClient();
		DB db = mongoClient.getDB( mongoDB );
		DBCollection coll = db.getCollection(mongoVariableCollection);

		// Get the results set
		DBCursor resultCursor = coll.find(query, fields).sort(new BasicDBObject("calculatedOnDateTime",-1)).limit(1);

		if (resultCursor.hasNext()){
			DBObject  result = resultCursor.next();
			// Make sure we retrieved a Date object
			if (result.get("calculatedOnDateTime") instanceof Date){
				Date date = (Date) result.get("calculatedOnDateTime");
				return date;
			}
		}
		// if nothing was found, return nothing
		return null;
	}

	private BasicDBObject createInitialVariableRetrievalQuery(Variable parameters) {

		// Set the values for the query parameters, we look for the unique combination of:
		//   1) The variable name, and
		//   2) The variable bar size, and
		//   3) The security key
		BasicDBObject query = new BasicDBObject();
		query.append("variableName", parameters.getVariableName())
		.append("barSize", parameters.getBarSize().toString())
		.append("securityKey", new BasicDBObject("key", parameters.getSecurityKey().toString())); 

		return query;
	}

	public List<DBObject> getBarsFromDB(Security security, BarSize barSize) {

		BasicDBObject query = createInitialBarRetrievalQuery(security, barSize);


		try {
			// Get the most recent bars to load into the bar cache
			return getHistoricalBarsFromDB(query);

		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;		
	}

	
	private List<DBObject> getHistoricalBarsFromDB(BasicDBObject query) throws UnknownHostException {
		MongoClient mongoClient;

		// setup the Mongo connection
		mongoClient = new MongoClient();
		DB db = mongoClient.getDB(mongoDB);
		DBCollection coll = db.getCollection(mongoBarCollection);

		// Get the results set, currently returns oldest to youngest
		// but can change if made to sort by -1
		return coll.find(query).sort(new BasicDBObject("dateTime",-1)).limit(10000).toArray();
	}

	
	private BasicDBObject createInitialBarRetrievalQuery(Security security, BarSize barSize) {

		// Set the values for the query parameters, we look for the unique combination of:
		//   1) The securityKey, and
		//   2) The variable bar size, and
		//   3) The exchange, and
		//   4) The expiration date, and
		// 	 3) We also include the time so that we only pull enough to calculate the 
		// 		variables we need (plus a little extra) and not every bar from all time

		BasicDBObject query = new BasicDBObject();

		query.append("security.symbol", security.getSymbol())
			.append("security.exchange", security.getExchange())
			.append("security.securityType", security.getSecurityType().toString())
			.append("security.expiry", security.getExpiry())
			.append("barSize", barSize.toString());


		return query;
	}
}
