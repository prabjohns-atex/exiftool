/**
 * Copyright 2011 The Buzz Media, LLC
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

package com.thebuzzmedia.exiftool;

import com.thebuzzmedia.exiftool.exceptions.UnsupportedFeatureException;
import com.thebuzzmedia.exiftool.logs.Logger;
import com.thebuzzmedia.exiftool.logs.LoggerFactory;
import com.thebuzzmedia.exiftool.process.Command;
import com.thebuzzmedia.exiftool.process.CommandExecutor;
import com.thebuzzmedia.exiftool.process.CommandProcess;
import com.thebuzzmedia.exiftool.process.CommandResult;
import com.thebuzzmedia.exiftool.process.command.CommandBuilder;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import static com.thebuzzmedia.exiftool.StopHandler.stopHandler;
import static com.thebuzzmedia.exiftool.StreamArgumentMapper.streamArgumentMapper;
import static com.thebuzzmedia.exiftool.commons.Collections.map;
import static com.thebuzzmedia.exiftool.commons.PreConditions.isReadable;
import static com.thebuzzmedia.exiftool.commons.PreConditions.isWritable;
import static com.thebuzzmedia.exiftool.commons.PreConditions.notEmpty;
import static com.thebuzzmedia.exiftool.commons.PreConditions.notNull;
import static java.util.Collections.unmodifiableSet;

/**
 * Class used to provide a Java-like interface to Phil Harvey's excellent,
 * Perl-based <a href="http://www.sno.phy.queensu.ca/~phil/exiftool">ExifTool</a>.
 *
 * There are a number of other basic Java wrappers to ExifTool available online,
 * but most of them only abstract out the actual Java-external-process execution
 * logic and do no additional work to make integration with the external
 * ExifTool any easier or intuitive from the perspective of the Java application
 * written to make use of ExifTool.
 *
 * This class was written in order to make integration with ExifTool inside of a
 * Java application seamless and performant with the goal being that the
 * developer can treat ExifTool as if it were written in Java, garnering all of
 * the benefits with none of the added headache of managing an external native
 * process from Java.
 *
 * Phil Harvey's ExifTool is written in Perl and runs on all major platforms
 * (including Windows) so no portability issues are introduced into your
 * application by utilizing this class.
 *
 * ## Usage
 *
 * Assuming ExifTool is installed on the host system correctly and either in the
 * system path, using this class to communicate with ExifTool is as simple as
 * creating an instance:
 *
 * ```java
 * ExifTool tool = new ExifToolBuilder().build();
 * ```
 *
 * This mode assume that:
 * - Path is set as an environment variable (i.e. `-Dexiftool.path=/usr/local/exiftool/bin/exiftool`).
 * - Or globally available.
 *
 * If you want to set the path of ExifTool, you can also specify it during creation:
 *
 * ```java
 * ExifTool tool = new ExifToolBuilder()
 *   .path("/usr/local/exiftool/bin/exiftool")
 *   .build();
 * ```
 *
 * Once created, usage is as simple as making calls to {@link #getImageMeta(File, Tag...)} or
 * {@link #getImageMeta(File, Format, Tag...)} with a list of {@link Tag}s you
 * want to pull values for from the given image.
 *
 * In this default mode, calls to `getImageMeta` will automatically
 * start an external ExifTool process to handle the request. After ExifTool has
 * parsed the tag values from the file, the external process exits and this
 * class parses the result before returning it to the caller.
 *
 * Results from calls to `getImageMeta` are returned in a {@link Map}
 * with the {@link Tag} values as the keys and {@link String} values for every
 * tag that had a value in the image file as the values. {@link Tag}s with no
 * value found in the image are omitted from the result map.
 *
 * While each {@link Tag} provides a hint at which format the resulting value
 * for that tag is returned as from ExifTool (see {@link Tag#getType()}), that
 * only applies to values returned with an output format of
 * {@link Format#NUMERIC} and it is ultimately up to the caller to decide how
 * best to parse or convert the returned values.
 *
 * The {@link Tag} Enum provides the {@link Tag#parseValue(Tag, String)}
 * convenience method for parsing given `String` values according to
 * the Tag hint automatically for you if that is what you plan on doing,
 * otherwise feel free to handle the return values anyway you want.
 *
 * ## ExifTool -stay_open Support
 *
 * ExifTool <a href="http://u88.n24.queensu.ca/exiftool/forum/index.php/topic,1402.msg12933.html#msg12933">8.36</a>
 * added a new persistent-process feature that allows ExifTool to stay
 * running in a daemon mode and continue accepting commands via a file or stdin.
 *
 * This new mode is controlled via the `-stay_open True/False`
 * command line argument and in a busy system that is making thousands of calls
 * to ExifTool, can offer speed improvements of up to <strong>60x</strong> (yes,
 * really that much).
 *
 * This feature was added to ExifTool shortly after user
 * <a href="http://www.christian-etter.de/?p=458">Christian Etter discovered</a> the overhead
 * for starting up a new Perl interpreter each time ExifTool is loaded accounts for
 * roughly <a href="http://u88.n24.queensu.ca/exiftool/forum/index.php/topic,1402.msg6121.html#msg6121">98.4% of the total runtime</a>.
 *
 * Support for using ExifTool in daemon mode is enabled by passing
 * {@link Feature#STAY_OPEN} to the constructor of the class when creating an
 * instance of this class and then simply using the class as you normally would.
 * This class will manage a single ExifTool process running in daemon mode in
 * the background to service all future calls to the class.
 *
 * Because this feature requires ExifTool 8.36 or later, this class will
 * actually verify support for the feature in the version of ExifTool
 * before successfully instantiating the class and will notify you via
 * an {@link com.thebuzzmedia.exiftool.exceptions.UnsupportedFeatureException} if the native
 * ExifTool doesn't support the requested feature.
 *
 * In the event of an {@link com.thebuzzmedia.exiftool.exceptions.UnsupportedFeatureException}, the caller can either
 * upgrade the native ExifTool upgrade to the version required or simply avoid
 * using that feature to work around the exception.
 *
 * ## Automatic Resource Cleanup
 *
 * When {@link Feature#STAY_OPEN} mode is used, there is the potential for
 * leaking both host OS processes (native 'exiftool' processes) as well as the
 * read/write streams used to communicate with it unless {@link #close()} is
 * called to clean them up when done. <strong>Fortunately</strong>, this class
 * provides an automatic cleanup mechanism that runs, by default, after 10 mins
 * of inactivity to clean up those stray resources.
 *
 * The inactivity period can be controlled by modifying the
 * {@link #PROCESS_CLEANUP_DELAY} system variable. A value of <code>0</code> or
 * less disabled the automatic cleanup process and requires you to cleanup
 * ExifTool instances on your own by calling {@link #close()} manually.
 *
 * Any class activity by way of calls to <code>getImageMeta</code> will always
 * reset the inactivity timer, so in a busy system the cleanup thread could
 * potentially never run, leaving the original host ExifTool process running
 * forever (which is fine).
 *
 * This design was chosen to help make using the class and not introducing
 * memory leaks and bugs into your code easier as well as making very inactive
 * instances of this class light weight while not in-use by cleaning up after
 * themselves.
 *
 * The only overhead incurred when opening the process back up is a 250-500ms
 * lag while launching the VM interpreter again on the first call (depending on
 * host machine speed and load).
 *
 * ## Reusing a "closed" ExifTool Instance
 * If you or the cleanup thread have called {@link #close()} on an instance of
 * this class, cleaning up the host process and read/write streams, the instance
 * of this class can still be safely used. Any followup calls to
 * <code>getImageMeta</code> will simply re-instantiate all the required
 * resources necessary to service the call (honoring any {@link Feature}s set).
 *
 * This can be handy behavior to be aware of when writing scheduled processing
 * jobs that may wake up every hour and process thousands of pictures then go
 * back to sleep. In order for the process to execute as fast as possible, you
 * would want to use ExifTool in daemon mode (pass {@link Feature#STAY_OPEN} to
 * the constructor of this class) and when done, instead of {@link #close()}-ing
 * the instance of this class and throwing it out, you can keep the reference
 * around and re-use it again when the job executes again an hour later.
 *
 * ## Performance
 *
 * Extra care is taken to ensure minimal object creation or unnecessary CPU
 * overhead while communicating with the external process.
 *
 * {@link Pattern}s used to split the responses from the process are explicitly
 * compiled and reused, string concatenation is minimized, Tag name lookup is
 * done via a <code>static final</code> {@link Map} shared by all instances and
 * so on.
 *
 * Additionally, extra care is taken to utilize the most optimal code paths when
 * initiating and using the external process, for example, the
 * {@link ProcessBuilder#command(List)} method is used to avoid the copying of
 * array elements when {@link ProcessBuilder#command(String...)} is used and
 * avoiding the (hidden) use of {@link StringTokenizer} when
 * {@link Runtime#exec(String)} is called.
 *
 * All of this effort was done to ensure that imgscalr and its supporting
 * classes continue to provide best-of-breed performance and memory utilization
 * in long running/high performance environments (e.g. web applications).
 *
 * ## Thread Safety
 *
 * Instances of this class are **not** Thread-safe. Both the
 * instance of this class and external ExifTool process maintain state specific
 * to the current operation. Use of instances of this class need to be
 * synchronized using an external mechanism or in a highly threaded environment
 * (e.g. web application), instances of this class can be used along with
 * {@link ThreadLocal}s to ensure Thread-safe, highly parallel use.
 *
 * ## Why ExifTool?
 *
 * <a href="http://www.sno.phy.queensu.ca/~phil/exiftool">ExifTool</a> is
 * written in Perl and requires an external process call from Java to make use
 * of.
 *
 * While this would normally preclude a piece of software from inclusion into
 * the imgscalr library (more complex integration), there is no other image
 * metadata piece of software available as robust, complete and well-tested as
 * ExifTool. In addition, ExifTool already runs on all major platforms
 * (including Windows), so there was not a lack of portability introduced by
 * providing an integration for it.
 *
 * Allowing it to be used from Java is a boon to any Java project that needs the
 * ability to read/write image-metadata from almost
 * <a href="http://www.sno.phy.queensu.ca/~phil/exiftool/#supported">any image or video file</a> format.
 *
 * ## Alternatives
 *
 * If integration with an external Perl process is something your app cannot do
 * and you still need image metadata-extraction capability, Drew Noakes has
 * written the 2nd most robust image metadata library I have come
 * across: <a href="http://drewnoakes.com/drewnoakes.com/code/exif/">Metadata Extractor</a>
 * that you might want to look at.
 *
 * @author Riyad Kalla (software@thebuzzmedia.com)
 * @author Mickael Jeanroy (mickael.jeanroy@gmail.com)
 * @since 1.1
 */
