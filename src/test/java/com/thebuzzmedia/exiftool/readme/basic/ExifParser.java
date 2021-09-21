package com.thebuzzmedia.exiftool.readme.basic;

import com.thebuzzmedia.exiftool.ExifTool;
import com.thebuzzmedia.exiftool.ExifToolBuilder;
import com.thebuzzmedia.exiftool.Tag;
import com.thebuzzmedia.exiftool.core.StandardTag;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

public class ExifParser {
	public static Map<Tag, String> parse(File image) throws Exception {
		// ExifTool path must be defined as a system property (`exiftool.path`),
		// but path can be set using `withPath` method.
		try (ExifTool exifTool = new ExifToolBuilder().build()) {
			return exifTool.getImageMeta(image, Arrays.asList(
					StandardTag.ISO,
					StandardTag.X_RESOLUTION,
					StandardTag.Y_RESOLUTION
			));

		}
	}

	public static void main(String[] args) throws Exception {
		for (String image : args) {
			System.out.println("Tags: " + ExifParser.parse(new File(image)));
		}
	}
}
