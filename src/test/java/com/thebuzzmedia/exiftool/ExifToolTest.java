/**
 * Copyright 2011 The Buzz Media, LLC
 * Copyright 2015-2019 Mickael Jeanroy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thebuzzmedia.exiftool;

import com.thebuzzmedia.exiftool.exceptions.UnsupportedFeatureException;
import com.thebuzzmedia.exiftool.process.Command;
import com.thebuzzmedia.exiftool.process.CommandExecutor;
import com.thebuzzmedia.exiftool.process.CommandResult;
import com.thebuzzmedia.exiftool.tests.builders.CommandResultBuilder;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.thebuzzmedia.exiftool.tests.ReflectionTestUtils.readPrivateField;
import static com.thebuzzmedia.exiftool.tests.ReflectionTestUtils.readStaticPrivateField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("resource")
@RunWith(MockitoJUnitRunner.Silent.class)
public class ExifToolTest {

	@Mock
	private CommandExecutor executor;

	@Mock
	private ExecutionStrategy strategy;

	@Captor
	private ArgumentCaptor<Command> cmdCaptor;

	@Captor
	private ArgumentCaptor<Version> versionCaptor;

	private String path;

	@Before
	public void setUp() throws Exception {
		path = "exiftool";

		CommandResult v9_36 = new CommandResultBuilder()
				.output("9.36")
				.build();

		when(executor.execute(any(Command.class))).thenReturn(v9_36);

		// Clear cache before each test
		VersionCache cache = readStaticPrivateField(ExifTool.class, "cache");
		cache.clear();
		assertThat(cache).isNotNull();
		assertThat(cache.size()).isZero();
	}

	@Test
	public void it_should_not_create_exiftool_if_path_is_null() {
		ThrowingCallable newExifTool = new ThrowingCallable() {
			@Override
			public void call() {
				new ExifTool(null, executor, strategy);
			}
		};

		assertThatThrownBy(newExifTool)
				.isInstanceOf(NullPointerException.class)
				.hasMessage("ExifTool path should not be null");
	}

	@Test
	public void it_should_not_create_exiftool_if_path_is_empty() {
		ThrowingCallable newExifTool = new ThrowingCallable() {
			@Override
			public void call() {
				new ExifTool("", executor, strategy);
			}
		};

		assertThatThrownBy(newExifTool)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("ExifTool path should not be null");
	}

	@Test
	public void it_should_not_create_exiftool_if_path_is_blank() {
		ThrowingCallable newExifTool = new ThrowingCallable() {
			@Override
			public void call() {
				new ExifTool("  ", executor, strategy);
			}
		};

		assertThatThrownBy(newExifTool)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("ExifTool path should not be null");
	}

	@Test
	public void it_should_not_create_exiftool_if_executor_is_null() {
		ThrowingCallable newExifTool = new ThrowingCallable() {
			@Override
			public void call() {
				new ExifTool(path, null, strategy);
			}
		};

		assertThatThrownBy(newExifTool)
				.isInstanceOf(NullPointerException.class)
				.hasMessage("Executor should not be null");
	}

	@Test
	public void it_should_not_create_exiftool_if_strategy_is_null() {
		ThrowingCallable newExifTool = new ThrowingCallable() {
			@Override
			public void call() {
				new ExifTool(path, executor, null);
			}
		};

		assertThatThrownBy(newExifTool)
				.isInstanceOf(NullPointerException.class)
				.hasMessage("Execution strategy should not be null");
	}

	@Test
	public void it_should_get_version_from_cache() {
		VersionCache cache = readStaticPrivateField(ExifTool.class, "cache");

		cache.clear();
		assertThat(cache).isNotNull();
		assertThat(cache.size()).isZero();

		when(strategy.isSupported(any(Version.class))).thenReturn(true);

		ExifTool exifTool = new ExifTool(path, executor, strategy);
		assertThat(exifTool.getVersion()).isNotNull();

		cache = readStaticPrivateField(ExifTool.class, "cache");
		assertThat(cache).isNotNull();
		assertThat(cache.size()).isEqualTo(1);
	}

	@Test
	public void it_should_check_if_exiftool_is_running() {
		when(strategy.isSupported(any(Version.class))).thenReturn(true);
		ExifTool exifTool = new ExifTool(path, executor, strategy);

		when(strategy.isRunning()).thenReturn(false);
		assertThat(exifTool.isRunning()).isFalse();
		verify(strategy).isRunning();

		reset(strategy);

		when(strategy.isRunning()).thenReturn(true);
		assertThat(exifTool.isRunning()).isTrue();
		verify(strategy).isRunning();
	}

	@Test
	public void it_should_close_exiftool() throws Exception {
		when(strategy.isSupported(any(Version.class))).thenReturn(true);
		ExifTool exifTool = new ExifTool(path, executor, strategy);
		exifTool.close();
		verify(strategy).shutdown();
	}

	@Test
	public void it_should_stop_exiftool() throws Exception {
		when(strategy.isSupported(any(Version.class))).thenReturn(true);
		ExifTool exifTool = new ExifTool(path, executor, strategy);
		exifTool.stop();
		verify(strategy).close();
		verify(strategy, never()).shutdown();
	}

	@Test
	public void it_should_get_exiftool_version() {
		when(strategy.isSupported(any(Version.class))).thenReturn(true);
		ExifTool exifTool = new ExifTool(path, executor, strategy);
		Version version = exifTool.getVersion();
		assertThat(version).isEqualTo(new Version("9.36"));
	}

	@Test
	public void it_should_create_exiftool_instance_and_get_version() throws Exception {
		when(strategy.isSupported(any(Version.class))).thenReturn(true);
		ExifTool exifTool = new ExifTool(path, executor, strategy);

		String path = readPrivateField(exifTool, "path");
		CommandExecutor executor = readPrivateField(exifTool, "executor");
		ExecutionStrategy strategy = readPrivateField(exifTool, "strategy");

		assertThat(path).isEqualTo(this.path);
		assertThat(executor).isEqualTo(this.executor);
		assertThat(strategy).isEqualTo(this.strategy);

		verify(this.strategy).isSupported(versionCaptor.capture());

		Version version = versionCaptor.getValue();
		assertThat(version).isNotNull();
		assertThat(version.getMajor()).isEqualTo(9);
		assertThat(version.getMinor()).isEqualTo(36);
		assertThat(version.getPatch()).isEqualTo(0);

		verify(this.executor).execute(cmdCaptor.capture());
		assertThat(cmdCaptor.getValue()).isNotNull();
		assertThat(cmdCaptor.getValue().getArguments()).hasSize(2).containsExactly(this.path, "-ver");
	}

	@Test
	public void it_should_not_create_exiftool_instance_if_strategy_is_not_supported() {
		when(strategy.isSupported(any(Version.class))).thenReturn(false);

		ThrowingCallable newExifTool = new ThrowingCallable() {
			@Override
			public void call() {
				new ExifTool(path, executor, strategy);
			}
		};

		assertThatThrownBy(newExifTool)
				.isInstanceOf(UnsupportedFeatureException.class)
				.hasMessage(
						"Use of feature requires version 9.36.0 or higher of the native ExifTool program. " +
								"The version of ExifTool referenced by the path '" + path + "' is not high enough. " +
								"You can either upgrade the install of ExifTool or avoid using this feature to workaround this exception."
				);
	}
}
