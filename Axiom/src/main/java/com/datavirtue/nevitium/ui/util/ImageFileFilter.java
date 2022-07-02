package com.datavirtue.axiom.ui.util;

import java.io.File;
import java.io.FilenameFilter;

/**
 *
 * @author SeanAnderson
 */
public class ImageFileFilter implements FilenameFilter {

    @Override
    public boolean accept(File dir, String fileName) {
        fileName = fileName.toLowerCase();
        return (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".gif"));
    }
}
