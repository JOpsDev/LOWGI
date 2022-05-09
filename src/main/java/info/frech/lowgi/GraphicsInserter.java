package info.frech.lowgi;
/*************************************************************************
 *
 *  The Contents of this file are made available subject to the terms of
 *  the BSD license.
 *
 *  Copyright 2000, 2010 Oracle and/or its affiliates.
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. Neither the name of Sun Microsystems, Inc. nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 *  OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 *  TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 *  USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *************************************************************************/

import com.sun.star.awt.Size;
import com.sun.star.awt.XBitmap;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XDesktop;
import com.sun.star.graphic.XGraphic;
import com.sun.star.graphic.XGraphicProvider;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.lib.uno.adapter.ByteArrayToXInputStreamAdapter;
import com.sun.star.text.ControlCharacter;
import com.sun.star.text.TextContentAnchorType;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import mediautil.image.jpeg.*;

import java.io.*;
import java.util.Arrays;


public class GraphicsInserter {
    public static void main(String args[]) {
        if (args.length < 1) {
            System.out.println("usage: java -jar GraphicsInserter.jar \"<path to directory>\"");
            System.exit(1);
        }

        try {

            // bootstrap UNO and get the remote component context. The context can
            // be used to get the service manager
            XComponentContext xContext = com.sun.star.comp.helper.Bootstrap.bootstrap();
            System.out.println("Connected to a running office ...");

            // get the remote office service manager
            XMultiComponentFactory xMCF = xContext.getServiceManager();

            /* A desktop environment contains tasks with one or more
               frames in which components can be loaded. Desktop is the
               environment for components which can instantiate within
               frames. */
            XDesktop xDesktop = UnoRuntime.queryInterface(XDesktop.class, xMCF.createInstanceWithContext("com.sun.star.frame.Desktop", xContext));

            XComponentLoader xCompLoader = UnoRuntime.queryInterface(XComponentLoader.class, xDesktop);

            // Load a Writer document, which will be automatically displayed
            XComponent xComp = xCompLoader.loadComponentFromURL("private:factory/swriter", "_blank", 0, new PropertyValue[0]);

            // Querying for the interface XTextDocument on the xcomponent
            XTextDocument xTextDoc = UnoRuntime.queryInterface(XTextDocument.class, xComp);

            // Querying for the interface XMultiServiceFactory on the xtextdocument
            XMultiServiceFactory xMSFDoc = UnoRuntime.queryInterface(XMultiServiceFactory.class, xTextDoc);

            // Getting the text
            XText xText = xTextDoc.getText();

            // Getting the cursor on the document
            XTextCursor xTextCursor = xText.createTextCursor();


            try {
                // Creating a string for the graphic url
                File sourceDir = new File(args[0]);
                File[] files = sourceDir.listFiles((file, name) -> name.toLowerCase().endsWith(".jpg"));

                for (File file : files) {
                    embedGraphics(xContext, xMCF, xMSFDoc, xText, xTextCursor, file);
                }
            } catch (Exception exception) {
                exception.printStackTrace();

            }

        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }

        System.exit(0);
    }

