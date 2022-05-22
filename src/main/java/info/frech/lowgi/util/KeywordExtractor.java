package info.frech.lowgi.util;

/**
 *    Copyright 2022 Tobias Frech
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegSegmentMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.iptc.IptcDirectory;
import com.drew.metadata.photoshop.PhotoshopReader;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;


public class KeywordExtractor
{
    public static void main(String[] args)
    {
        File file = new File(args[0]);

        boolean dirMode = file.isDirectory();

        List<File> filesToProcess;
        if (dirMode) {
            File[] files = file.listFiles((dir, name) -> name.toLowerCase().endsWith(".jpg"));
            filesToProcess = Arrays.asList(files);
        } else {
            filesToProcess = List.of(file);
        }

        HashSet<String> gatherKeyworde = new HashSet<>();

        Set<String> allKeywords = filesToProcess.stream().map(KeywordExtractor::gatherKeywords).flatMap(Collection::stream).collect(Collectors.toSet());

        String s = allKeywords.stream().sorted().collect(Collectors.joining("\n"));

        System.out.println(s);
    }

    private static Collection<String> gatherKeywords(File file) {
        Iterable<JpegSegmentMetadataReader> readers = Arrays.asList(new PhotoshopReader());

        Metadata metadata = null;
        try {
            metadata = JpegMetadataReader.readMetadata(file, readers);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        IptcDirectory directory = metadata.getFirstDirectoryOfType(IptcDirectory.class);
        String[] keywords = directory.getStringArray(IptcDirectory.TAG_KEYWORDS);
        return Arrays.asList(keywords);
    }
}