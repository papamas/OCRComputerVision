/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dms.helper;

import com.mysql.jdbc.Connection;
import java.io.IOException;
import java.io.InputStream;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;
import java.io.FileInputStream;

/**
 *
 * @author Nur Muhamad
 */
public class DBConnection {
    
    private static final String  filename = "config.properties";
    private static final String DB_DRIVER_CLASS = "com.mysql.jdbc.Driver";
    private static String DB_URL = "jdbc:mysql://localhost:3306/proyekocr";
    private static String DB_USERNAME = "root";
    private static String DB_PASSWORD = "";    
    static final Logger log = Logger.getLogger(DBConnection.class.getName());

    public static Connection getConnection() throws ClassNotFoundException, SQLException, IOException {
        
        Properties prop = new Properties();
        InputStream input = new FileInputStream(filename);
        Connection con;
        // load the Driver Class
        Class.forName(DB_DRIVER_CLASS);
        //load a properties file from class path, inside static method
        prop.load(input);
        //get the property value and print it out
        DB_URL = "jdbc:mysql://"+prop.getProperty("dbhost")+":3306/"+prop.getProperty("database");
        DB_USERNAME = prop.getProperty("dbuser");
        DB_PASSWORD = prop.getProperty("dbpassword");            
        // create the connection now
        con = (Connection) DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
        log.info("DB Connection created successfully");
             
        return con;        
    }
    
}
