/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dms.helper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author Nur Muhamad
 */
public class RestClient {
    
    private static final String  filename = "config.properties";
    private static Connection con;
    private static PreparedStatement  ps;
    private static ResultSet rs ;   
    static final Logger log = Logger.getLogger(RestClient.class.getName());
    
    
    private static  String  RestLogin() throws IOException{
        
        Properties prop = new Properties();
        InputStream input = new FileInputStream(filename);
        prop.load(input);        
        
        String restUser = prop.getProperty("restuser");     
        String restPassword = prop.getProperty("restpassword");     
        
        String encoding = Base64.getEncoder().encodeToString((restUser+":"+restPassword).getBytes());        
        return encoding;
    }
    
    public boolean IsValidDocumet(String DocId) throws IOException{
        
        
        Properties prop = new Properties();
        InputStream input = new FileInputStream(filename);
        prop.load(input);        
        String location = prop.getProperty("resthost"); 
        
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add( new BasicNameValuePair("docId", DocId));
        String url = location + "/services/rest/document/isValid/";
        
        CloseableHttpClient client = HttpClientBuilder.create().build();        
        String queryString = URLEncodedUtils.format(urlParameters, "UTF-8");
	url += ("?" + queryString);
        
        HttpGet get = new HttpGet(url);
        get.addHeader("accept", "application/json");
        get.addHeader("Authorization", "Basic " + RestLogin());
        CloseableHttpResponse response = client.execute(get);
            
        // Status Code
        int statusCode = response.getStatusLine().getStatusCode();            
        String  responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8.name());
        client.getConnectionManager().shutdown();
        
        log.info(responseBody);
        JsonObject json = new JsonObject();
        json.addProperty("responseCode", statusCode);
        json.addProperty("docId", DocId);                        
                
        boolean result = Boolean.valueOf(responseBody);
        if(result){
           json.addProperty("message", "Document IsValid");
           json.addProperty("status", result);           
        }else{
           json.addProperty("message", "Document NotValid");
           json.addProperty("status", result); 
        }  
        
