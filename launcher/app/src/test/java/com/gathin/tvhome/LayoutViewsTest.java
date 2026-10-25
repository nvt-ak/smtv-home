package com.gathin.tvhome;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.util.AttributeSet;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

/**
 * A custom view named in a layout is created by reflection through a public (Context, AttributeSet)
 * constructor. A missing one only shows on a TV, as an inflate crash and a black Home, so check them all here.
 */
public class LayoutViewsTest {

    private static final Pattern TAG = Pattern.compile("<\\s*(com\\.gathin\\.tvhome\\.[A-Za-z0-9_.$]+)");

    @Test
    public void everyCustomViewInALayoutCanBeInflated() throws Exception {
        File res = new File("src/main/res");
        assertTrue("run from the app module: " + res.getAbsolutePath(), res.isDirectory());
        int seen = 0;
        File[] dirs = res.listFiles((d, n) -> n.startsWith("layout"));
        assertNotNull(dirs);
        for (File dir : dirs) {
            File[] files = dir.listFiles((d, n) -> n.endsWith(".xml"));
            if (files == null) continue;
            for (File f : files) {
                String xml = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                Matcher m = TAG.matcher(xml);
                while (m.find()) {
                    Class<?> c = Class.forName(m.group(1));
                    c.getConstructor(Context.class, AttributeSet.class);  // throws NoSuchMethodException
                    seen++;
                }
            }
        }
        assertTrue("expected at least one custom view in the layouts", seen > 0);
    }
}
