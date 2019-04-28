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

package com.thebuzzmedia.exiftool.logs;

import com.thebuzzmedia.exiftool.tests.junit.SystemOutRule;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractLoggerTest {

	private static final String INFO = "INFO";
	private static final String WARN = "WARN";
	private static final String ERROR = "ERROR";
	private static final String TRACE = "TRACE";
	private static final String DEBUG = "DEBUG";

	@Rule
	public SystemOutRule outRule = new SystemOutRule(false);

	@Test
	public void it_should_display_info() {
		getLogger().info("message");
		verifyOutput(INFO, "message");
	}

	@Test
	public void it_should_display_info_with_one_parameter() {
		getLogger().info("A message: {}", "test");
		verifyOutput(INFO, "A message: test");
	}

	@Test
	public void it_should_display_info_with_two_parameters() {
		getLogger().info("A message: {} -- {}", "p1", "p2");
		verifyOutput(INFO, "A message: p1 -- p2");
	}

	@Test
	public void it_should_display_warn() {
		getLogger().warn("message");
		verifyOutput(WARN, "message");
	}

	@Test
	public void it_should_display_warn_with_one_parameter() {
		getLogger().warn("A message: {}", "p1");
		verifyOutput(WARN, "A message: p1");
	}

	@Test
	public void it_should_display_warn_with_two_parameters() {
		getLogger().warn("A message: {} -- {}", "p1", "p2");
		verifyOutput(WARN, "A message: p1 -- p2");
	}

	@Test
	public void it_should_display_error() {
		getLogger().error("message");
		verifyOutput(ERROR, "message");
	}

	@Test
	public void it_should_display_error_with_one_parameter() {
		getLogger().error("A message: {}", "p1");
		verifyOutput(ERROR, "A message: p1");
	}

	@Test
	public void it_should_display_error_with_two_parameters() {
		getLogger().error("A message: {} -- {}", "p1", "p2");
		verifyOutput(ERROR, "A message: p1 -- p2");
	}

	@Test
	public void it_should_display_trace() {
		getLogger().trace("message");
		verifyOutput(TRACE, "message");
	}

	@Test
	public void it_should_display_trace_with_one_parameter() {
		getLogger().trace("A message: {}", "p1");
		verifyOutput(TRACE, "A message: p1");
	}

	@Test
	public void it_should_display_trace_with_two_parameters() {
		getLogger().trace("A message: {} -- {}", "p1", "p2");
		verifyOutput(TRACE, "A message: p1 -- p2");
	}

	@Test
	public void it_should_display_debug() {
		getLogger().debug("message");
		verifyOutput(DEBUG, "message");
	}

	@Test
	public void it_should_display_debug_with_one_parameter() {
		getLogger().debug("A message: {}", "p1");
		verifyOutput(DEBUG, "A message: p1");
	}

	@Test
	public void it_should_display_debug_with_two_parameters() {
		getLogger().debug("A message: {} -- {}", "p1", "p2");
		verifyOutput(DEBUG, "A message: p1 -- p2");
	}

	@Test
	public void it_should_not_display_debug_if_disabled() {
		getLoggerWithoutDebug().debug("message");
		assertThat(outRule.getPendingOut()).isEmpty();
	}

	@Test
	public void it_should_check_if_debug_is_enabled() {
		Logger logger1 = getLogger();
		assertThat(logger1.isDebugEnabled()).isTrue();

		Logger logger2 = getLoggerWithoutDebug();
		assertThat(logger2.isDebugEnabled()).isFalse();
	}

	@Test
	public void it_should_display_error_exception() {
		RuntimeException ex = new RuntimeException("Error Message");
		getLogger().error(ex.getMessage(), ex);
		verifyException("ERROR", ex);
	}

	@Test
	public void it_should_display_warn_exception() {
		RuntimeException ex = new RuntimeException("Error Message");
		getLogger().warn(ex.getMessage(), ex);
		verifyException("WARN", ex);
	}

	/**
	 * Create default logger to test.
	 *
	 * @return Logger.
	 */
	abstract Logger getLogger();

	/**
	 * Create logger, with debug disabled, to test.
	 *
	 * @return Logger.
	 */
	abstract Logger getLoggerWithoutDebug();

	private void verifyOutput(String level, String message) {
		String out = outRule.getPendingOut().trim();
		assertThat(out).contains(level);
		assertThat(out).endsWith(message);
	}

	private void verifyException(String level, Exception ex) {
		String out = outRule.getPendingOut().trim();
		assertThat(out).contains(level);
		assertThat(out).contains(ex.getMessage());
		assertThat(out).contains("at com.thebuzzmedia.exiftool.logs.AbstractLoggerTest");
	}
}
