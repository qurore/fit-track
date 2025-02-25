package com.github.qurore.fittrack.database;

import android.util.Log;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MongoDBClient {
    private static final String TAG = "MongoDBClient";
    private static MongoDBClient instance;
    private MongoClient mongoClient;
    private final String connectionString;
    private final String databaseName;

    private MongoDBClient(String connectionString, String databaseName) {
        this.connectionString = connectionString;
        this.databaseName = databaseName;
    }

    public static synchronized MongoDBClient getInstance(String connectionString, String databaseName) {
        if (instance == null) {
            instance = new MongoDBClient(connectionString, databaseName);
        }
        return instance;
    }

    public synchronized void connect() {
        if (mongoClient == null) {
            try {
                Log.d(TAG, "Attempting to connect to MongoDB with connection string: " + 
                      connectionString.replaceAll(":[^/][^/][^@]*@", ":****@"));
                
                // Check if using SRV connection string
                if (connectionString.startsWith("mongodb+srv://")) {
                    Log.w(TAG, "SRV connection strings require special handling on Android");
                    
                    // Parse the connection string to extract components
                    String modifiedConnString = connectionString;
                    
                    // Extract database name if present
                    String dbNameInUrl = "";
                    if (connectionString.contains("/") && connectionString.lastIndexOf("/") > connectionString.indexOf("@")) {
                        int lastSlashIndex = connectionString.lastIndexOf("/");
                        dbNameInUrl = connectionString.substring(lastSlashIndex + 1);
                        modifiedConnString = connectionString.substring(0, lastSlashIndex);
                    }
                    
                    // Convert to standard connection string format
                    modifiedConnString = modifiedConnString.replace("mongodb+srv://", "mongodb://");
                    
                    // Extract host part
                    String hostPart = "";
                    if (modifiedConnString.contains("@")) {
                        hostPart = modifiedConnString.substring(modifiedConnString.indexOf("@") + 1);
                    }
                    
                    // For MongoDB Atlas, we can construct the direct connection string
                    if (hostPart.contains(".mongodb.net")) {
                        // Get the cluster name
                        String clusterName = hostPart.split("\\.")[0];
                        
                        // Construct direct connection to all shards
                        String shardedHosts = clusterName + "-shard-00-00." + hostPart.substring(hostPart.indexOf(".") + 1) + ":27017," +
                                             clusterName + "-shard-00-01." + hostPart.substring(hostPart.indexOf(".") + 1) + ":27017," +
                                             clusterName + "-shard-00-02." + hostPart.substring(hostPart.indexOf(".") + 1) + ":27017";
                        
                        // Replace the host part
                        modifiedConnString = modifiedConnString.replace(hostPart, shardedHosts);
                        
                        // Add required parameters for direct connection
                        modifiedConnString += "/?ssl=true&replicaSet=atlas-" + 
                                             clusterName.substring(0, Math.min(6, clusterName.length())) + 
                                             "-shard-0&authSource=admin&retryWrites=true&w=majority";
                        
                        // Add back the database name if it was present
                        if (!dbNameInUrl.isEmpty()) {
                            modifiedConnString = modifiedConnString.replace("/?", "/" + dbNameInUrl + "?");
                        }
                        
                        Log.d(TAG, "Modified connection string for direct connection: " + 
                              modifiedConnString.replaceAll(":[^/][^/][^@]*@", ":****@"));
                    }
                    
                    // Create MongoDB client with the modified connection string
                    ConnectionString connString = new ConnectionString(modifiedConnString);
                    MongoClientSettings settings = MongoClientSettings.builder()
                            .applyConnectionString(connString)
                            .build();
                    
                    mongoClient = MongoClients.create(settings);
                } else {
                    // Standard connection string
                    ConnectionString connString = new ConnectionString(connectionString);
                    MongoClientSettings settings = MongoClientSettings.builder()
                            .applyConnectionString(connString)
                            .build();
                    
                    mongoClient = MongoClients.create(settings);
                }
                Log.d(TAG, "Successfully connected to MongoDB Atlas");
            } catch (Exception e) {
                Log.e(TAG, "Error connecting to MongoDB Atlas: " + e.getMessage(), e);
                throw e;
            }
        }
    }

    public synchronized void close() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            Log.d(TAG, "MongoDB connection closed");
        }
    }

    public MongoDatabase getDatabase() {
        if (mongoClient == null) {
            connect();
        }
        return mongoClient.getDatabase(databaseName);
    }

    public MongoCollection<Document> getCollection(String collectionName) {
        return getDatabase().getCollection(collectionName);
    }

    // Method to asynchronously insert a document
    public Single<Boolean> insertDocumentAsync(String collectionName, Document document) {
        return Single.fromCallable(() -> {
            try {
                // Database and collection will be created automatically if they don't exist
                Log.d(TAG, "Inserting document into " + databaseName + "." + collectionName);
                getCollection(collectionName).insertOne(document);
                Log.d(TAG, "Document inserted successfully");
                return true;
            } catch (MongoException e) {
                Log.e(TAG, "Error inserting document: " + e.getMessage(), e);
                return false;
            }
        }).subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread());
    }

    // Method to asynchronously find documents
    public Single<List<Document>> findDocumentsAsync(String collectionName, Document query) {
        return Single.fromCallable((Callable<List<Document>>) () -> {
            List<Document> results = new ArrayList<>();
            try {
                getCollection(collectionName).find(query).into(results);
            } catch (MongoException e) {
                Log.e(TAG, "Error finding documents: " + e.getMessage());
            }
            return results;
        }).subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread());
    }
} 