package com.iamuse.admin.util;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class ZohoContactsSynUtil {
	
	//public static void main(String args[]) throws ClientProtocolException, IOException {
	
	public static void zohoContactSync(String emailId) throws ClientProtocolException, IOException {
		
        String productImaUse="https://campaigns.zoho.com/api/addlistsubscribersinbulk";
		   productImaUse+="?scope=CampaignsAPI";
		   productImaUse+= "&resfmt=JSON";
		   productImaUse+= "&authtoken=33e8d3683c3a13bcaeff859002297da8";
		   productImaUse+= "&listkey=d1ec5d9ab01c9c07011e644c02d82e148060bef678df20a0";
		   productImaUse+= "&emailids="+emailId;
		
		/*String productImaUse="https://campaigns.zoho.com/api/json/listsubscribe";
		   productImaUse+="?scope=CampaignsAPI";
		   productImaUse+= "&resfmt=JSON";
		   productImaUse+= "&authtoken=33e8d3683c3a13bcaeff859002297da8";
		   productImaUse+= "&listkey=d1ec5d9ab01c9c07011e644c02d82e148060bef678df20a0";
		   productImaUse+= "&contactinfo="+"test@gmail.com";*/
	
	HttpHeaders headers = new HttpHeaders();
	headers.add("Content-Type", "application/json");
	HttpEntity<String> req = new HttpEntity<String>(headers);
	
	RestTemplate restTemplate = new RestTemplate();
	ResponseEntity<Object> responseForLeads = restTemplate.exchange(productImaUse, HttpMethod.GET, req,Object.class);
	System.out.println("Zoho Marketinghub leads added=" + responseForLeads.getStatusCode());
	}

}