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

package com.thebuzzmedia.exiftool.commons.lang;

import com.thebuzzmedia.exiftool.exceptions.UnreadableFileException;
import com.thebuzzmedia.exiftool.exceptions.UnwritableFileException;
import com.thebuzzmedia.exiftool.tests.builders.FileBuilder;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PreConditionsTest {

	@Test
	public void it_should_fail_with_npe() {
		final String message = "should not be null";

		ThrowingCallable notNull = new ThrowingCallable() {
			@Override
			public void call() {
				PreConditions.notNull(null, message);
			}
		};

		assertThatThrownBy(notNull)
				.isInstanceOf(NullPointerException.class)
				.hasMessage(message);
	}

	@Test
	public void it_should_not_fail_with_npe() {
		String val = "foo";
		String foo = PreConditions.notNull(val, "should not be null");
		assertThat(foo).isEqualTo(val);
	}

	@Test
	public void it_should_fail_with_null_string() {
		final String message = "should not be empty";

		ThrowingCallable notBlank = new ThrowingCallable() {
			@Override
			public void call() {
				PreConditions.notBlank(null, message);
			}
		};

		assertThatThrownBy(notBlank)
				.isInstanceOf(NullPointerException.class)
				.hasMessage(message);
	}

	@Test
	public void it_should_fail_with_empty_string() {
		final String message = "should not be empty";

		ThrowingCallable notBlank = new ThrowingCallable() {
			@Override
			public void call() {
				PreConditions.notBlank("", message);
			}
		};

		assertThatThrownBy(notBlank)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage(message);
	}

	@Test
	public void it_should_fail_with_blank_string() {
		final String message = "should not be empty";

		ThrowingCallable notBlank = new ThrowingCallable() {
			@Override
			public void call() {
				PreConditions.notBlank("  ", message);
			}
		};

		assertThatThrownBy(notBlank)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage(message);
	}

	@Test
	public void it_should_not_fail_with_valid_string() {
		String message = "should not be empty";
		String val = "foo";

		String result = PreConditions.notBlank(val, message);

		assertThat(result).isEqualTo(val);
	}

	@Test
	public void it_should_fail_with_null_array() {
		final String message = "should not be empty";

		ThrowingCallable notEmpty = new ThrowingCallable() {
			@Override
			public void call() {
				PreConditions.notBlank("  ", message);
			}
		};

		assertThatThrownBy(notEmpty)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage(message);
	}

	@Test
	public void it_should_fail_with_empty_array() {
		final String message = "should not be empty";

		ThrowingCallable notEmpty = new ThrowingCallable() {
			@Override
			public void call() {
				PreConditions.notEmpty(new String[]{}, message);
			}
		};

		assertThatThrownBy(notEmpty)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage(message);
	}

	@Test
	public void it_should_not_fail_with_valid_array() {
		String message = "should not be empty";
		String[] val = new String[]{"foo"};

		String[] results = PreConditions.notEmpty(val, message);

		assertThat(results).isSameAs(val);
	}

	@Test
	public void it_should_fail_with_null_map() {
		final String message = "should not be empty";

		ThrowingCallable notEmpty = new ThrowingCallable() {
			@Override
			public void call() {
				PreConditions.notEmpty((Map<Object, Object>) null, message);
			}
		};

		assertThatThrownBy(notEmpty)
				.isInstanceOf(NullPointerException.class)
				.hasMessage(message);
	}

	@Test
	public void it_should_fail_with_empty_map() {
		final String message = "should not be empty";

		ThrowingCallable notEmpty = new ThrowingCallable() {
			@Override
			public void call() {
				PreConditions.notEmpty(emptyMap(), message);
			}
		};

		assertThatThrownBy(notEmpty)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage(message);
	}

	@Test
	public void it_should_not_fail_with_valid_map() {
		String message = "should not be empty";
		Map<String, String> val = new HashMap<>();
		val.put("foo", "bar");

		Map<String, String> results = PreConditions.notEmpty(val, message);

		assertThat(results).isSameAs(val);
	}

	@Test
	public void it_should_fail_with_null_iterable() {
		final String message = "should not be empty";

		ThrowingCallable notEmpty = new ThrowingCallable() {
			@Override
			public void call() {
				PreConditions.notEmpty((Iterable<Object>) null, message);
			}
		};

		assertThatThrownBy(notEmpty)
				.isInstanceOf(NullPointerException.class)
				.hasMessage(message);
	}

	@Test
	public void it_should_fail_with_empty_iterable() {
		final String message = "should not be empty";

		ThrowingCallable notEmpty = new ThrowingCallable() {
			@Override
			public void call() {
				PreConditions.notEmpty(emptyList(), message);
			}
		};

		assertThatThrownBy(notEmpty)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage(message);
	}

	@Test
	public void it_should_not_fail_with_valid_iterable() {
		String message = "should not be empty";
		Iterable<String> val = asList("foo", "bar");

		Iterable<String> results = PreConditions.notEmpty(val, message);

		assertThat(results)
				.isNotNull()
				.isNotEmpty()
				.isSameAs(val);
	}

	@Test
	public void it_should_fail_with_null_number() {
		final String message = "should be readable";

		ThrowingCallable isPositive = new ThrowingCallable() {
			@Override
			public void call() {
				PreConditions.isPositive(null, message);
			}
		};

		assertThatThrownBy(isPositive)
				.isInstanceOf(NullPointerException.class)
				.hasMessage(message);
	}

	@Test
	public void it_should_fail_with_negative_number() {
		final String message = "should be readable";

		ThrowingCallable isPositive = new ThrowingCallable() {
			@Override
			public void call() {
				PreConditions.isPositive(-1, message);
			}
		};

		assertThatThrownBy(isPositive)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage(message);
	}

	@Test
	public void it_should_fail_with_zero() {
		final String message = "should be readable";

		ThrowingCallable isPositive = new ThrowingCallable() {
			@Override
			public void call() {
				PreConditions.isPositive(0, message);
			}
		};

		assertThatThrownBy(isPositive)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage(message);
	}

	@Test
	public void it_should_not_fail_with_positive_number() {
		String message = "should be readable";

		int nb = PreConditions.isPositive(1, message);

		assertThat(nb).isEqualTo(1);
	}

	@Test
	public void it_should_not_be_readable_with_null_file() {
		final String message = "should be readable";

		ThrowingCallable isReadable = new ThrowingCallable() {
			@Override
			public void call() {
				PreConditions.isReadable(null, message);
			}
		};

		assertThatThrownBy(isReadable)
				.isInstanceOf(NullPointerException.class)
				.hasMessage(message);
	}

	@Test
	public void it_should_not_be_readable_with_file_that_does_not_exist() {
		final String message = "should be readable";
		final File file = new FileBuilder("foo.png").exists(false).build();

		ThrowingCallable isReadable = new ThrowingCallable() {
			@Override
			public void call() {
				PreConditions.isReadable(file, message);
			}
		};

		assertThatThrownBy(isReadable)
				.isInstanceOf(UnreadableFileException.class)
				.hasMessage(message);
	}

	@Test
	public void it_should_fail_with_file_that_is_not_readable() {
		final String message = "should be readable";
		final File file = new FileBuilder("foo.png").canRead(false).build();

		ThrowingCallable isReadable = new ThrowingCallable() {
			@Override
			public void call() {
				PreConditions.isReadable(file, message);
			}
		};

		assertThatThrownBy(isReadable)
				.isInstanceOf(UnreadableFileException.class)
				.hasMessage(message);
	}

	@Test
	public void it_should_not_fail_with_readable_file() {
		String message = "should be readable";
		File file = new FileBuilder("foo.png").build();

		File result = PreConditions.isReadable(file, message);

		assertThat(result).isSameAs(file);
	}

	@Test
	public void it_should_not_be_writable_with_null_file() {
		final String message = "should be writable";

		ThrowingCallable isWritable = new ThrowingCallable() {
			@Override
			public void call() {
				PreConditions.isWritable(null, message);
			}
		};

		assertThatThrownBy(isWritable)
				.isInstanceOf(NullPointerException.class)
				.hasMessage(message);
	}

	@Test
	public void it_should_not_be_writable_with_file_that_does_not_exist() {
		final String message = "should be writable";
		final File file = new FileBuilder("foo.png").exists(false).build();

		ThrowingCallable isWritable = new ThrowingCallable() {
			@Override
			public void call() {
				PreConditions.isWritable(file, message);
			}
		};

		assertThatThrownBy(isWritable)
				.isInstanceOf(UnwritableFileException.class)
				.hasMessage(message);
	}

	@Test
	public void it_should_fail_with_file_that_is_not_writable() {
		final String message = "should be writable";
		final File file = new FileBuilder("foo.png").canWrite(false).build();

		ThrowingCallable isWritable = new ThrowingCallable() {
			@Override
			public void call() {
				PreConditions.isWritable(file, message);
			}
		};

		assertThatThrownBy(isWritable)
				.isInstanceOf(UnwritableFileException.class)
				.hasMessage(message);
	}

	@Test
	public void it_should_not_fail_with_writable_file() {
		String message = "should be writable";
		File file = new FileBuilder("foo.png").build();

		File result = PreConditions.isWritable(file, message);

		assertThat(result).isSameAs(file);
	}
}