public class ExifTool implements AutoCloseable {

	/**
	 * Internal Logger.
	 * Will used slf4j, log4j or internal implementation.
	 */
	private static final Logger log = LoggerFactory.getLogger(ExifTool.class);

	/**
	 * Interval (in milliseconds) of inactivity before the cleanup thread wakes
	 * up and cleans up the daemon ExifTool process and the read/write streams
	 * used to communicate with it when the {@link Feature#STAY_OPEN} feature is
	 * used.
	 * <p/>
	 * Ever time a call to <code>getImageMeta</code> is processed, the timer
	 * keeping track of cleanup is reset; more specifically, this class has to
	 * experience no activity for this duration of time before the cleanup
	 * process is fired up and cleans up the host OS process and the stream
	 * resources.
	 * <p/>
	 * Any subsequent calls to <code>getImageMeta</code> after a cleanup simply
	 * re-initializes the resources.
	 * <p/>
	 * This system property can be set on startup with:<br/>
	 * <code>
	 * -Dexiftool.processCleanupDelay=600000
	 * </code> or by calling {@link System#setProperty(String, String)} before
	 * this class is loaded.
	 * <p/>
	 * Setting this value to 0 disables the automatic cleanup thread completely
	 * and the caller will need to manually cleanup the external ExifTool
	 * process and read/write streams by calling {@link #close()}.
	 * <p/>
	 * Default value is <code>600,000</code> (10 minutes).
	 */
	public static final long PROCESS_CLEANUP_DELAY = Long.getLong(
		"exiftool.processCleanupDelay", 600000);

