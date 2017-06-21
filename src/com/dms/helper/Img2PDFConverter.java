/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dms.helper;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * @author Nur Muhamad
 */
public class Img2PDFConverter {
    
    private static String  filename = "config.properties";
    
    public void convertImg2PDF() throws FileNotFoundException, DocumentException, BadElementException, IOException{
        
        String home = System.getProperty("user.home");
        
        String input =  home + File.separator + "skpengalihan.jpg"; // .gif and .jpg are ok too!
        String output = home + File.separator + "sk_pengalihan.pdf";

        Document document = new Document();
        FileOutputStream fos = new FileOutputStream(output);
        PdfWriter writer = PdfWriter.getInstance(document, fos);
        writer.open();
        document.open();
        Image image = Image.getInstance(input);
        float scaler = ((document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin() - 0) / image.getWidth()) * 100;
        image.scalePercent(scaler);
        document.add(image);
        document.close();
        writer.close();
    }
    
}
