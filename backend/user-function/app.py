import os
import json
from pymongo import MongoClient
from pymongo.server_api import ServerApi
from bson import json_util

# Initialize MongoDB client
client = MongoClient(os.environ['MONGODB_URI'], server_api=ServerApi('1'))
db = client.fittrack
users_collection = db.users

def parse_json(data):
    """Convert MongoDB BSON to JSON"""
    return json.loads(json_util.dumps(data))

def lambda_handler(event, context):
    try:
        # Get user ID from the authorizer context
        user_id = event['requestContext']['authorizer']['uid']
        http_method = event['httpMethod']
        
        if http_method == 'GET':
            # Get user info
            user = users_collection.find_one({'_id': user_id})
            if user:
                return {
                    'statusCode': 200,
                    'body': json.dumps(parse_json(user))
                }
            else:
                return {
                    'statusCode': 404,
                    'body': json.dumps({'message': 'User not found'})
                }
                
        elif http_method == 'POST':
            # Create or update user info
            body = json.loads(event['body'])
            
            # Add required fields
            user_data = {
                '_id': user_id,  # Use Firebase UID as MongoDB _id
                'email': event['requestContext']['authorizer']['email']
            }
            
            # If name is provided in the request body, use it
            if 'name' in body:
                user_data['name'] = body['name']
            else:
                user_data['name'] = event['requestContext']['authorizer']['name']
            
            # Upsert the user document
            result = users_collection.update_one(
                {'_id': user_id},
                {'$set': user_data},
                upsert=True
            )
            
            if result.upserted_id:
                # User was created (upserted), fetch the newly created document
                created_user = users_collection.find_one({'_id': user_id})
                return {
                    'statusCode': 201,
                    'body': json.dumps(parse_json(created_user)) # Return the full user data
                }
            else:
                # User was updated (not upserted)
                return {
                    'statusCode': 200,
                    'body': json.dumps({
                        'message': 'User updated',
                        'userId': user_id,
                        'name': user_data['name'] # Or fetch and return updated doc if needed
                    })
                }
            
        elif http_method == 'PATCH':
            # Update specific user fields
            body = json.loads(event['body'])
            
            if 'name' not in body:
                return {
                    'statusCode': 400,
                    'body': json.dumps({'message': 'Name field is required'})
                }
            
            # Update only the name field
            result = users_collection.update_one(
                {'_id': user_id},
                {'$set': {'name': body['name']}}
            )
            
            if result.modified_count > 0:
                return {
                    'statusCode': 200,
                    'body': json.dumps({
                        'message': 'Name updated successfully',
                        'name': body['name']
                    })
                }
            else:
                return {
                    'statusCode': 404,
                    'body': json.dumps({'message': 'User not found'})
                }
            
        else:
            return {
                'statusCode': 405,
                'body': json.dumps({'message': 'Method not allowed'})
            }
            
    except Exception as e:
        print(f"Error: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps({'message': 'Internal server error'})
        }
