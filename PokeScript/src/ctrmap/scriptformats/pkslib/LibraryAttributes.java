package ctrmap.scriptformats.pkslib;

public class LibraryAttributes {

	/**
	 * Indicates that the library contains multiple source packages for
	 * different platform/SDK targets.
	 */
	public static final String AK_MR_ENABLE = "MultiRelease";
	
	/**
	 * If multi-release is not used with this library, specifies the platform that the library should target.
	 */
	public static final String AK_SR_PLAF = "SingleTargetPlatform";

	/**
	 * Specifies bindings of source packages to platform names.
	 */
	public static final String AK_PATH_LIST = "SourceDirectories";

	/**
	 * Specifies a friendly name of the library for use with IDE/package manager
	 * UIs.
	 */
	public static final String AK_PROD_NAME = "ProductName";

	/**
	 * Specifies a unique identifier for the library with which it should be
	 * looked up/referenced in projects/build configurations.
	 */
	public static final String AK_PROD_ID = "ProductIdentifier";

	/**
	 * Contains an optional list of ProductIndentifiers of libraries that are to
	 * be included on the classpath for this library to be successfully built.
	 */
	public static final String AK_COMPILE_DEPS = "Dependencies";
}
