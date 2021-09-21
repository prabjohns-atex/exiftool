package com.thebuzzmedia.exiftool.readme.stayopen;

import com.thebuzzmedia.exiftool.ExifTool;
import com.thebuzzmedia.exiftool.ExifToolBuilder;
import com.thebuzzmedia.exiftool.Tag;
import com.thebuzzmedia.exiftool.core.StandardTag;
import com.thebuzzmedia.exiftool.exceptions.UnsupportedFeatureException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class ExifParser {

	private static final ExifTool exifTool = detect();

	private static ExifTool detect() {
		try {
			return new ExifToolBuilder().enableStayOpen().build();
		} catch (UnsupportedFeatureException ex) {
			// Fallback to simple exiftool instance.
			return new ExifToolBuilder().build();
		}
	}

	public static Map<Tag, String> parse(File image) throws IOException {
		return exifTool.getImageMeta(image, Arrays.asList(
				StandardTag.ISO,
				StandardTag.X_RESOLUTION,
				StandardTag.Y_RESOLUTION
		));
	}

	public static void main(String[] args) throws Exception {
		try {
			for (String image : args) {
				System.out.println("Tags: "+ ExifParser.parse(new File(image)));
			}
		} finally {
			exifTool.close();
		}
	}
}
