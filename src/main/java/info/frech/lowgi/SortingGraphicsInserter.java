package info.frech.lowgi;
/**
 * Copyright 2022 Tobias Frech
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import com.sun.star.text.*;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import info.frech.lowgi.util.KeywordExtractor;
import mediautil.image.jpeg.AbstractImageInfo;
import mediautil.image.jpeg.Entry;
import mediautil.image.jpeg.Exif;
import mediautil.image.jpeg.LLJTran;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class SortingGraphicsInserter {

    private static List<String> ignoredKeywords = List.of("Kornwestheim", "25 Auswahl A", "25 Auswahl B", "Kornwestheim AmStadtgarten25");

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

                List<CookedImage> cookedImages = Arrays.stream(files).map(f -> precookImage(f)).sorted((Comparator.comparing(CookedImage::getTags))).collect(Collectors.toList());

                for (CookedImage image : cookedImages) {
                    embedGraphics(xContext, xMCF, xMSFDoc, xText, xTextCursor, image);

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


    private static CookedImage precookImage(File file) {
        System.out.println("now " + file);
        CookedImage cookedImage = loadAndRotateImage(file);
        List<String> strings = KeywordExtractor.gatherKeywords(file);
        strings.removeAll(ignoredKeywords);
        Collections.sort(strings);
        String keywords = String.join(", ", strings);
        cookedImage.setTags(keywords);
        return cookedImage;
    }

    private static void embedGraphics(XComponentContext xContext, XMultiComponentFactory xMCF, XMultiServiceFactory xMSFDoc, XText xText, XTextCursor xTextCursor, CookedImage image) throws com.sun.star.uno.Exception, IOException {

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

        XGraphicProvider xGraphicProvider = UnoRuntime.queryInterface(XGraphicProvider.class, xMCF.createInstanceWithContext("com.sun.star.graphic.GraphicProvider", xContext));

        PropertyValue[] propValues = new PropertyValue[]{new PropertyValue()};
        propValues[0].Name = "InputStream";
        propValues[0].Value = new ByteArrayToXInputStreamAdapter(image.getImage());

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

        int fixedWidth;
        if (ratio < 1.0) {
            fixedWidth = 8000;
        } else {
            fixedWidth = 15000;
        }

        xPropSet.setPropertyValue("Width", Integer.valueOf(fixedWidth));
        xPropSet.setPropertyValue("Height", Integer.valueOf((int) (fixedWidth / ratio)));

        try {
            // Inserting the content
            xText.insertControlCharacter(xTextCursor, ControlCharacter.PARAGRAPH_BREAK, false);
            xText.insertTextContent(xTextCursor, xTextContent, false);
            xText.insertControlCharacter(xTextCursor, ControlCharacter.PARAGRAPH_BREAK, false);
            xText.insertString(xTextCursor, image.getDate(), false);
            xText.insertControlCharacter(xTextCursor, ControlCharacter.HARD_SPACE, false);
            xText.insertString(xTextCursor, image.getFileName(), false);
            xText.insertControlCharacter(xTextCursor, ControlCharacter.PARAGRAPH_BREAK, false);
            xText.insertString(xTextCursor, image.getTags(), false);
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

    public static CookedImage loadAndRotateImage(File imageFile) {

        try {
            CookedImage result = new CookedImage();
            // Read image EXIF data
            LLJTran llj = new LLJTran(imageFile);
            llj.read(LLJTran.READ_INFO, true);
            AbstractImageInfo<?> imageInfo = llj.getImageInfo();
            if (!(imageInfo instanceof Exif)) throw new Exception("Image has no EXIF data");

            // Determine the orientation
            Exif exif = (Exif) imageInfo;
            int orientation = 1;
            Entry orientationTag = exif.getTagValue(Exif.ORIENTATION, true);
            Entry dateTag = exif.getTagValue(Exif.DATETIME, true);
            Entry commentTag = exif.getTagValue(Exif.USERCOMMENT, true);

            String d = dateTag.toString();
            if (commentTag != null) {
                result.setComment(extractComment(commentTag));
            }

            SimpleDateFormat parseSdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
            Date date = parseSdf.parse(d.substring(0, d.length() - 1));
            SimpleDateFormat formatSdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            result.setDate(formatSdf.format(date));

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
                result.setImage(outputStream.toByteArray());
                result.setFileName(imageFile.getName());
                return result;

            } finally {
                llj.freeMemory();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String extractComment(Entry commentTag) {
        Object[] values = commentTag.getValues();
        String comment = "";
        for (int i = 8; i < values.length; i++) {
            comment += Character.toString((Integer) values[i]);
            ;
        }
        return comment;
    }
}
