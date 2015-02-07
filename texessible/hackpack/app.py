from twilioMaps import *
import re
 
from flask import Flask
from flask import render_template
from flask import url_for
from flask import request
import sendgrid
 
from twilio import twiml
from twilio.util import TwilioCapability

import forecastio

from geopy import geocoders

import twilioMaps
import feedparser
 
# Declare and configure application
app = Flask(__name__, static_url_path='/static')
app.config.from_pyfile('local_settings.py')

#GET THIS SHIT FROM BODY OF MSG
#ie. "Email to david.hong42@gmail.com Re: 'Nyaa' hihi"
d = feedparser.parse('http://news.google.com/news?pz=1&cf=all&ned=us&hl=en&output=rss')

# Voice Request URL
@app.route('/voice', methods=['GET', 'POST'])
def voice():
    response = twiml.Response()
    response.say("Congratulations! You deployed the Twilio Hackpack "
                 "for Heroku and Flask.")
    return str(response)
 
 
@app.route('/sms', methods=['POST'])
def sms():
    text_body = ""
    subject = ""
    email_address = ""

    c = '"'
    c += "'"
    response = twiml.Response()
    body = request.form['Body']
    bodyList = body.split( )
    f = ""
    toCity =""
    to = ""
    i = 1
    city =""
    country = ""
    j = 1
    api_key = "73d4e3aa68e68f9d169121c88304c3dd"

    sg_username = "ambiguousoup"
    sg_password = "Bamboozled123"
    sg = sendgrid.SendGridClient(sg_username, sg_password)

    for k in range (0, len(bodyList)):
        bodyList[k] = bodyList[k].lower()
    if bodyList[0] == "email":
        sg_username = bodyList[1]
        sg_password = bodyList[2]
        user_name = bodyList[3]
        user_email = bodyList[4]
        user_from = user_email

        bodyList = bodyList[5:] 
        if bodyList[1] == "to":
            if "@" in bodyList[2]:
                email_address = bodyList[2]
                if 're:' in bodyList[3]:
                    a = len(bodyList)-1
                    while bodyList[a][-1] not in c and a > 2:
                        a = a - 1
                    b = a + 1
                    while b < len(bodyList):
                        text_body += bodyList[b] + " "
                        b +=1
                    for b in range(3, a+1):
                        subject += bodyList[b] + " "
        message = sendgrid.Mail()
        message.add_to(email_address)
        message.set_subject(subject)
        message.set_html(text_body)
        message.set_from(user_from)
        sg.send(message)
        respons = "sucessful email sent!"
    elif bodyList[0] == "directions":
        if bodyList[i] != "to":
            while i<len(bodyList) and bodyList[i] != "to":
                f += bodyList[i] + " "
                i+=1
            i+=1
            while i<len(bodyList) and bodyList[i] != ",":
                toCity += bodyList[i] + " "
                i+=1
            i+= 1
            while i<len(bodyList):
                to += bodyList[i] + " "
                i+=1
            bob = Directions(f,toCity,to)
            string = bob.printDirections()
            if(len(string)>1000):
                respons = string[0:1000]
            else:
                respons = string
        else:
            respons = "error, directions reading not recognized"
    elif bodyList[0] == "weather":
        if bodyList[1] != ",":
            while j<len(bodyList) and bodyList[j] != ",":
                city += bodyList[j]
                j+=1
            gn = geocoders.GeoNames(country, "d22hong")
            lat = gn.geocode(city,True) [1] [0];
            long = gn.geocode(city,True) [1] [1];
            forecast = forecastio.load_forecast(api_key, lat, long)
            respons = forecast.currently().summary + " " + str(forecast.currently().temperature) + " C"
    elif bodyList[0] == "news" and len(bodyList) == 1:
        respons = getSites()
    elif bodyList[0] == "news" and bodyList[1].isdigit():
        respons = getLinks(n=int(bodyList[1]))
    else:
        respons = "incorrect messgae"
    response.sms(respons)
    return str(response)

# Twilio Client demo template
@app.route('/client')
def client():
    configuration_error = None
    for key in ('TWILIO_ACCOUNT_SID', 'TWILIO_AUTH_TOKEN', 'TWILIO_APP_SID',
                'TWILIO_CALLER_ID'):
        if not app.config.get(key, None):
            configuration_error = "Missing from local_settings.py: " \
                                  "{0}".format(key)
            token = None
 
    if not configuration_error:
        capability = TwilioCapability(app.config['TWILIO_ACCOUNT_SID'],
                                      app.config['TWILIO_AUTH_TOKEN'])
        capability.allow_client_incoming("joey_ramone")
        capability.allow_client_outgoing(app.config['TWILIO_APP_SID'])
        token = capability.generate()
    params = {'token': token}
    return render_template('client.html', params=params,
                           configuration_error=configuration_error)
 
 
@app.route('/client/incoming', methods=['POST'])
def client_incoming():
    try:
        from_number = request.values.get('PhoneNumber', None)
 
        resp = twiml.Response()
 
        if not from_number:
            resp.say("Your app is missing a Phone Number. "
                     "Make a request with a Phone Number to make outgoing "
                     "calls with the Twilio hack pack.")
            return str(resp)
 
        if 'TWILIO_CALLER_ID' not in app.config:
            resp.say(
                "Your app is missing a Caller ID parameter. "
                "Please add a Caller ID to make outgoing calls with Twilio "
                "Client")
            return str(resp)
 
        with resp.dial(callerId=app.config['TWILIO_CALLER_ID']) as r:
            # If we have a number, and it looks like a phone number:
            if from_number and re.search('^[\d\(\)\- \+]+$', from_number):
                r.number(from_number)
            else:
                r.say("We couldn't find a phone number to dial. Make sure "
                      "you are sending a Phone Number when you make a "
                      "request with Twilio Client")
        return str(resp)
    except:
        resp = twiml.Response()
        resp.say("An error occurred. Check your debugger at twilio dot com "
                 "for more information.")
        return str(resp)
 
 
# Installation success page
@app.route('/')
def index():
    params = {
        'Voice Request URL': url_for('.voice', _external=True),
        'SMS Request URL': url_for('.sms', _external=True),
        'Client URL': url_for('.client', _external=True)}
    return render_template('index.html', params=params,
                           configuration_error=None)

def getSites():
    x = ""
    for i in range (0, 10):
        x += (str(i+1)+": " + d.entries[i]['title']+"\n")
    return x

def getLinks(n = 0):
    x = (str(n+1)+": "+ d.entries[n]['link']+"\n")
    return x
