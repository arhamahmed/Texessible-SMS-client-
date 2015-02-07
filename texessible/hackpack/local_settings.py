'''
Configuration Settings
'''

''' Uncomment to configure using the file.  
WARNING: Be careful not to post your account credentials on GitHub.

TWILIO_ACCOUNT_SID = "ACxxxxxxxxxxxxx" 
TWILIO_AUTH_TOKEN = "yyyyyyyyyyyyyyyy"
TWILIO_APP_SID = "APzzzzzzzzz"
TWILIO_CALLER_ID = "+17778889999"
'''

# Begin Heroku configuration - configured through environment variables.
import os
TWILIO_ACCOUNT_SID = os.environ.get('AC3dcb8a0eaa67cc79db6bb88045982ce3', None)
TWILIO_AUTH_TOKEN = os.environ.get('c94dce07725aeabcdf7c2404cad6c66c', None)
TWILIO_CALLER_ID = os.environ.get('+18737000071', None)