	/**
	 * Name used to identify the (optional) cleanup {@link Thread}.
	 * <p/>
	 * This is only provided to make debugging and profiling easier for
	 * implementors making use of this class such that the resources this class
	 * creates and uses (i.e. Threads) are readily identifiable in a running VM.
	 * <p/>
	 * Default value is "<code>ExifTool Cleanup Thread</code>".
	 */
	protected static final String CLEANUP_THREAD_NAME = "ExifTool Cleanup Thread";

	private Timer cleanupTimer;

	private TimerTask currentCleanupTask;

	/**
	 * Set of features enabled on this exif tool
	 * instance.
	 */
	private final Set<Feature> features;

	/**
	 * Command Executor.
	 * This executor will be used to execute exiftool process and commands.
	 */
	private final CommandExecutor executor;

	/**
	 * Exiftool Path.
	 * Path is first read from `exiftool.path` system property,
	 * otherwise `exiftool` must be globally available.
	 */
	private final String path;

	/**
	 * This is the version detected on exiftool executable.
	 * This version depends on executable given on instantiation.
	 */
	private final String version;

	private CommandProcess process;

	/**
	 * Create new ExifTool instance.
	 * When exiftool is created, it will try to activate some features.
	 * If feature is not available on this specific exiftool version, then
	 * an it an {@link UnsupportedFeatureException} will be thrown.
	 *
	 * @param features List of features to activate.
	 * @throws UnsupportedFeatureException If feature is not available for this exiftool version.
	 */
	ExifTool(String path, CommandExecutor executor, Set<Feature> features) {
		this.executor = notNull(executor, "Executor should not be null");
		this.path = notNull(path, "ExifTool path should not be null");
		this.version = parseVersion();
		this.features = parseFeature(features);

		/*
		 * Now that initialization is done, init the cleanup timer if we are
		 * using STAY_OPEN and the delay time set is non-zero.
		 */
		if (isFeatureEnabled(Feature.STAY_OPEN) && PROCESS_CLEANUP_DELAY > 0) {
			this.cleanupTimer = new Timer(CLEANUP_THREAD_NAME, true);

			// Start the first cleanup task counting down.
			resetCleanupTask();
		}
	}

