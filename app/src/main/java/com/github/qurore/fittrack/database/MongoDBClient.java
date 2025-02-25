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
                        if (lastSlashIndex < connectionString.length() - 1 && !connectionString.substring(lastSlashIndex + 1).startsWith("?")) {
                            dbNameInUrl = connectionString.substring(lastSlashIndex + 1);
                            if (dbNameInUrl.contains("?")) {
                                dbNameInUrl = dbNameInUrl.substring(0, dbNameInUrl.indexOf("?"));
                            }
                            modifiedConnString = connectionString.substring(0, lastSlashIndex);
                            if (connectionString.contains("?")) {
                                modifiedConnString += "/" + connectionString.substring(connectionString.indexOf("?"));
                            }
                        }
                    }
                    
                    // Convert to standard connection string format
                    modifiedConnString = modifiedConnString.replace("mongodb+srv://", "mongodb://");
                    
                    // Extract host part
                    String hostPart = "";
                    if (modifiedConnString.contains("@")) {
                        hostPart = modifiedConnString.substring(modifiedConnString.indexOf("@") + 1);
                        if (hostPart.contains("/")) {
                            hostPart = hostPart.substring(0, hostPart.indexOf("/"));
                        }
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
                        
                        // Check if we already have query parameters
                        boolean hasParams = modifiedConnString.contains("?");
                        
                        // Add required parameters for direct connection
                        if (hasParams) {
                            modifiedConnString += "&ssl=true&replicaSet=atlas-" + 
                                               clusterName.substring(0, Math.min(6, clusterName.length())) + 
                                               "-shard-0&authSource=admin";
                            
                            // Only add these if they're not already present
                            if (!modifiedConnString.contains("retryWrites=")) {
                                modifiedConnString += "&retryWrites=true";
                            }
                            if (!modifiedConnString.contains("w=")) {
                                modifiedConnString += "&w=majority";
                            }
                        } else {
                            modifiedConnString += "/?ssl=true&replicaSet=atlas-" + 
                                               clusterName.substring(0, Math.min(6, clusterName.length())) + 
                                               "-shard-0&authSource=admin&retryWrites=true&w=majority";
                        }
                        
                        // Add back the database name if it was present
                        if (!dbNameInUrl.isEmpty()) {
                            if (modifiedConnString.contains("/?")) {
                                modifiedConnString = modifiedConnString.replace("/?", "/" + dbNameInUrl + "?");
                            } else if (modifiedConnString.contains("?")) {
                                int questionMarkIndex = modifiedConnString.indexOf("?");
                                modifiedConnString = modifiedConnString.substring(0, questionMarkIndex) + 
                                                  "/" + dbNameInUrl + 
                                                  modifiedConnString.substring(questionMarkIndex);
                            }
                        }
                        
                        // Ensure we have timeout parameters
                        if (!modifiedConnString.contains("connectTimeoutMS=")) {
                            modifiedConnString += "&connectTimeoutMS=30000";
                        }
                        if (!modifiedConnString.contains("socketTimeoutMS=")) {
                            modifiedConnString += "&socketTimeoutMS=60000";
                        }
                        
                        Log.d(TAG, "Modified connection string for direct connection: " + 
                              modifiedConnString.replaceAll(":[^/][^/][^@]*@", ":****@"));
                    }
                    
                    // Create MongoDB client with the modified connection string
                    ConnectionString connString = new ConnectionString(modifiedConnString);
                    MongoClientSettings settings = MongoClientSettings.builder()
                            .applyConnectionString(connString)
                            .applyToSocketSettings(builder -> {
                                builder.connectTimeout(30000, java.util.concurrent.TimeUnit.MILLISECONDS);
                                builder.readTimeout(60000, java.util.concurrent.TimeUnit.MILLISECONDS);
                            })
                            .build();
                    
                    mongoClient = MongoClients.create(settings);
                } else {
                    // Standard connection string
                    ConnectionString connString = new ConnectionString(connectionString);
                    MongoClientSettings settings = MongoClientSettings.builder()
                            .applyConnectionString(connString)
                            .applyToSocketSettings(builder -> {
                                builder.connectTimeout(30000, java.util.concurrent.TimeUnit.MILLISECONDS);
                                builder.readTimeout(60000, java.util.concurrent.TimeUnit.MILLISECONDS);
                            })
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
            int maxRetries = 3;
            int currentRetry = 0;
            boolean success = false;
            
            while (currentRetry < maxRetries && !success) {
                try {
                    // Database and collection will be created automatically if they don't exist
                    Log.d(TAG, "Inserting document into " + databaseName + "." + collectionName + 
                          " (Attempt " + (currentRetry + 1) + "/" + maxRetries + ")");
                    
                    // Ensure we have a fresh connection
                    if (currentRetry > 0) {
                        Log.d(TAG, "Refreshing MongoDB connection for retry attempt");
                        close();
                        connect();
                    }
                    
                    // Set a shorter server selection timeout for this operation
                    MongoCollection<Document> collection = mongoClient
                        .getDatabase(databaseName)
                        .getCollection(collectionName);
                    
                    // Execute with a timeout
                    collection.insertOne(document);
                    
                    Log.d(TAG, "Document inserted successfully");
                    success = true;
                    return true;
                } catch (MongoException e) {
                    currentRetry++;
                    Log.e(TAG, "Error inserting document (Attempt " + currentRetry + "/" + maxRetries + 
                          "): " + e.getMessage());
                    
                    if (currentRetry >= maxRetries) {
                        Log.e(TAG, "Maximum retry attempts reached. Giving up.");
                        return false;
                    }
                    
                    // Wait before retrying
                    try {
                        long backoffMs = 1000 * currentRetry;
                        Log.d(TAG, "Waiting " + backoffMs + "ms before retry...");
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        Log.e(TAG, "Retry interrupted", ie);
                        return false;
                    }
                }
            }
            
            return success;
        }).subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread());
    }

    // Method to asynchronously find documents with retry logic
    public Single<List<Document>> findDocumentsAsync(String collectionName, Document query) {
        return Single.fromCallable(() -> {
            List<Document> results = new ArrayList<>();
            int maxRetries = 3;
            int currentRetry = 0;
            boolean success = false;
            
            while (currentRetry < maxRetries && !success) {
                try {
                    Log.d(TAG, "Finding documents in " + databaseName + "." + collectionName + 
                          " (Attempt " + (currentRetry + 1) + "/" + maxRetries + ")");
                    
                    // Ensure we have a fresh connection for retries
                    if (currentRetry > 0) {
                        Log.d(TAG, "Refreshing MongoDB connection for retry attempt");
                        close();
                        connect();
                    }
                    
                    // Get collection and execute query
                    MongoCollection<Document> collection = mongoClient
                        .getDatabase(databaseName)
                        .getCollection(collectionName);
                    
                    collection.find(query).into(results);
                    success = true;
                } catch (MongoException e) {
                    currentRetry++;
                    Log.e(TAG, "Error finding documents (Attempt " + currentRetry + "/" + maxRetries + 
                          "): " + e.getMessage());
                    
                    if (currentRetry >= maxRetries) {
                        Log.e(TAG, "Maximum retry attempts reached. Returning empty results.");
                        break;
                    }
                    
                    // Wait before retrying
                    try {
                        long backoffMs = 1000 * currentRetry;
                        Log.d(TAG, "Waiting " + backoffMs + "ms before retry...");
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        Log.e(TAG, "Retry interrupted", ie);
                        break;
                    }
                }
            }
            
            return results;
        }).subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread());
    }
} 