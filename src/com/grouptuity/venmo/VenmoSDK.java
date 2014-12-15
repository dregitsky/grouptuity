/*	The MIT License (MIT)
 *	
 *	Copyright (c) 2014 Venmo Inc.
 *	
 *	Permission is hereby granted, free of charge, to any person obtaining a copy of
 *	this software and associated documentation files (the "Software"), to deal in
 *	the Software without restriction, including without limitation the rights to
 *	use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 *	the Software, and to permit persons to whom the Software is furnished to do so,
 *	subject to the following conditions:
 *	The above copyright notice and this permission notice shall be included in all
 *	copies or substantial portions of the Software.
 *	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *	FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *	COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 *	IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *	CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *	MODIFIED FOR GROUPTUITY
 */

package com.grouptuity.venmo; //Replace this with the name of your package 

import java.io.*;
import java.net.URLEncoder;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.grouptuity.Grouptuity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

public class VenmoSDK
{
	public VenmoSDK(){}

	/* Takes the recipients, amount, and note, and returns an Intent object */
	public static Intent openVenmoPayment(String myAppId, String myAppName, String recipients, String amount, String note, String txn)
	{
		String venmo_uri = "venmosdk://paycharge?txn=" + txn;

    	if(!recipients.equals("")){try{venmo_uri += "&recipients=" + URLEncoder.encode(recipients, "UTF-8");}catch(UnsupportedEncodingException e){Log.e("venmodemo", "cannot encode recipients");}}
    	if(!amount.equals("")){try{venmo_uri += "&amount=" + URLEncoder.encode(amount, "UTF-8");}catch(UnsupportedEncodingException e){Log.e("venmodemo", "cannot encode amount");}}
    	if(!note.equals("")){try{venmo_uri += "&note=" + URLEncoder.encode(note, "UTF-8");}catch(UnsupportedEncodingException e){Log.e("venmodemo", "cannot encode note");}}

    	try{venmo_uri+= "&app_id=" + URLEncoder.encode(myAppId, "UTF-8");}catch(UnsupportedEncodingException e){Log.e("venmodemo", "cannot encode app ID");}
    	try{venmo_uri+= "&app_name=" + URLEncoder.encode(myAppName, "UTF-8");}catch(UnsupportedEncodingException e){Log.e("venmodemo", "cannot encode app Name");}
    	try{venmo_uri+= "&app_local_id=" + URLEncoder.encode("abcd", "UTF-8");}catch(UnsupportedEncodingException e){Log.e("venmodemo", "cannot encode app local id");}

    	venmo_uri += "&using_new_sdk=true";
    	venmo_uri = venmo_uri.replaceAll("\\+", "%20"); // use %20 encoding instead of +

		return new Intent(Intent.ACTION_VIEW, Uri.parse(venmo_uri));
	}

	/* Takes the recipients, amount, and note, and returns a String representing the URL to visit to complete the transaction */
	public static String openVenmoPaymentInWebView(String myAppId, String myAppName, String recipients, String amount, String note, String txn)
	{
		String venmo_uri = "https://venmo.com/touch/signup_to_pay?txn=" + txn;

		if(!recipients.equals("")){try{venmo_uri += "&recipients=" + URLEncoder.encode(recipients, "UTF-8");}catch(UnsupportedEncodingException e){Log.e("venmodemo", "cannot encode recipients");}}
		if(!amount.equals("")){try{venmo_uri += "&amount=" + URLEncoder.encode(amount, "UTF-8");}catch(UnsupportedEncodingException e){Log.e("venmodemo", "cannot encode amount");}}
		if(!note.equals("")){try{venmo_uri += "&note=" + URLEncoder.encode(note, "UTF-8");}catch(UnsupportedEncodingException e){Log.e("venmodemo", "cannot encode note");}}

		try{venmo_uri+= "&app_id=" + URLEncoder.encode(myAppId, "UTF-8");}catch(UnsupportedEncodingException e){Log.e("venmodemo", "cannot encode app ID");}
    	try{venmo_uri+= "&app_name=" + URLEncoder.encode(myAppName, "UTF-8");}catch(UnsupportedEncodingException e){Log.e("venmodemo", "cannot encode app Name");}
    	try{venmo_uri+= "&app_local_id=" + URLEncoder.encode("abcd", "UTF-8");}catch(UnsupportedEncodingException e){Log.e("venmodemo", "cannot encode app local id");}
    	try{venmo_uri+= "&client=" + URLEncoder.encode("android", "UTF-8");}catch(UnsupportedEncodingException e){Log.e("venmodemo", "cannot encode client=android");}

    	venmo_uri = venmo_uri.replaceAll("\\+", "%20"); // use %20 encoding instead of +
		return venmo_uri;
	}