	/**
	 * Parse Version from this exiftool instance.
	 * This function need to execute exiftool external process.
	 *
	 * Executed command is:
	 * <exiftool> -ver
	 *
	 * where:
	 * - <exiftool> is the path to exiftool executable.
	 * - -ver is the only one argument.
	 *
	 * This function should remain private since it is used directly in the constructor (and we
	 * don't want to escape this object).
	 *
	 * @return Exiftool version, null if command failed.
	 */
	private String parseVersion() {
		log.debug("Checking exiftool version");

		Command command = CommandBuilder.builder(path)
			.addArgument("-ver")
			.build();

		CommandResult result = executor.execute(command);
		return result.isSuccess() ? result.getOutput() : null;
	}

	/**
	 * This function will returned available feature from this exiftool instances.
	 *
	 * Each feature given in parameters is checked:
	 * - If feature is supported by this exiftool instance, then OK.
	 * - Otherwise, an instance of {@link UnsupportedFeatureException} is thrown.
	 *
	 * If an instance of {@link UnsupportedFeatureException} is thrown, it means that:
	 * - You should update exiftool.
	 * - If you can't update exiftool, then you should not use this feature.
	 *
	 * This function should remain private since it is used directly in the constructor (and we
	 * don't want to escape this object).
	 *
	 * @param features Features to check.
	 * @return Set of available features.
	 * @throws UnsupportedFeatureException If a feature is not supported.
	 */
	private Set<Feature> parseFeature(Set<Feature> features) {
		log.debug("Parsing activated features");

		// Store it in a set, this will avoid duplicates
		for (Feature feature : features) {
			boolean supported = feature.isSupported(version);
			if (supported) {
				log.debug("Feature %s SUPPORTED", feature);
			} else {
				log.error("Feature %s NOT SUPPORTED", feature);
				throw new UnsupportedFeatureException(version, feature);
			}
		}

		return unmodifiableSet(features);
	}