        log.info(json.toString());
        return result;
    }
    
    public  JsonObject CreateSimpleDocumet(File Srcfile,JsonObject jsonObj,int fileId) throws SQLException, ClassNotFoundException, FileNotFoundException, IOException {
        
        log.info(Srcfile.getAbsolutePath());
        
        JsonParser parser = new JsonParser();
        JsonElement instansi = jsonObj.get("instansi");
        JsonElement nip = jsonObj.get("nip");
        JsonElement jenis_doc = jsonObj.get("jenis_doc");
        
        
        String fileName = Srcfile.getName();
        Properties prop = new Properties();
        InputStream input = new FileInputStream(filename);
        prop.load(input);        
        
        String location = prop.getProperty("analisis");     
        String RestHost = prop.getProperty("resthost");     
        
        File file = new File(location,fileName);
        FileBody fileBody = new FileBody(file, ContentType.DEFAULT_BINARY);

        String fld = "/okm:root" + "/"+instansi.getAsString() + "/" +  nip.getAsString() + "/" 
                +jenis_doc.getAsString().substring(0, jenis_doc.getAsString().length() - 1);   
        
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addPart("content", fileBody);
        builder.addPart("docPath", new StringBody(fld + "/" + fileName));
        HttpEntity entity = builder.build();

        HttpPost request = new HttpPost(RestHost +"/services/rest/document/createSimple/");
        request.setHeader("Accept", "application/json");
        request.addHeader("Authorization", "Basic " + RestLogin());
        request.setEntity(entity);
        JsonObject json = new JsonObject();                

        CloseableHttpClient client = HttpClientBuilder.create().build();
        try {
            
            CloseableHttpResponse response = client.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();            
            
            // Response Body
            String  responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8.name());
            client.getConnectionManager().shutdown();
            
            if(statusCode == 200){
                
                JsonObject jo = (JsonObject) parser.parse(responseBody);
                JsonElement uuid = jo.get("uuid");
                JsonElement path = jo.get("path");
                JsonElement created = jo.get("created");  
                
                String query = "update proyekocr.file_bucket SET status=?,"
                        + " dms_file_name=?, dms_created=NOW() where file_bucket.id=?";                        
                con = DBConnection.getConnection();               
                ps = con.prepareStatement(query);
                ps.setInt(1, 2);
                ps.setString(2, uuid.getAsString());
                ps.setInt(3,fileId);            
                
                json.addProperty("statusCode", statusCode);
                json.addProperty("uuid", uuid.getAsString());
                json.addProperty("path", path.getAsString());
                json.addProperty("created", created.getAsString());
                json.addProperty("message", "Document has been created successfully!");
                json.addProperty("status", true);  
                log.info(json.toString());
            }else{
                json.addProperty("responseCode", statusCode);
                json.addProperty("message", responseBody);
                json.addProperty("status", false);  
                log.info(json.toString());
            }
        } catch (IOException e) {
            log.info(e.getMessage());
        }  
        
        return json;
    }
    
    public  JsonObject CreateSimpleFolder(JsonObject jsonObject) throws UnsupportedEncodingException, IOException{
        
        Properties prop = new Properties();
        InputStream input = new FileInputStream(filename);
        prop.load(input);        
        
        JsonElement instansi = jsonObject.get("instansi");
        JsonElement nip = jsonObject.get("nip");
        JsonElement jenis_doc = jsonObject.get("jenis_doc");
        
        String fld1 = "/okm:root" + "/"+instansi.getAsString() + "/" +  nip.getAsString()
                + "/" +jenis_doc.getAsString().substring(0, jenis_doc.getAsString().length() - 1);           
        String fld2 = "/okm:root" + "/"+instansi.getAsString() + "/" +  nip.getAsString();  
        String fld3 = "/okm:root" + "/"+instansi.getAsString();  
        JsonObject json = new JsonObject();        
        try {
            // try create folder jenis doc
            if(!IsValidFolder(fld3)){                
                log.info("Not Valid  folder : " +  fld3);                
                json = RestPost(fld3);
                log.info("try create folder : " + fld3);
                if(!IsValidFolder(fld2)){
                    json = RestPost(fld2);
                    log.info("try create folder : " + fld2);
                    if(!IsValidFolder(fld1)){
                        json = RestPost(fld1);
                        log.info("try create folder : " + fld1);      
                    }
                }else{
                    if(!IsValidFolder(fld1)){
                        json = RestPost(fld1);
                        log.info("try create folder : " + fld1);                              
                    }
                }                
            }else{
                log.info("Valid  folder : " +  fld3);                
                if(!IsValidFolder(fld2)){
                    json = RestPost(fld2);
                    log.info("try create folder : " + fld2);
                    if(!IsValidFolder(fld1)){
                        json = RestPost(fld1);
                        log.info("try create folder : " + fld1);                                  
                    }
                }else{                    
                    if(!IsValidFolder(fld1)){
                        json = RestPost(fld1);
                        log.info("try create folder : " + fld1);                                            
                    }else{
                        log.info("try create folder with all status true");
                        json.addProperty("responseCode", "200");
                        json.addProperty("path", fld1);                        
                        json.addProperty("message", "All Folder already exist"); 
                        json.addProperty("status", true); 
                        json.addProperty("uuid", -1);
                        json.addProperty("created", -1);
                
                    }                    
                }
            }
            JsonElement responseStatus = json.get("status");
            boolean status = responseStatus.getAsBoolean();
            JsonElement responseCode = json.get("responseCode");
            JsonElement message = json.get("message");
                                    
            if(status){
                JsonElement uuid = json.get("uuid");
                JsonElement path = json.get("path");
                JsonElement created = json.get("created");
                
                json.addProperty("responseCode", responseCode.getAsString());
                json.addProperty("uuid", uuid.getAsString());
                json.addProperty("path", path.getAsString());
                json.addProperty("created", created.getAsString()); 
                json.addProperty("message", "Folder has been created successfully!");
                json.addProperty("status", true);  
                log.info(json.toString());
                
            }else{
                
                json.addProperty("responseCode", responseCode.getAsString());
                json.addProperty("message", message.getAsString());
                json.addProperty("status", false);  
                log.info(json.toString());
            } 
        } catch (IOException e) {
            log.info(e.getMessage());
        }
        
        return json;
    }
    
    private  static JsonObject RestPost(String fld) throws FileNotFoundException, IOException{
        
        Properties prop = new Properties();
        InputStream input = new FileInputStream(filename);
        prop.load(input);        
        String location = prop.getProperty("resthost");         
        JsonParser parser = new JsonParser();   
        
        StringEntity entity = new StringEntity(fld);
        String url  = location + "/services/rest/folder/createSimple";
        
        HttpPost request = new HttpPost(url);
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Authorization", "Basic " + RestLogin());
        request.addHeader("Accept", "application/json");
              
        request.setEntity(entity);
        CloseableHttpClient  client =  HttpClients.createDefault();

        JsonObject json = new JsonObject();
                
        try {
            
            CloseableHttpResponse  response = client.execute(request);                       
            String  responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8.name());
            
            int statusCode = response.getStatusLine().getStatusCode();       
                
            if(statusCode == 200){
                JsonObject jo = (JsonObject) parser.parse(responseBody);
                JsonElement uuid = jo.get("uuid");
                JsonElement path = jo.get("path");
                JsonElement created = jo.get("created");                
                
                json.addProperty("responseCode", statusCode);
                json.addProperty("uuid", uuid.getAsString());
                json.addProperty("path", path.getAsString());
                json.addProperty("created", created.getAsString()); 
                json.addProperty("message", "Folder has been created successfully!");
                json.addProperty("status", true);  
                log.info(json.toString());
            }else{
                json.addProperty("responseCode", statusCode);
                json.addProperty("message", responseBody);
                json.addProperty("status", false);  
                log.info(json.toString());
            }
            
            client.getConnectionManager().shutdown();
            

        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return json;
    }
    
    public  boolean IsValidFolder(String fldId) throws IOException{
        
        Properties prop = new Properties();
        InputStream input = new FileInputStream(filename);
        prop.load(input);        
        String location = prop.getProperty("resthost"); 
        
        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add( new BasicNameValuePair("fldId", fldId));
        String url = location + "/services/rest/folder/isValid/";
        
        CloseableHttpClient client = HttpClientBuilder.create().build();        
        String queryString = URLEncodedUtils.format(urlParameters, "UTF-8");
	url += ("?" + queryString);
        
        HttpGet get = new HttpGet(url);
        get.addHeader("accept", "application/json");
        get.addHeader("Authorization", "Basic " + RestLogin());
        CloseableHttpResponse response = client.execute(get);
            
        // Status Code
        int statusCode = response.getStatusLine().getStatusCode();            
        String  responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8.name());
        client.getConnectionManager().shutdown();
        JsonObject json = new JsonObject();
        
        json.addProperty("responseCode", statusCode);
        json.addProperty("fldId", fldId);
        boolean result = Boolean.valueOf(responseBody);
        
        if(result){
           json.addProperty("message", "Folder IsValid");
           json.addProperty("status", result);
           
        }else{
           json.addProperty("message", "Folder NotValid");
           json.addProperty("status", result); 
        }  
        
        log.info(json.toString());
        
        return result;
    }
    
    
}
