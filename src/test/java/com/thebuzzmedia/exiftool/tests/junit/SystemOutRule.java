/**
 * Copyright 2011 The Buzz Media, LLC
 * Copyright 2015-2019 Mickael Jeanroy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thebuzzmedia.exiftool.tests.junit;

import org.junit.rules.ExternalResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Catch System.out logging and store in a buffer.
 */
public class SystemOutRule extends ExternalResource {

	/**
	 * A flag to enable/disable log to console.
	 */
	private final boolean writeToConsole;

	/**
	 * Original out stream.
	 * Will be initialized before each tests.
	 * Will be restored after each tests.
	 */
	private PrintStream originalOut;

	/**
	 * Custom out stream.
	 * Will be initialized before each tests.
	 * Will be flushed after each tests.
	 */
	private CompositeOutputStream compositeOut;

	public SystemOutRule(boolean writeToConsole) {
		this.writeToConsole = writeToConsole;
	}

	@Override
	public void before() {
		originalOut = System.out;
		compositeOut = new CompositeOutputStream(
				writeToConsole ? originalOut : new ByteArrayOutputStream()
		);

		System.setOut(new PrintStream(
				compositeOut
		));
	}

	@Override
	public void after() {
		try {
			compositeOut.flush();
		}
		catch (IOException ex) {
			// No worries
		}

		try {
			compositeOut.close();
		}
		catch (IOException ex) {
			// No worries
		}

		// Restore original out stream
		System.setOut(originalOut);

		// Remove this one.
		compositeOut = null;
	}

	public String getPendingOut() {
		if (compositeOut == null) {
			return null;
		}

		return compositeOut.sb.toString();
	}

	private static class CompositeOutputStream extends OutputStream {
		private final OutputStream o1;
		private final StringBuilder sb;

		private CompositeOutputStream(OutputStream o1) {
			this.o1 = o1;
			this.sb = new StringBuilder();
		}

		@Override
		public void write(int b) throws IOException {
			o1.write(b);
			sb.append((char) b);
		}

		@Override
		public void flush() throws IOException {
			super.flush();
			o1.flush();
		}

		@Override
		public void close() throws IOException {
			super.close();
			o1.close();
			sb.delete(0, sb.length());
		}
	}
}