	/**
	 * Used to shutdown the external ExifTool process and close the read/write
	 * streams used to communicate with it when {@link Feature#STAY_OPEN} is
	 * enabled.
	 *
	 * **NOTE**: Calling this method does not preclude this
	 * instance of {@link ExifTool} from being re-used, it merely disposes of
	 * the native and internal resources until the next call to
	 * <code>getImageMeta</code> causes them to be re-instantiated.
	 *
	 * The cleanup thread will automatically call this after an interval of
	 * inactivity defined by {@link #PROCESS_CLEANUP_DELAY}.
	 *
	 * Calling this method on an instance of this class without
	 * {@link Feature#STAY_OPEN} support enabled has no effect.
	 */
	@Override
	public void close() throws Exception {
		if (!isRunning()) {
			// no-op if the underlying process and streams have already been closed
			// OR if stayOpen was never used in the first place in which case
			// nothing is open right now anyway.
			return;
		}

		// First, try to stop cleanup task
		// If task is not stopped, it may be executed later

		try {
			log.debug("Attempting to stop cleanup task");
			stopCleanupTask();
			log.debug("Cleanup task successfully stopped");
		}
		catch (Exception ex) {
			log.warn("Cleanup task failed to stop");
			log.warn(ex.getMessage(), ex);
		}

		// Then, stop current process

		try {
			// If ExifTool was used in stayOpen mode but getImageMeta was never
			// called then the streams were never initialized and there is nothing
			// to shut down or destroy, otherwise we need to close down all the
			// resources in use.
			log.debug("Attempting to close ExifTool daemon process, issuing '-stay_open\\nFalse\\n' command...");
			process.close();
			log.debug("ExifTool daemon process successfully closed");
		}
		catch (Exception ex) {
			// Log some warnings.
			log.warn("ExifTool daemon failed to stop");
			log.warn(ex.getMessage(), ex);

			// Re-throw the error, this will let the caller do what he wants with the exception.
			throw ex;
		}
		finally {
			// Dot not forget to set it to null.
			process = null;
		}
	}

	/**
	 * For {@link ExifTool} instances with {@link Feature#STAY_OPEN} support
	 * enabled, this method is used to determine if there is currently a running
	 * ExifTool process associated with this class.
	 *
	 * Any dependent processes and streams can be shutdown using
	 * {@link #close()} and this class will automatically re-create them on the
	 * next call to <code>getImageMeta</code> if necessary.
	 *
	 * @return <code>true</code> if there is an external ExifTool process in
	 * daemon mode associated with this class utilizing the
	 * {@link Feature#STAY_OPEN} feature, otherwise returns
	 * <code>false</code>.
	 */
	public boolean isRunning() {
		return process != null && process.isRunning();
	}

	// Implement finalizer.
	// This is just a small security: it should not prevent caller
	// to explicitly close the exiftool process (but add this finalizer
	// if someone forget).
	@Override
	protected void finalize() throws Throwable {
		// Be sure process is closed
		close();

		// Just delegate to original finalizer implementation
		super.finalize();
	}

