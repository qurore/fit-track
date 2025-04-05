/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const {onRequest} = require("firebase-functions/v2/https");
const {onCall} = require("firebase-functions/v2/https");
const {onUserCreated} = require("firebase-functions/v2/identity");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const {MongoClient} = require("mongodb");

// Initialize Firebase Admin SDK
admin.initializeApp();

// MongoDB Connection URI from environment config
const MONGODB_URI = process.env.MONGODB_URI || "";
let mongoClient = null;

/**
 * Establishes a connection to MongoDB Atlas.
 * Reuses the connection if it already exists.
 * @returns {Promise<MongoClient>} The MongoDB client instance
 */
async function connectToMongoDB() {
  if (!mongoClient) {
    try {
      mongoClient = new MongoClient(MONGODB_URI);
      await mongoClient.connect();
      logger.info("Successfully connected to MongoDB Atlas");
    } catch (error) {
      logger.error("MongoDB connection error:", error);
      throw error;
    }
  }
  return mongoClient;
}

/**
 * Creates a new user document in MongoDB when a user is created in Firebase Auth
 */
exports.onUserSignUp = onUserCreated(async (event) => {
  const user = event.data;
  logger.info("New user created:", {uid: user.uid, email: user.email});

  try {
    const client = await connectToMongoDB();
    const db = client.db("fittrack");
    const usersCollection = db.collection("users");

    const userDoc = {
      _id: user.uid,
      email: user.email,
      displayName: user.displayName || null,
      photoURL: user.photoURL || null,
      createdAt: new Date(),
      updatedAt: new Date(),
      preferences: {
        // Default user preferences
        weightUnit: "kg",
        distanceUnit: "km",
        theme: "light"
      },
      profile: {
        height: null,
        weight: null,
        birthDate: null,
        gender: null,
        fitnessLevel: "beginner"
      }
    };

    await usersCollection.insertOne(userDoc);
    logger.info("Successfully created user document in MongoDB:", {uid: user.uid});
  } catch (error) {
    logger.error("Error creating user document:", error);
    throw error;
  }
});

/**
 * HTTP endpoint to check if MongoDB connection is working
 */
exports.checkMongoConnection = onRequest(async (req, res) => {
  try {
    const client = await connectToMongoDB();
    await client.db("admin").command({ping: 1});
    res.json({status: "success", message: "MongoDB connection successful"});
  } catch (error) {
    logger.error("MongoDB connection test failed:", error);
    res.status(500).json({ 
      status: "error", 
      message: "MongoDB connection failed",
      error: error.message 
    });
  }
});

/**
 * Callable function to get user profile
 */
exports.getUserProfile = onCall(async (data, context) => {
  // Check if user is authenticated
  if (!context.auth) {
    throw new Error("Unauthorized");
  }

  try {
    const client = await connectToMongoDB();
    const db = client.db("fittrack");
    const usersCollection = db.collection("users");

    const userProfile = await usersCollection.findOne({_id: context.auth.uid});
    if (!userProfile) {
      throw new Error("User profile not found");
    }

    return userProfile;
  } catch (error) {
    logger.error("Error fetching user profile:", error);
    throw error;
  }
});

// Create and deploy your first functions
// https://firebase.google.com/docs/functions/get-started

// exports.helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });
