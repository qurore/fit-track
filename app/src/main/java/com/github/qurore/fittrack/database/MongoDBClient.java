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
                ConnectionString connString = new ConnectionString(connectionString);
                MongoClientSettings settings = MongoClientSettings.builder()
                        .applyConnectionString(connString)
                        .build();
                
                mongoClient = MongoClients.create(settings);
                Log.d(TAG, "Successfully connected to MongoDB Atlas");
            } catch (Exception e) {
                Log.e(TAG, "Error connecting to MongoDB Atlas: " + e.getMessage());
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

    // 非同期でドキュメントを挿入するメソッド
    public Single<Boolean> insertDocumentAsync(String collectionName, Document document) {
        return Single.fromCallable(() -> {
            try {
                getCollection(collectionName).insertOne(document);
                return true;
            } catch (MongoException e) {
                Log.e(TAG, "Error inserting document: " + e.getMessage());
                return false;
            }
        }).subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread());
    }

    // 非同期でドキュメントを検索するメソッド
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