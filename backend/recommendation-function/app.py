import os
import json
import random
from datetime import datetime
from typing import Dict, List, Optional
from pymongo import MongoClient
from pymongo.server_api import ServerApi
from bson import json_util

# Initialize MongoDB client
client = MongoClient(os.environ['MONGODB_URI'], server_api=ServerApi('1'))
db = client.fittrack
exercises_collection = db.exercises

def parse_json(data):
    """Convert MongoDB BSON to JSON"""
    return json.loads(json_util.dumps(data))

def get_exercise_catalog():
    """Load the exercise catalog from the exercises.json file"""
    try:
        dir_path = os.path.dirname(os.path.realpath(__file__))
        exercises_json_path = os.path.join(dir_path, '..', 'exercise-function', 'exercises.json')
        
        with open(exercises_json_path, 'r') as f:
            catalog = json.load(f)
        return catalog
    except Exception as e:
        print(f"Error loading exercise catalog: {str(e)}")
        return None

def get_random_exercise_from_catalog():
    """Get a random exercise from the exercises.json catalog"""
    catalog = get_exercise_catalog()
    if not catalog:
        # Fallback to a default exercise if catalog can't be loaded
        return {
            "exercise_type": "strength",
            "exercise_subtype": "chest",
            "exercise_name": "bench press"
        }
    
    # Select a random exercise type
    exercise_type = random.choice(catalog['categories'])
    
    # Select a random subcategory
    subcategory = random.choice(exercise_type['subcategories'])
    
    # Select a random exercise
    exercise = random.choice(subcategory['exercises'])
    
    return {
        "exercise_type": exercise_type['type'].lower(),
        "exercise_subtype": subcategory['name'].lower(),
        "exercise_name": exercise['name'].lower()
    }

def get_user_exercise_recommendation(user_id: str) -> Dict:
    """Get a recommended exercise for the user.
    
    Strategy:
    1. If user has exercise history, recommend a random exercise from their history
    2. If user has no history, recommend a random exercise from the catalog
    """
    try:
        # Get user's exercise history
        user_exercises = list(exercises_collection.find({"user_id": user_id}))
        
        if user_exercises:
            # User has exercise history, recommend one of their exercises
            random_exercise = random.choice(user_exercises)
            
            # Extract the relevant fields
            recommendation = {
                "exercise_type": random_exercise.get("exercise_type"),
                "exercise_subtype": random_exercise.get("exercise_subtype"),
                "exercise_name": random_exercise.get("exercise_name")
            }
            
            return recommendation
        else:
            # User has no exercise history, recommend a random exercise from catalog
            return get_random_exercise_from_catalog()
    except Exception as e:
        print(f"Error getting exercise recommendation: {str(e)}")
        # Return a fallback recommendation if an error occurs
        return get_random_exercise_from_catalog()

def lambda_handler(event, context):
    """Main Lambda handler function."""
    try:
        # Log the incoming event for debugging
        print(f"Received event: {json.dumps(event)}")
        
        # Extract user ID from the authorizer context
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

        if http_method == 'GET':
            # Get recommendation for the user
            recommendation = get_user_exercise_recommendation(user_id)
            
            return {
                'statusCode': 200,
                'body': json.dumps(recommendation)
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