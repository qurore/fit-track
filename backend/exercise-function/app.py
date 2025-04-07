import json
import os
from datetime import datetime
from typing import Dict, List, Optional

import firebase_admin
from firebase_admin import auth, credentials
from pymongo import MongoClient
from bson import ObjectId

# Initialize Firebase Admin SDK
if not firebase_admin._apps:
    cred = credentials.Certificate('fittrack-6775b-firebase-adminsdk.json')
    firebase_admin.initialize_app(cred)

# Initialize MongoDB client
mongo_uri = os.environ['MONGODB_URI']
client = MongoClient(mongo_uri)
db = client.fittrack
exercises_collection = db.exercises

def verify_firebase_token(token: str) -> Dict:
    """Verify Firebase ID token and return user claims."""
    try:
        decoded_token = auth.verify_id_token(token)
        return decoded_token
    except Exception as e:
        raise Exception(f"Invalid token: {str(e)}")

def create_exercise(user_id: str, exercise_data: Dict) -> Dict:
    """Create a new exercise record."""
    exercise_doc = {
        "user_id": user_id,
        "exercise_type": exercise_data.get("exercise_type"),  # 'strength', 'cardio', 'flexibility', 'functional'
        "exercise_name": exercise_data.get("exercise_name"),
        "start_time": exercise_data.get("start_time"),
        "duration": exercise_data.get("duration"),
        "created_at": datetime.utcnow(),
        "updated_at": datetime.utcnow(),
        "notes": exercise_data.get("notes")
    }

    # Add type-specific fields
    if exercise_data.get("exercise_type") == "strength":
        exercise_doc.update({
            "weight": exercise_data.get("weight"),
            "reps": exercise_data.get("reps")
        })
    elif exercise_data.get("exercise_type") == "cardio":
        exercise_doc.update({
            "distance": exercise_data.get("distance")
        })

    result = exercises_collection.insert_one(exercise_doc)
    exercise_doc["_id"] = str(result.inserted_id)
    return exercise_doc

def update_exercise(user_id: str, exercise_id: str, exercise_data: Dict) -> Optional[Dict]:
    """Update an existing exercise record."""
    try:
        exercise_doc = {
            "exercise_type": exercise_data.get("exercise_type"),
            "exercise_name": exercise_data.get("exercise_name"),
            "start_time": exercise_data.get("start_time"),
            "duration": exercise_data.get("duration"),
            "updated_at": datetime.utcnow(),
            "notes": exercise_data.get("notes")
        }

        # Add type-specific fields
        if exercise_data.get("exercise_type") == "strength":
            exercise_doc.update({
                "weight": exercise_data.get("weight"),
                "reps": exercise_data.get("reps")
            })
        elif exercise_data.get("exercise_type") == "cardio":
            exercise_doc.update({
                "distance": exercise_data.get("distance")
            })

        # Remove None values
        exercise_doc = {k: v for k, v in exercise_doc.items() if v is not None}

        result = exercises_collection.update_one(
            {"_id": ObjectId(exercise_id), "user_id": user_id},
            {"$set": exercise_doc}
        )

        if result.modified_count == 0:
            return None

        updated_doc = exercises_collection.find_one({"_id": ObjectId(exercise_id)})
        updated_doc["_id"] = str(updated_doc["_id"])
        return updated_doc
    except Exception as e:
        raise Exception(f"Error updating exercise: {str(e)}")

def get_user_exercises(user_id: str, exercise_type: Optional[str] = None) -> List[Dict]:
    """Get all exercises for a specific user, optionally filtered by type."""
    query = {"user_id": user_id}
    if exercise_type:
        query["exercise_type"] = exercise_type

    exercises = list(exercises_collection.find(query))
    for exercise in exercises:
        exercise["_id"] = str(exercise["_id"])
    return exercises

def lambda_handler(event, context):
    """Main Lambda handler function."""
    try:
        # Extract authorization token
        auth_token = event.get('headers', {}).get('Authorization', '').replace('Bearer ', '')
        if not auth_token:
            return {
                'statusCode': 401,
                'body': json.dumps({'error': 'No authorization token provided'})
            }

        # Verify token and get user claims
        user_claims = verify_firebase_token(auth_token)
        user_id = user_claims['uid']

        # Process request based on HTTP method
        http_method = event['httpMethod']
        path_parameters = event.get('pathParameters', {})
        query_parameters = event.get('queryStringParameters', {})

        if http_method == 'POST':
            # Create new exercise
            body = json.loads(event['body'])
            result = create_exercise(user_id, body)
            return {
                'statusCode': 201,
                'body': json.dumps(result)
            }

        elif http_method == 'PUT':
            # Update existing exercise
            exercise_id = path_parameters.get('id')
            if not exercise_id:
                return {
                    'statusCode': 400,
                    'body': json.dumps({'error': 'Exercise ID is required'})
                }

            body = json.loads(event['body'])
            result = update_exercise(user_id, exercise_id, body)
            
            if result is None:
                return {
                    'statusCode': 404,
                    'body': json.dumps({'error': 'Exercise not found'})
                }

            return {
                'statusCode': 200,
                'body': json.dumps(result)
            }

        elif http_method == 'GET':
            # Get user exercises
            exercise_type = query_parameters.get('type')
            result = get_user_exercises(user_id, exercise_type)
            return {
                'statusCode': 200,
                'body': json.dumps(result)
            }

        else:
            return {
                'statusCode': 405,
                'body': json.dumps({'error': 'Method not allowed'})
            }

    except Exception as e:
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        } 