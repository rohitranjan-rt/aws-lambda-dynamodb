package com.rapyder;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import java.util.Set;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.endpointdiscovery.Constants;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rapyder.entity.Creds;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * Note: Example:
 * http://docs.aws.amazon.com/lambda/latest/dg/get-started-step4-optional.html
 * doesn't work (gives - deserialization error with Integer
 * com.fasterxml.jackson.databind.JsonMappingException: Can not deserialize
 * instance of java.lang.Integer out of START_OBJECT token
 * 
 * Solution:
 * http://stackoverflow.com/questions/35545642/error-executing-hello-world-for-aws-lambda-in-java
 * http://docs.aws.amazon.com/AWSToolkitEclipse/latest/ug/lambda-tutorial.html
 * 
 * @author namit
 *
 */

public class App {
	// AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
	// DynamoDB dynamoDB = new DynamoDB(client);
	// String tableName = "cred";
	private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
	 private static String SECRET_KEY = "oeRaYY7Wo24sDqKSX3IM9ASGmdGPmkTd9jo1QTy4b7P9Ze5_9hKolVX8xNrQDcNRfVEdTZNOuOyqEGhXEbdJI-ZQ19k_o9MI0y3eZN2lp9jow55FfXMiINEdt1XR85VipRLSOkT6kSpzs2x-jbLDiz9iFVzkd81YKxMgPA7VfZeQUm4n-mOmnWMaVX30zGFU4L3oPBctYKkl4dYfqYWqRNfrgPJVi5DGFjywgxx0ASEiJHtV72paI3fDR2XwlSkyhhmY-ICjCRmsJN4fX1pdoL8a18-aQrvyu4j0Os6dVPYIoPvvY0SAZtWYKHfM15g7A3HD4cVREf9cUsprCRK93w";
	/*
	 * public static void main(String[] args) { AmazonDynamoDBClient dynamoDBClient
	 * = new AmazonDynamoDBClient( new BasicAWSCredentials("AKIAS5RVJMNGTNFRFLHW",
	 * "fKUWMhqcgala6jQWqF2YG91Lk4PEhX5s2ZGh8aSj")); QueryRequest query = new
	 * QueryRequest().withTableName("cred");
	 * 
	 * DynamoDB dynamoDB = new DynamoDB(dynamoDBClient);
	 * 
	 * Table table = dynamoDB.getTable("cred");
	 * 
	 * 
	 * QuerySpec spec = new QuerySpec() .withKeyConditionExpression("id = :v_id")
	 * .withValueMap(new ValueMap() .withString(":v_id", "1"));
	 * 
	 * ItemCollection<QueryOutcome> items = table.query(spec);
	 * 
	 * Iterator<Item> iterator = items.iterator(); Item item = null; while
	 * (iterator.hasNext()) { // item = iterator.next();
	 * System.out.println(iterator.next().toJSONPretty());
	 * 
	 * }
	 * 
	 * Item item = table.getItem("id", 120); System.err.println("query = " + query);
	 * System.out.println("Printing item after retrieving it...."+item); }
	 */

	public String myHandler(Map<String, String> map, Context context) {

		Map<String, String> responseHeaders = new HashMap<String, String>(map);
		String userName = "";
		String password = "";
		for (Map.Entry<String, String> pair : responseHeaders.entrySet()) {

			if (pair.getKey().equals("password"))
				password = pair.getValue();
			if (pair.getKey().equals("userName"))
				userName = pair.getValue();

			System.out.println(
					String.format("Key (userName) is: %s, Value (password) is : %s", pair.getKey(), pair.getValue()));
			System.err.println("userName Lambda = " + userName);
			System.err.println("password Lambda = " + password);
		}

		authorize(userName, password);
		// createItems();
		// retrieveItem();

		return "Success";

	}

	private void authorize(String userName, String password) {
		BasicAWSCredentials awsCreds = new BasicAWSCredentials("AKIAS5RVJMNGTNFRFLHW",
				"fKUWMhqcgala6jQWqF2YG91Lk4PEhX5s2ZGh8aSj");
		AmazonDynamoDB amazonDynamoDBClient = AmazonDynamoDBClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).withRegion("ap-south-1").build();
		DynamoDB dynamoDB = new DynamoDB(amazonDynamoDBClient);

		Table table = dynamoDB.getTable("creds");
		QuerySpec spec = new QuerySpec().withKeyConditionExpression("userName = :userName")
				.withValueMap(new ValueMap()
						.withString(":userName", userName));
		 //.withNumber(":id", 120)).withMaxPageSize(1);

		ItemCollection<QueryOutcome> items = table.query(spec);

		Iterator<Item> iterator = items.iterator();
		Item item = null;
		String user = "";
		String pass = "";
		while (iterator.hasNext()) {
			item = iterator.next();
			user = item.getString("userName");
			pass = item.getString("password");
			System.out.println("user = " + user);
			System.out.println("pass = " + pass);
			
			if (userName.equalsIgnoreCase(user)&& password.equalsIgnoreCase(pass)) {
				System.err.println("Login Succesfull...");
				 SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

			        long nowMillis = System.currentTimeMillis();
			        Date now = new Date(nowMillis);
			        
			        byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(SECRET_KEY);
			        Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
			        
			        JwtBuilder builder = Jwts.builder().setId(userName)
			                .setIssuedAt(now)
			                .setSubject(pass)
			               // .setIssuer(issuer)
			                .signWith(signatureAlgorithm, signingKey);
			        System.err.println("JwtBuilder = "+builder.toString());
			        //if it has been specified, let's add the expiration 
			        // https://tallygo.tallysolutions.com/m30/?authToken=token#
			        int ttlMillis= 1200;
			        if (ttlMillis >= 0) {
			            long expMillis = nowMillis + ttlMillis;
			            Date exp = new Date(expMillis);
			            builder.setExpiration(exp);
			        }
			        Object obj = new String(builder.compact());
			     //   JSON json = new JSON();
			        JSONObject obj1=new JSONObject();    
					 
			        String url =  "https://tallygo.tallysolutions.com/"+userName+"/"+obj;
			        obj1.put("URL",url);    
			        System.err.println("obj1 = "+obj1);
			      //  System.out.println("Builder.compact = "+builder.compact().toString());
			        //decodeJWT(builder.compact().toString());
			}else {
					System.err.println("Username and Password Not Valid");
			}
			
		}
	}
	
	public static void main(String[] args) {
		System.out.println("Main Started...");		
		JSONObject obj=new JSONObject();    
		  obj.put("URL","https://tallygo.tallysolutions.com/");    
		  
		   System.out.print(obj);   
		   System.out.println("Main Ended...");		  
	}
	
	public static void decodeJWT(String jwt) {

        //This line will throw an exception if it is not a signed JWS (as expected)
        Claims claims = Jwts.parser()
                .setSigningKey(DatatypeConverter.parseBase64Binary(SECRET_KEY))
                .parseClaimsJws(jwt).getBody();
        System.err.println("Claims = "+claims.toString());
       //return claims;
    }

	private static void createItems() {
		/*
		 * 
		 * LOGGER.info("App.createItems(): "); Table table =
		 * dynamoDB.getTable(tableName); try { System.err.println("App.createItems ");
		 * Item item = new Item().withString("userName", "Rohit").withString("password",
		 * "Password");
		 * 
		 * table.putItem(item);
		 * 
		 * item = new Item().withString("userName", "Aditya").withString("password",
		 * "pass");
		 * 
		 * item = new Item().withString("userName", "Santhosh").withString("password",
		 * "enc"); table.putItem(item);
		 * 
		 * 
		 * item = new Item().withPrimaryKey("Id", 121).withString("Title",
		 * "Book 121 Title") .withString("ISBN", "121-1111111111")
		 * .withStringSet("Authors", new HashSet<String>(Arrays.asList("Author21",
		 * "Author 22"))) .withNumber("Price", 20).withString("Dimensions",
		 * "8.5x11.0x.75").withNumber("PageCount", 500) .withBoolean("InPublication",
		 * true).withString("ProductCategory", "Book"); table.putItem(item);
		 * 
		 * } catch (Exception e) { System.err.println("Create items failed.");
		 * System.err.println(e.getMessage());
		 * 
		 * }
		 */}

	private static void retrieveItem() {
		/*
		 * 
		 * LOGGER.info("App.retrieveItem(): "); Table table =
		 * dynamoDB.getTable(tableName);
		 * 
		 * try { System.err.println("App.retrieveItem "); Item item =
		 * table.getItem("Id, userName, password", null); item =
		 * table.getItem("Id, userName, password", null); item =
		 * table.getItem("Id, userName, password", null);
		 * 
		 * System.out.println("Printing item after retrieving it....");
		 * System.out.println(item.toJSONPretty());
		 * 
		 * } catch (Exception e) { System.err.println("GetItem failed.");
		 * System.err.println(e.getMessage()); }
		 * 
		 */}

}