	//Called once control has been given back to your app - it takes the signed_payload, decodes it, and gives you the response object which 
	//gives you details about the transaction - whether it was successful, the note, the amount, etc. 
	public static VenmoResponse validateVenmoPaymentResponse(String signed_payload)
	{
		String encoded_signature, payload;
		if(signed_payload == null){return new VenmoResponse(null, null, null, "0");}
		try
		{
			String[] encodedsig_payload_array = signed_payload.split("\\.");
			encoded_signature = encodedsig_payload_array[0];
			payload = encodedsig_payload_array[1];
		}
		catch(ArrayIndexOutOfBoundsException e){return new VenmoResponse(null, null, null, "0");}
		
		String decoded_signature = base64_url_decode(encoded_signature);
		String data;
        // check signature 
        String expected_sig = hash_hmac(payload, Grouptuity.VENMO_SECRET, "HmacSHA256");
        
        Log.d("VenmoSDK", "expected_sig using HmacSHA256:" + expected_sig);
        
        VenmoResponse myVenmoResponse;
        
        if (decoded_signature.equals(expected_sig))
        {
            data = base64_url_decode(payload);

            try
            {
                JSONArray response = (JSONArray)JSONValue.parse(data);
                JSONObject obj = (JSONObject)response.get(0);
                String payment_id = obj.get("payment_id").toString();
                String note = obj.get("note").toString();
                String amount = obj.get("amount").toString();
                String success = obj.get("success").toString();
                myVenmoResponse = new VenmoResponse(payment_id, note, amount, success);
            }
            catch(Exception e){Log.d("VenmoSDK", "Exception caught: " + e.getMessage());myVenmoResponse = new VenmoResponse(null, null, null, "0");} 
        }
        else{Log.d("VenmoSDK", "Signature does NOT match");myVenmoResponse = new VenmoResponse(null, null, null, "0");}
        return myVenmoResponse;
	}
	
	
	private static String hash_hmac(String payload, String app_secret, String algorithm)
	{
		try 
		{
		    Mac mac = Mac.getInstance(algorithm);
		    SecretKeySpec secret = new SecretKeySpec(app_secret.getBytes(), algorithm);
		    mac.init(secret);
		    byte[] digest = mac.doFinal(payload.getBytes());
		    String enc = new String(digest);
		    return enc;
		} 
		catch (Exception e) 
		{
		    Log.d("VenmoSDK Error Message Caught", e.getMessage());
		    return "";
		}
	}
	private static String base64_url_decode(String payload)
	{
		String payload_modified = payload.replace('-', '+').replace('_', '/').trim();
		String jsonString = new String(Base64.decode(payload_modified, Base64.DEFAULT));
		
		return jsonString;
	}

	//This is the object returned to you after a transaction has gone through.
	//It tells you whether it was successful, the amount, the note, and the payment id. 
	public static class VenmoResponse
	{
		private String payment_id, note, amount, success;
		public VenmoResponse(String payment_id, String note, String amount, String success)
		{
			this.payment_id = payment_id;
			this.note = note;
			this.amount= amount;
			this.success = success;
		}
		public String getPaymentId(){return payment_id;}
		public String getNote(){return note;}
		public String getAmount(){return amount;}
		public String getSuccess(){return success;}
	}
}