	/**
	 * Exiftool version pointed by this instance.
	 *
	 * @return Version.
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Used to determine if the given {@link Feature} has been enabled for this
	 * particular instance of {@link ExifTool}.
	 * <p/>
	 *
	 * @param feature The feature to check if it has been enabled for us or not on
	 * this instance.
	 * @return <code>true</code> if the given {@link Feature} is currently
	 * enabled on this instance of {@link ExifTool}, otherwise returns
	 * <code>false</code>.
	 * @throws NullPointerException if <code>feature</code> is <code>null</code>.
	 */
	public boolean isFeatureEnabled(Feature feature) throws IllegalArgumentException {
		return features.contains(notNull(feature, "Feature cannot be null"));
	}

	/**
	 * Parse image metadata.
	 * Output format is numeric.
	 *
	 * @param image Image.
	 * @param tags List of tags to extract.
	 * @return Pair of tag associated with the value.
	 * @throws IOException If something bad happen during I/O operations.
	 * @throws NullPointerException If one parameter is null.
	 * @throws IllegalArgumentException If list of tag is empty.
	 * @throws com.thebuzzmedia.exiftool.exceptions.UnreadableFileException If image cannot be read.
	 */
	public Map<Tag, String> getImageMeta(File image, Tag... tags) throws IOException {
		return getImageMeta(image, Format.NUMERIC, tags);
	}

	/**
	 * Parse image metadata.
	 *
	 * @param image Image.
	 * @param format Output format.
	 * @param tags List of tags to extract.
	 * @return Pair of tag associated with the value.
	 * @throws IOException If something bad happen during I/O operations.
	 * @throws NullPointerException If one parameter is null.
	 * @throws IllegalArgumentException If list of tag is empty.
	 * @throws com.thebuzzmedia.exiftool.exceptions.UnreadableFileException If image cannot be read.
	 */
	public Map<Tag, String> getImageMeta(File image, Format format, Tag... tags) throws IOException {
		notNull(image, "Image cannot be null and must be a valid stream of image data.");
		notNull(format, "Format cannot be null.");
		notEmpty(tags, "Tags cannot be null and must contain 1 or more Tag to query the image for.");
		isReadable(image, "Unable to read the given image [%s], ensure that the image exists at the given path and that the executing Java process has permissions to read it.", image);

		log.debug("Querying %s tags from image: %s", tags.length, image);

		// Create a result map big enough to hold results for each of the tags
		// and avoid collisions while inserting.
		final TagHandler tagHandler = new TagHandler(tags.length * 3);

		// Build list of exiftool arguments.
		final List<String> args = getImageMetaArguments(format, image, tags);

		// Using ExifTool in daemon mode (-stay_open True) executes different
		// code paths below. So establish the flag for this once and it is
		// reused a multitude of times later in this method to figure out where
		// to branch to.
		final boolean stayOpen = features.contains(Feature.STAY_OPEN);

		// Execute exiftool
		if (stayOpen) {
			doGetImageMetaStayOpen(tagHandler, args);
		} else {
			doGetImageMeta(tagHandler, args);
		}

		// Add some debugging log
		log.debug("Image Meta Processed [queried %s tags and found %s values]", tags.length, tagHandler.size());

		return tagHandler.getTags();
	}

	/**
	 * Parse image metadata.
	 * If exiftool process is not already started, then it will be opened
	 * with stay_open flag.
	 *
	 * @param tagHandler Tag handler, will be executed for each line output.
	 * @param args Command arguments.
	 * @throws IOException If something bad happen process input/output operations.
	 */
	private void doGetImageMetaStayOpen(TagHandler tagHandler, List<String> args) throws IOException {
		log.debug("Using ExifTool in daemon mode (-stay_open True)...");

		// Always reset the cleanup task.
		resetCleanupTask();

		// Start deamon process if it is not already started.
		start();

		process.write(map(args, streamArgumentMapper()));
		process.flush();
		process.read(tagHandler);
	}

