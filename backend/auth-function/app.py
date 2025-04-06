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
        
        # Extract API Gateway ARN components
        # Example ARN: arn:aws:execute-api:us-west-2:123456789012:abcdef123/dev/GET/user
        arn_parts = event['methodArn'].split(':')
        api_gateway_arn_tmp = arn_parts[5].split('/')
        aws_account_id = arn_parts[4]
        region = arn_parts[3]
        rest_api_id = api_gateway_arn_tmp[0]
        stage = api_gateway_arn_tmp[1]
        # Construct the resource path allowing all methods for this specific resource
        # Example: arn:aws:execute-api:us-west-2:123456789012:abcdef123/dev/*/user
        resource = f"arn:aws:execute-api:{region}:{aws_account_id}:{rest_api_id}/{stage}/*/{'/'.join(api_gateway_arn_tmp[3:])}"

        # Generate the IAM policy
        return {
            "principalId": decoded_token['uid'],
            "policyDocument": {
                "Version": "2012-10-17",
                "Statement": [
                    {
                        "Action": "execute-api:Invoke",
                        "Effect": "Allow",
                        #"Resource": event['methodArn'] # Original restrictive resource
                        "Resource": resource # Allow all methods for this path
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