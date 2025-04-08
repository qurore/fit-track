import os
import json
from datetime import datetime
from typing import Dict, List, Optional
from pymongo import MongoClient
from pymongo.server_api import ServerApi
from bson import ObjectId, json_util

# Initialize MongoDB client
client = MongoClient(os.environ['MONGODB_URI'], server_api=ServerApi('1'))
db = client.fittrack
exercises_collection = db.exercises

def parse_json(data):
    """Convert MongoDB BSON to JSON"""
    return json.loads(json_util.dumps(data))

def create_exercise(user_id: str, exercise_data: Dict) -> Dict:
    """Create a new exercise record."""
    exercise_doc = {
        "user_id": user_id,
        "exercise_type": exercise_data.get("exercise_type"),  # 'strength', 'cardio', 'flexibility', 'functional'
        "exercise_subtype": exercise_data.get("exercise_subtype"),  # Add exercise subtype
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
            "exercise_subtype": exercise_data.get("exercise_subtype"),
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
        return parse_json(updated_doc)
    except Exception as e:
        raise Exception(f"Error updating exercise: {str(e)}")

def get_user_exercises(user_id: str, exercise_type: Optional[str] = None) -> List[Dict]:
    """Get all exercises for a specific user, optionally filtered by type."""
    query = {"user_id": user_id}
    if exercise_type:
        query["exercise_type"] = exercise_type

    exercises = list(exercises_collection.find(query))
    return parse_json(exercises)

def lambda_handler(event, context):
    """Main Lambda handler function."""
    try:
        # Log the incoming event for debugging
        print(f"Received event: {json.dumps(event)}")
        
        # Validate event structure
        if not event:
            return {
                'statusCode': 400,
                'body': json.dumps({'message': 'Event object is empty'})
            }
            
        # Check if this is a test event from API Gateway
        if event.get('source') == 'aws.events':
            return {
                'statusCode': 200,
                'body': json.dumps({'message': 'Test event received successfully'})
            }
            
        # Get user ID from the authorizer context with proper error handling
        try:
            user_id = event.get('requestContext', {}).get('authorizer', {}).get('uid')
            if not user_id:
                print("Error: User ID not found in event context")
                print(f"RequestContext: {json.dumps(event.get('requestContext', {}))}")
                return {
                    'statusCode': 401,
                    'body': json.dumps({'message': 'Unauthorized - User ID not found'})
                }
        except Exception as e:
            print(f"Error extracting user ID: {str(e)}")
            return {
                'statusCode': 500,
                'body': json.dumps({'message': 'Error processing authorization'})
            }

        # Get HTTP method with proper error handling
        http_method = event.get('httpMethod')
        if not http_method:
            return {
                'statusCode': 400,
                'body': json.dumps({'message': 'HTTP method not specified'})
            }

        path_parameters = event.get('pathParameters', {})
        query_parameters = event.get('queryStringParameters', {})

        if http_method == 'POST':
            # Create new exercise
            try:
                body = json.loads(event.get('body', '{}'))
            except json.JSONDecodeError:
                return {
                    'statusCode': 400,
                    'body': json.dumps({'message': 'Invalid JSON in request body'})
                }
                
            result = create_exercise(user_id, body)
            return {
                'statusCode': 201,
                'body': json.dumps(parse_json(result))
            }

        elif http_method == 'PUT':
            # Update existing exercise
            exercise_id = path_parameters.get('id')
            if not exercise_id:
                return {
                    'statusCode': 400,
                    'body': json.dumps({'message': 'Exercise ID is required'})
                }

            try:
                body = json.loads(event.get('body', '{}'))
            except json.JSONDecodeError:
                return {
                    'statusCode': 400,
                    'body': json.dumps({'message': 'Invalid JSON in request body'})
                }
            
            result = update_exercise(user_id, exercise_id, body)
            
            if result is None:
                return {
                    'statusCode': 404,
                    'body': json.dumps({'message': 'Exercise not found'})
                }

            return {
                'statusCode': 200,
                'body': json.dumps(result)
            }

        elif http_method == 'GET':
            # Get user exercises
            exercise_type = query_parameters.get('type') if query_parameters else None
            result = get_user_exercises(user_id, exercise_type)
            return {
                'statusCode': 200,
                'body': json.dumps(result)
            }

        else:
            return {
                'statusCode': 405,
                'body': json.dumps({'message': 'Method not allowed'})
            }

    except Exception as e:
        print(f"Error in lambda_handler: {str(e)}")
        print(f"Event that caused the error: {json.dumps(event)}")
        return {
            'statusCode': 500,
            'body': json.dumps({
                'message': 'Internal server error',
                'error': str(e)
            })
        } 