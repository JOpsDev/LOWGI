package info.frech.lowgi.test;
/*
 * Copyright 2002-2019 Drew Noakes and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 * More information about this project is available at:
 *
 *    https://drewnoakes.com/code/exif/
 *    https://github.com/drewnoakes/metadata-extractor
 */

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.imaging.jpeg.JpegSegmentMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.iptc.IptcDirectory;
import com.drew.metadata.photoshop.PhotoshopReader;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Showcases the most popular ways of using the metadata-extractor library.
 * <p>
 * For more information, see the project wiki: https://github.com/drewnoakes/metadata-extractor/wiki/GettingStarted
 *
 * @author Drew Noakes https://drewnoakes.com
 */

public class MetadataExtractor
{
    public static void main(String[] args)
    {
        File file = new File(args[0]);

        try {
            // We are only interested in handling
            Iterable<JpegSegmentMetadataReader> readers = Arrays.asList(
                    new PhotoshopReader()
                   );

            Metadata metadata = JpegMetadataReader.readMetadata(file, readers);

            IptcDirectory directory = metadata.getFirstDirectoryOfType(IptcDirectory.class);
            String[] strings = directory.getStringArray(IptcDirectory.TAG_KEYWORDS);
            for (String s:strings) {
                System.out.println(s);

            }

        } catch (JpegProcessingException e) {
            print(e);
        } catch (IOException e) {
            print(e);
        }
    }


    private static void print(Exception exception)
    {
        System.err.println("EXCEPTION: " + exception);
    }
}