	/**
	 * Parse image metadata using new exiftool process.
	 * Process is automatically closed when finished.
	 *
	 * @param tagHandler Tag handler, will be executed for each line output.
	 * @param args Command arguments.
	 */
	private void doGetImageMeta(TagHandler tagHandler, List<String> args) {
		log.debug("Using ExifTool in non-daemon mode (-stay_open False)...");

		Command cmd = CommandBuilder.builder(path)
			.addAll(args)
			.build();

		executor.execute(cmd, tagHandler);
	}

	/**
	 * Write image metadata.
	 * Default format is numeric.
	 *
	 * @param image Image.
	 * @param tags Tags to write.
	 * @throws IOException If an error occurs during write operation.
	 */
	public void setImageMeta(File image, Map<Tag, String> tags) throws IOException {
		setImageMeta(image, Format.NUMERIC, tags);
	}

	/**
	 * Write image metadata in a specific format.
	 *
	 * @param image Image.
	 * @param format Specified format.
	 * @param tags Tags to write.
	 * @throws IOException If an error occurs during write operation.
	 */
	public void setImageMeta(File image, Format format, Map<Tag, String> tags) throws IOException {
		notNull(image, "Image cannot be null and must be a valid stream of image data.");
		notNull(format, "Format cannot be null.");
		notEmpty(tags, "Tags cannot be null and must contain 1 or more Tag to query the image for.");
		isWritable(image, "Unable to read the given image [%s], ensure that the image exists at the given path and that the executing Java process has permissions to read it.", image);

		final long startTime = System.currentTimeMillis();

		log.debug("Writing %d tags to image: %s", tags.size(), image);

		// Using ExifTool in daemon mode (-stay_open True) executes different
		// code paths below. So establish the flag for this once and it is
		// reused a multitude of times later in this method to figure out where
		// to branch to.
		final boolean stayOpen = features.contains(Feature.STAY_OPEN);

		if (stayOpen) {
			doSetImageMetaStayOpen(image, format, tags);
		} else {
			doSetImageMeta(image, format, tags);
		}

		log.debug("Image Meta Processed in %d ms [write %d tags]", System.currentTimeMillis() - startTime, tags.size());
	}

	/**
	 * Write image metadata using stay_open flag.
	 * If daemon process is not already started, it will be automatically open.
	 *
	 * @param image Image.
	 * @param format Specified format.
	 * @param tags Tags to write.
	 * @throws IOException If an error occurs during write operation on opened process.
	 */
	private void doSetImageMetaStayOpen(File image, Format format, Map<Tag, String> tags) throws IOException {
		// Always reset the cleanup task.
		resetCleanupTask();

		// Start deamon process if it is not already started.
		start();

		process.write(map(setImageMetaArguments(format, image, tags), streamArgumentMapper()));
		process.flush();
		process.read(stopHandler());
	}

	/**
	 * Write image metadata.
	 * A new process will be started and closed at the end of the operation.
	 *
	 * @param image Image.
	 * @param format Specified format.
	 * @param tags Tags to write.
	 */
	private void doSetImageMeta(File image, Format format, Map<Tag, String> tags) {
		log.debug("Using ExifTool in non-daemon mode (-stay_open False)...");

		// Since we are not using a stayOpen process, we need to setup the
		// execution arguments completely each time.
		Command cmd = CommandBuilder.builder(path)
			.addAll(setImageMetaArguments(format, image, tags))
			.build();

		executor.execute(cmd, stopHandler());
	}

	/**
	 * Helper method used to make canceling the current task and scheduling a
	 * new one easier.
	 *
	 * It is annoying that we cannot just reset the timer on the task, but that
	 * isn't the way the java.util.Timer class was designed unfortunately.
	 */
	private void resetCleanupTask() {
		// no-op if the timer was never created.
		if (cleanupTimer == null) {
			return;
		}

		stopCleanupTask();
		startCleanupTask();
	}

