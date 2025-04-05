import os
import json
import firebase_admin
from firebase_admin import credentials, auth

# Initialize Firebase Admin SDK
cred_json = json.loads(os.environ['FIREBASE_CREDENTIALS'])
cred = credentials.Certificate(cred_json)
firebase_admin.initialize_app(cred)

def lambda_handler(event, context):
    try:
        # Get the Authorization header from the request
        if 'Authorization' not in event['headers']:
            raise Exception('No Authorization header')
            
        token = event['headers']['Authorization'].split(' ')[1]
        
        # Verify the Firebase token
        decoded_token = auth.verify_id_token(token)
        
        # Generate the IAM policy
        return {
            "principalId": decoded_token['uid'],
            "policyDocument": {
                "Version": "2012-10-17",
                "Statement": [
                    {
                        "Action": "execute-api:Invoke",
                        "Effect": "Allow",
                        "Resource": event['methodArn']
                    }
                ]
            },
            "context": {
                "uid": decoded_token['uid'],
                "email": decoded_token.get('email', ''),
                "name": decoded_token.get('name', '')
            }
        }
    except Exception as e:
        print(f"Authorization error: {str(e)}")
        raise Exception('Unauthorized') 