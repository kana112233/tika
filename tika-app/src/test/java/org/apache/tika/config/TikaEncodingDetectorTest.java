/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tika.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.tika.Tika;
import org.apache.tika.detect.CompositeEncodingDetector;
import org.apache.tika.detect.EncodingDetector;
import org.apache.tika.detect.NonDetectingEncodingDetector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AbstractEncodingDetectorParser;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.CompositeParser;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ParserDecorator;
import org.apache.tika.parser.html.HtmlEncodingDetector;
import org.apache.tika.parser.txt.Icu4jEncodingDetector;
import org.apache.tika.parser.txt.TXTParser;
import org.apache.tika.parser.txt.UniversalEncodingDetector;
import org.junit.Ignore;
import org.junit.Test;

public class TikaEncodingDetectorTest extends AbstractTikaConfigTest {

    @Test
    @Ignore("until we figure out how to get legacy ordering")
    public void testDefault() {
        EncodingDetector detector = TikaConfig.getDefaultConfig().getEncodingDetector();
        assertTrue(detector instanceof CompositeEncodingDetector);
        List<EncodingDetector> detectors = ((CompositeEncodingDetector) detector).getDetectors();
        assertEquals(3, detectors.size());
        assertTrue(detectors.get(0) instanceof HtmlEncodingDetector);
        assertTrue(detectors.get(1) instanceof UniversalEncodingDetector);
        assertTrue(detectors.get(2) instanceof Icu4jEncodingDetector);
    }

    @Test
    @Ignore("getting 4 detectors instead of 2 in sure-fire tests")
    public void testBlackList() throws Exception {
        TikaConfig config = getConfig("TIKA-2273-blacklist-encoding-detector-default.xml");
        EncodingDetector detector = config.getEncodingDetector();
        assertTrue(detector instanceof CompositeEncodingDetector);
        List<EncodingDetector> detectors = ((CompositeEncodingDetector) detector).getDetectors();
        assertEquals(2, detectors.size());

        EncodingDetector detector1 = detectors.get(0);
        assertTrue(detector1 instanceof CompositeEncodingDetector);
        List<EncodingDetector> detectors1Children = ((CompositeEncodingDetector) detector1).getDetectors();
        assertEquals(2, detectors1Children.size());
        assertTrue(detectors1Children.get(0) instanceof UniversalEncodingDetector);
        assertTrue(detectors1Children.get(1) instanceof Icu4jEncodingDetector);

        assertTrue(detectors.get(1) instanceof NonDetectingEncodingDetector);

    }

    @Test
    @Ignore("until we add @Field to 2.x")
    public void testParameterization() throws Exception {
        TikaConfig config = getConfig("TIKA-2273-parameterize-encoding-detector.xml");
        EncodingDetector detector = config.getEncodingDetector();
        assertTrue(detector instanceof CompositeEncodingDetector);
        List<EncodingDetector> detectors = ((CompositeEncodingDetector) detector).getDetectors();
        assertEquals(2, detectors.size());
        assertTrue(((Icu4jEncodingDetector) detectors.get(0)).getStripMarkup());
        assertTrue(detectors.get(1) instanceof NonDetectingEncodingDetector);
    }

    @Test
    public void testEncodingDetectorsAreLoaded() {
        EncodingDetector encodingDetector = ((AbstractEncodingDetectorParser) new TXTParser()).getEncodingDetector();

        assertTrue(encodingDetector instanceof CompositeEncodingDetector);
    }

    @Test
    public void testEncodingDetectorConfigurability() throws Exception {
        TikaConfig tikaConfig = new TikaConfig(
                getResourceAsStream("/org/apache/tika/config/TIKA-2273-no-icu4j-encoding-detector.xml"));
        AutoDetectParser p = new AutoDetectParser(tikaConfig);

        try {
            Metadata metadata = getXML("english.cp500.txt", p).metadata;
            fail("can't detect w/out ICU");
        } catch (TikaException e) {
            assertContains("Failed to detect", e.getMessage());
        }

        Tika tika = new Tika(tikaConfig);
        Path tmp = getTestDocumentAsTempFile("english.cp500.txt");
        try {
            String txt = tika.parseToString(tmp);
            fail("can't detect w/out ICU");
        } catch (TikaException e) {
            assertContains("Failed to detect", e.getMessage());
        } finally {
            Files.delete(tmp);
        }
    }


    @Test
    @Ignore("need to add @Field to 2.x")
    public void testNonDetectingDetectorParams() throws Exception {
        TikaConfig tikaConfig = new TikaConfig(
                getResourceAsStream("/org/apache/tika/config/TIKA-2273-non-detecting-params.xml"));
        AutoDetectParser p = new AutoDetectParser(tikaConfig);
        List<Parser> parsers = new ArrayList<>();
        findEncodingDetectionParsers(p, parsers);

        assertEquals(3, parsers.size());
        EncodingDetector encodingDetector = ((AbstractEncodingDetectorParser)parsers.get(0)).getEncodingDetector();
        assertTrue(encodingDetector instanceof CompositeEncodingDetector);
        assertEquals(1, ((CompositeEncodingDetector) encodingDetector).getDetectors().size());
        EncodingDetector child = ((CompositeEncodingDetector) encodingDetector).getDetectors().get(0);
        assertTrue( child instanceof NonDetectingEncodingDetector);

        assertEquals(StandardCharsets.UTF_16LE, ((NonDetectingEncodingDetector)child).getCharset());

    }

    @Test
    @Ignore("need to add @Field to 2.x")
    public void testNonDetectingDetectorParamsBadCharset() throws Exception {
        /*
        try {
            TikaConfig tikaConfig = new TikaConfig(
                    getResourceAsStream("/org/apache/tika/config/TIKA-2273-non-detecting-params-bad-charset.xml"));
            fail("should have thrown TikaConfigException");
        } catch (TikaConfigException e) {

        }*/
    }

    @Test
    @Ignore("getting 5 parsers instead of 3 in sure-fire tests")
    public void testConfigurabilityOfUserSpecified() throws Exception {
        TikaConfig tikaConfig = new TikaConfig(
                getResourceAsStream("/org/apache/tika/config/TIKA-2273-encoding-detector-outside-static-init.xml"));
        AutoDetectParser p = new AutoDetectParser(tikaConfig);

        //make sure that all static and non-static parsers are using the same encoding detector!
        List<Parser> parsers = new ArrayList<>();
        findEncodingDetectionParsers(p, parsers);

        assertEquals(3, parsers.size());

        for (Parser encodingDetectingParser : parsers) {
            EncodingDetector encodingDetector = ((AbstractEncodingDetectorParser) encodingDetectingParser).getEncodingDetector();
            assertTrue(encodingDetector instanceof CompositeEncodingDetector);
            assertEquals(3, ((CompositeEncodingDetector) encodingDetector).getDetectors().size());
            for (EncodingDetector child : ((CompositeEncodingDetector) encodingDetector).getDetectors()) {
                assertNotContained("cu4j", child.getClass().getCanonicalName());
            }
        }

    }

    private void findEncodingDetectionParsers(Parser p, List<Parser> encodingDetectionParsers) {

        if (p instanceof CompositeParser) {
            for (Parser child : ((CompositeParser) p).getAllComponentParsers()) {
                findEncodingDetectionParsers(child, encodingDetectionParsers);
            }
        } else if (p instanceof ParserDecorator) {
            findEncodingDetectionParsers(((ParserDecorator) p), encodingDetectionParsers);
        }

        if (p instanceof AbstractEncodingDetectorParser) {
            encodingDetectionParsers.add(p);
        }
    }
}