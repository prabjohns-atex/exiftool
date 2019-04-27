/**
 * Copyright 2011 The Buzz Media, LLC
 * Copyright 2015 Mickael Jeanroy <mickael.jeanroy@gmail.com>
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

import org.apache.log4j.Level;

import static com.thebuzzmedia.exiftool.tests.ReflectionTestUtils.readPrivateField;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoggerLog4jTest extends AbstractLoggerTest {

	@Override
	Logger getLogger() {
		org.apache.log4j.Logger log4j = mock(org.apache.log4j.Logger.class);
		when(log4j.isDebugEnabled()).thenReturn(true);
		when(log4j.isInfoEnabled()).thenReturn(true);

		when(log4j.isEnabledFor(Level.TRACE)).thenReturn(true);
		when(log4j.isEnabledFor(Level.DEBUG)).thenReturn(true);
		when(log4j.isEnabledFor(Level.INFO)).thenReturn(true);
		when(log4j.isEnabledFor(Level.WARN)).thenReturn(true);
		when(log4j.isEnabledFor(Level.ERROR)).thenReturn(true);

		return new LoggerLog4j(log4j);
	}

	@Override
	Logger getLoggerWithoutDebug() {
		org.apache.log4j.Logger log4j = mock(org.apache.log4j.Logger.class);
		when(log4j.isDebugEnabled()).thenReturn(false);
		when(log4j.isInfoEnabled()).thenReturn(true);

		when(log4j.isEnabledFor(Level.TRACE)).thenReturn(true);
		when(log4j.isEnabledFor(Level.DEBUG)).thenReturn(false);
		when(log4j.isEnabledFor(Level.INFO)).thenReturn(true);
		when(log4j.isEnabledFor(Level.WARN)).thenReturn(true);
		when(log4j.isEnabledFor(Level.ERROR)).thenReturn(true);

		return new LoggerLog4j(log4j);
	}

	@Override
	void verifyInfo(Logger logger, String message, Object... params) {
		verifyCall(logger, Level.INFO, message, params);
	}

	@Override
	void verifyWarn(Logger logger, String message, Object... params) {
		verifyCall(logger, Level.WARN, message, params);
	}

	@Override
	void verifyError(Logger logger, String message, Object... params) {
		verifyCall(logger, Level.ERROR, message, params);
	}

	@Override
	void verifyErrorException(Logger logger, String message, Exception ex) {
		verifyException(logger, Level.ERROR, message, ex);
	}

	@Override
	void verifyWarnException(Logger logger, String message, Exception ex) {
		verifyException(logger, Level.WARN, message, ex);
	}

	@Override
	void verifyDebug(Logger logger, String message, Object... params) {
		verifyCall(logger, Level.DEBUG, message, params);
	}

	@Override
	void verifyWithoutDebug(Logger logger, String message, Object... params) {
		verify(getLog4j(logger), never()).debug(message);
	}

	@Override
	void verifyTrace(Logger logger, String message, Object... params) {
		verifyCall(logger, Level.TRACE, message, params);
	}

	private void verifyException(Logger logger, Level level, String message, Exception ex) {
		org.apache.log4j.Logger log4j = getLog4j(logger);
		verify(log4j).isEnabledFor(level);
		verify(log4j).log(level, message, ex);
	}

	private static void verifyCall(Logger logger, Level level, String message, Object... params) {
		String template = toStringFormatMessage(message);
		String msg = String.format(template, params);

		org.apache.log4j.Logger log4j = getLog4j(logger);
		verify(log4j).isEnabledFor(level);
		verify(log4j).log(level, msg);
	}

	private static org.apache.log4j.Logger getLog4j(Logger logger) {
		return readPrivateField(logger, "log");
	}

	private static String toStringFormatMessage(String message) {
		return message.replace("{}", "%s");
	}
}