    private static void embedGraphics(XComponentContext xContext, XMultiComponentFactory xMCF, XMultiServiceFactory xMSFDoc, XText xText, XTextCursor xTextCursor, File file) throws com.sun.star.uno.Exception, IOException {

        System.out.println("now " + file);

        Object oGraphic = null;
        try {
            // Creating the service GraphicObject
            oGraphic = xMSFDoc.createInstance("com.sun.star.text.TextGraphicObject");
        } catch (Exception exception) {
            System.out.println("Could not create instance");
            exception.printStackTrace();
        }

        // Querying for the interface XTextContent on the GraphicObject
        XTextContent xTextContent = UnoRuntime.queryInterface(XTextContent.class, oGraphic);

        // Querying for the interface XPropertySet on GraphicObject
        XPropertySet xPropSet = UnoRuntime.queryInterface(XPropertySet.class, oGraphic);

        StringBuffer sUrl = new StringBuffer("file://" + file.getCanonicalPath());
        System.out.println("insert graphic \"" + sUrl + "\"");

        XGraphicProvider xGraphicProvider = UnoRuntime.queryInterface(XGraphicProvider.class, xMCF.createInstanceWithContext("com.sun.star.graphic.GraphicProvider", xContext));

//        PropertyValue[] aMediaProps = new PropertyValue[]{new PropertyValue()};
//        aMediaProps[0].Name = "URL";
//        aMediaProps[0].Value = sUrl.toString();

//        byte[] imageBytes = bytesFromFile(file);

        byte[] imageBytes = loadAndRotateImage(file);

        PropertyValue[] propValues = new PropertyValue[]{new PropertyValue()};
        propValues[0].Name = "InputStream";
        propValues[0].Value = new ByteArrayToXInputStreamAdapter(imageBytes);

        XGraphic xGraphic = UnoRuntime.queryInterface(XGraphic.class, xGraphicProvider.queryGraphic(propValues));

        // Setting the anchor type
        xPropSet.setPropertyValue("AnchorType", TextContentAnchorType.AS_CHARACTER);
        //xPropSet.setPropertyValue("AnchorType", TextContentAnchorType.AT_CHARACTER);

        //xPropSet.setPropertyValue("SurroundContour", false);
        //xPropSet.setPropertyValue("ContourOutside", true);


        // Setting the graphic url
        xPropSet.setPropertyValue("Graphic", xGraphic);

        XBitmap xBitmap = (XBitmap) UnoRuntime.queryInterface(XBitmap.class, xGraphic);
        Size sz = xBitmap.getSize();
        double width = sz.Width;
        double ratio = width / sz.Height;

        byte type = xGraphic.getType();

        System.out.println(sz.Width + "/" + sz.Height + " = " + ratio);

        int fixedWidth = 10000;
        xPropSet.setPropertyValue("Width", Integer.valueOf(fixedWidth));
        //xPropSet.setPropertyValue("Height", Integer.valueOf(fixedWidth));
        xPropSet.setPropertyValue("Height", Integer.valueOf((int) (fixedWidth / ratio)));


        try {
            // Inserting the content
            xText.insertControlCharacter(xTextCursor, ControlCharacter.PARAGRAPH_BREAK, false);
            xText.insertTextContent(xTextCursor, xTextContent, false);
            xText.insertControlCharacter(xTextCursor, ControlCharacter.PARAGRAPH_BREAK, false);
        } catch (Exception exception) {
            System.out.println("Could not insert Content");
            exception.printStackTrace(System.err);
        }


    }

    public static byte[] bytesFromFile(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            fis.close();
            return bytes;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] loadAndRotateImage(File imageFile) {

        try {
            // Read image EXIF data
            LLJTran llj = new LLJTran(imageFile);
            llj.read(LLJTran.READ_INFO, true);
            AbstractImageInfo<?> imageInfo = llj.getImageInfo();
            if (!(imageInfo instanceof Exif)) throw new Exception("Image has no EXIF data");

            // Determine the orientation
            Exif exif = (Exif) imageInfo;
            int orientation = 1;
            Entry orientationTag = exif.getTagValue(Exif.ORIENTATION, true);
            if (orientationTag != null) orientation = (Integer) orientationTag.getValue(0);

            // Determine required transform operation
            int operation = 0;
            if (orientation > 0 && orientation < Exif.opToCorrectOrientation.length)
                operation = Exif.opToCorrectOrientation[orientation];

            try {
                // Transform image
                llj.read(LLJTran.READ_ALL, true);
                llj.transform(operation, LLJTran.OPT_DEFAULTS | LLJTran.OPT_XFORM_ORIENTATION);


                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                llj.save(outputStream, LLJTran.OPT_WRITE_ALL);
                return outputStream.toByteArray();

            } finally {
                llj.freeMemory();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}


/* vim:set shiftwidth=4 softtabstop=4 expandtab: */