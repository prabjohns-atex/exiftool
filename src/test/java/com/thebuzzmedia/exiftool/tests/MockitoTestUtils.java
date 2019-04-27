package com.thebuzzmedia.exiftool.tests;

import org.mockito.ArgumentMatchers;

import java.util.List;

/**
 * Static Mockito Utilities.
 * Used in test only.
 */
public final class MockitoTestUtils {

	// Ensure non instantiation.
	private MockitoTestUtils() {
	}

	/**
	 * Replacement for deprecated Mockito#anyListOf.
	 *
	 * @param klass The class.
	 * @param <T> Type of elements.
	 * @return List of elements of type T.
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> anyListOf(Class<T> klass) {
		return (List<T>) ArgumentMatchers.anyList();
	}
}
