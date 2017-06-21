/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dms.posted;

import com.dms.helper.DBConnection;
import com.dms.helper.RestClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.itextpdf.text.DocumentException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nur Muhamad
 */
public class DMSPosted {

    /**
     * @param args the command line arguments
     */
    private static String  filename = "config.properties";
    static final Logger log = Logger.getLogger(DMSPosted.class.getName());
    private static Connection con;
    private static PreparedStatement  ps;
    private static ResultSet rs;        
        
    
    public static void main(String[] args) throws DocumentException, FileNotFoundException, IOException {
        
        RestClient Rclient = new RestClient();
        Properties prop = new Properties();
        InputStream input = new FileInputStream(filename);
        prop.load(input);        
        String location = prop.getProperty("analisis"); 
        JsonParser parser = new JsonParser();
        JsonObject jo,json;
        JsonElement message;        
        try {
            
            HashMap<Integer,String> analisFileName = getAnalisFileName();
            Set<Integer> keys = analisFileName.keySet();
            boolean status = false;
            int responseCode;
            String uuid;
            
            for (Integer key : keys) { 
                jo = (JsonObject) parser.parse(getInstansi(key).toString()); 
                System.out.println(jo);
                File file = new File(location + File.separator + analisFileName.get(key));
                
                JsonElement instansi  = jo.get("instansi");
                JsonElement nip       = jo.get("nip");
                JsonElement jenis_doc = jo.get("jenis_doc");
                              
                if(!instansi.isJsonNull() &&  !nip.isJsonNull() && !jenis_doc.isJsonNull()){
                    json = Rclient.CreateSimpleFolder(jo); 
                    JsonElement responseStatus = json.get("status"); 
                    status =  responseStatus.getAsBoolean();                    
                }
                
                //jo = Rclient.CreateSimpleFolder("/okm:root/test");
                //jo = Rclient.IsValidDocumet("00190f60-0cee-4761-bebe-b49a7addb9be");
                //Rclient.IsValidFolder("fde0d4f4-14b2-4a12-a13b-f02e3fe51bb7");
                // Try create file
                if(status){
                    jo = Rclient.CreateSimpleDocumet(file,jo,key);
                    message = jo.get("message");
                    status = jo.get("status").getAsBoolean();
                    log.info(message.getAsString());
                    if(status){
                        // jika berhasil folder berarti ada update status
                        uuid  = jo.get("uuid").getAsString();
                        log.info(uuid);
                        updateStatus(key,uuid);
                    }else{
                        jo = Rclient.CreateSimpleFolder(jo); 
                        status = jo.get("status").getAsBoolean();
                    }
                }
                /*
                else{
                    // jika folder tidak ada response code 500
                    responseCode  = jo.get("responseCode").getAsInt();
                    if(responseCode == 500){
                        // create folder baru dgn nama instansi                        
                        jo = Rclient.CreateSimpleFolder(jo);
                        message = jo.get("message");
                        status = jo.get("status").getAsBoolean();
                        log.info(message.getAsString());
                        if(status){
                            // jika berhasil create folder create ulang dokumen
                            jo = Rclient.CreateSimpleDocumet(file,jo,key);
                            message = jo.get("message");
                            status = jo.get("status").getAsBoolean();
                            if(status){
                                // jika create berhaasil
                                uuid  = jo.get("uuid").getAsString();
                                log.info(uuid);
                               updateStatus(key,uuid);
                            }
                            log.info(message.getAsString());
                        }
                        
                    }
                }
                */
                
            }
        
            
            } catch (ClassNotFoundException | SQLException  | IOException ex) {
            Logger.getLogger(DMSPosted.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static HashMap<Integer,String> getAnalisFileName() throws ClassNotFoundException, SQLException, IOException{

        HashMap<Integer,String> FileScanner = new HashMap<>();        
            String query = "select id,analis_file_name FROM proyekocr.file_bucket WHERE status='1'";
            con = DBConnection.getConnection();
            ps = con.prepareStatement(query);
            
            rs = ps.executeQuery();
                while (rs.next()) {
                    FileScanner.put(rs.getInt("id"),rs.getString("analis_file_name"));     
                }
            ps.close();
            con.close();
            return FileScanner;
    } 
    
    private  static JsonObject  getInstansi(int fId) throws ClassNotFoundException, SQLException, IOException{

        String query = "select instansi,jenis_doc,nip,id FROM proyekocr.file_bucket WHERE file_bucket.id=?";
        String instansi,nip,jenis_doc = null;
        int id;
        JsonObject json = new JsonObject();                
        
        con = DBConnection.getConnection();
        ps = con.prepareStatement(query);
        ps.setInt(1, fId);
        rs = ps.executeQuery();
        
        if(rs.first()){    
            instansi = rs.getString("instansi");
            nip      = rs.getString("nip");
            jenis_doc = rs.getString("jenis_doc");
            id   = rs.getInt("id");
            
            json.addProperty("instansi", instansi);
            json.addProperty("nip", nip);
            json.addProperty("jenis_doc", jenis_doc);
            json.addProperty("id", id);
            
        }
        ps.close();
        con.close();
        
        log.info("Table file bucket update dms");    
        return json;
    }
    
    private  static void  updateStatus(int fId, String fileName) throws ClassNotFoundException, SQLException, IOException{

        String query = "update proyekocr.file_bucket SET dms_file_name = ?,"
                + "status = ? , dms_created=NOW() WHERE file_bucket.id=?";
        con = DBConnection.getConnection();
        ps = con.prepareStatement(query);
        ps.setString(1, fileName);
        ps.setInt(2, 2);
        ps.setInt(3, fId);
        int updated =  ps.executeUpdate();
        ps.close();
        con.close();
       
        log.info("Table file bucket update dms");    
   
    }
 
    
    
}

    