	/**
	 * Start new clean task.
	 *
	 * This task will be scheduled and will run in the
	 * specified delay.
	 *
	 * If no timer has been initialized, then this method
	 * is a no-op.
	 */
	private void startCleanupTask() {
		// no-op if the timer was never created.
		if (cleanupTimer == null) {
			return;
		}

		log.debug("Starting cleanup task");

		// Create task
		currentCleanupTask = new CleanupTimerTask(this);

		// Schedule a new cleanup task.
		cleanupTimer.schedule(currentCleanupTask, PROCESS_CLEANUP_DELAY, PROCESS_CLEANUP_DELAY);
	}

	/**
	 * Stop pending clean task.
	 *
	 * If no timer has been initialized, then this method
	 * is a no-op.
	 */
	private void stopCleanupTask() {
		// no-op if the timer was never created.
		if (cleanupTimer == null) {
			return;
		}

		if (currentCleanupTask != null) {
			log.debug("Stopping cleanup task");
			currentCleanupTask.cancel();
			currentCleanupTask = null;
		}
	}

	/**
	 * Build argument list to parse image metadata using exiftool command
	 * line.
	 *
	 * @param format Output format.
	 * @param image Image.
	 * @param tags List of tags.
	 * @return List of associated arguments.
	 */
	private List<String> getImageMetaArguments(Format format, File image, Tag[] tags) {
		List<String> args = new LinkedList<String>();

		// Format output.
		args.addAll(format.getArgs());

		// Compact output.
		args.add("-S");

		// Add tags arguments.
		for (Tag tag : tags) {
			args.add("-" + tag.getName());
		}

		// Add image argument.
		args.add(image.getAbsolutePath());

		// Add last argument.
		// This argument will only be used by exiftool if stay_open flag has been set.
		args.add("-execute");

		return args;
	}

	/**
	 * Build argument list to parse image metadata using exiftool command
	 * line.
	 *
	 * @param format Output format.
	 * @param image Image.
	 * @param tags List of tags.
	 * @return List of associated arguments.
	 */
	private List<String> setImageMetaArguments(Format format, File image, Map<Tag, String> tags) {
		List<String> args = new LinkedList<String>();

		// Format output.
		args.addAll(format.getArgs());

		// Compact output.
		args.add("-S");

		// Add tags arguments.
		for (Map.Entry<Tag, String> entry : tags.entrySet()) {
			args.add("-" + entry.getKey().getName() + "=" + entry.getValue());
		}

		// Add image argument.
		args.add(image.getAbsolutePath());

		// Add last argument.
		// This argument will only be used by exiftool if stay_open flag has been set.
		args.add("-execute");

		return args;
	}

	/**
	 * Start exiftool process with stay_open flag.
	 * If exiftool process is already started, then this function is a no-op.
	 */
	private void start() {
		// If this is our first time calling getImageMeta with a "stayOpen"
		// connection, set up the persistent process and run it so it is
		// ready to receive commands from us.
		if (process == null || process.isClosed()) {
			log.debug("Start exiftool process");

			Command cmd = CommandBuilder.builder(path)
				.addArgument("-stay_open", "True", "-@", "-")
				.build();

			process = executor.start(cmd);
		}
	}

	/**
	 * Class used to represent the {@link TimerTask} used by the internal auto
	 * cleanup {@link Timer} to call {@link ExifTool#close()} after a specified
	 * interval of inactivity.
	 *
	 * @author Riyad Kalla (software@thebuzzmedia.com)
	 * @since 1.1
	 */
	private class CleanupTimerTask extends TimerTask {
		private ExifTool owner;

		public CleanupTimerTask(ExifTool owner) throws IllegalArgumentException {
			if (owner == null)
				throw new IllegalArgumentException(
					"owner cannot be null and must refer to the ExifTool instance creating this task.");

			this.owner = owner;
		}

		@Override
		public void run() {
			log.debug("\tAuto cleanup task running...");
			try {
				owner.close();
			}
			catch (Exception ex) {
				log.error(ex.getMessage(), ex);
			}
		}
	}
}
