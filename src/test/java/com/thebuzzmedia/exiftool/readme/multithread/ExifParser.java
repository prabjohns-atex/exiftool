package com.thebuzzmedia.exiftool.readme.multithread;

import com.thebuzzmedia.exiftool.ExifTool;
import com.thebuzzmedia.exiftool.ExifToolBuilder;
import com.thebuzzmedia.exiftool.Tag;
import com.thebuzzmedia.exiftool.core.StandardTag;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExifParser {

	private static final ExifTool exifTool = detect();

	private static ExifTool detect() {
		return new ExifToolBuilder()
				.withPoolSize(10)  // Allow 10 process
				.enableStayOpen()
				.build();
	}

	public static Map<Tag, String> parse(File image) throws Exception {
		try (ExifTool exifTool = new ExifToolBuilder().build()) {
			return exifTool.getImageMeta(image, Arrays.asList(
					StandardTag.ISO,
					StandardTag.X_RESOLUTION,
					StandardTag.Y_RESOLUTION
			));

		}
	}

	private static Map<Tag, String> parse(String image) throws Exception {
		return parse(new File(image));
	}

	public static void main(String[] args) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(10);

		try {
			for (final String image : args) {
				executor.submit(() -> {
					try {
						System.out.println("Tags: " + parse(image));
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}
				});
			}
		} finally {
			executor.shutdown();
			exifTool.close();
		}
	}
}
