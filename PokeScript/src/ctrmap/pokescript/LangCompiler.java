package ctrmap.pokescript;

import ctrmap.scriptformats.gen6.GFLPawnScript;
import ctrmap.pokescript.instructions.providers.CTRInstructionProvider;
import ctrmap.pokescript.instructions.providers.AInstructionProvider;
import ctrmap.pokescript.instructions.providers.VInstructionProvider;
import ctrmap.pokescript.stage0.CompilerPragma;
import ctrmap.pokescript.stage0.Preprocessor;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.stage2.CTRAssembler;
import ctrmap.pokescript.stage2.VAssembler;
import ctrmap.scriptformats.gen5.VScriptFile;
import ctrmap.scriptformats.pkslib.LibraryFile;
import ctrmap.stdlib.cli.ArgumentBuilder;
import ctrmap.stdlib.cli.ArgumentContent;
import ctrmap.stdlib.cli.ArgumentPattern;
import ctrmap.stdlib.cli.ArgumentType;
import ctrmap.stdlib.fs.FSFile;
import ctrmap.stdlib.fs.FSUtil;
import ctrmap.stdlib.fs.accessors.DiskFile;
import ctrmap.stdlib.io.base.iface.ReadableStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class LangCompiler {

	public static final String COMPILER_VERSION = "0.4.2@2021/04/11";

	public static final ArgumentPattern[] langCompilerArgConfig = new ArgumentPattern[]{
		new ArgumentPattern("target", "Target platform (ntrv/ntriv/ctr)", ArgumentType.STRING, LangPlatform.AMX_CTR.name, true, "-t", "--target"),
		new ArgumentPattern("defines", "Preprocessor definitions (can be chained)", ArgumentType.STRING, null, true, "-D", "--define"),
		new ArgumentPattern("includes", "Include directory paths (can be chained)", ArgumentType.STRING, null, true, "-I", "--include"),
		new ArgumentPattern("inputs", "List of files to compile (can be chained, default)", ArgumentType.STRING, null, true, "-i", "--input"),
		new ArgumentPattern("output", "Optional output file specification (only allowed for one input)", ArgumentType.STRING, null, "-o", "--output"),
		new ArgumentPattern("logfile", "Optional log file for output.", ArgumentType.STRING, null, "-l", "--logfile"),
		new ArgumentPattern("optLevel", "Compiler optimization level", ArgumentType.INT, 2, "-O", "--opt-level"),
		new ArgumentPattern("optPassCount", "Compiler optimization pass count", ArgumentType.INT, 2, "-p", "--opt-pass-count"),
		new ArgumentPattern("help", "Prints this help dialog.", ArgumentType.BOOLEAN, false, "-h", "-?", "--help")
	};

	public static void main(String[] args) {
		System.out.println("* * * New PokéScript compiler U version " + COMPILER_VERSION + " * * *\n");
		System.out.println("PokéScript is part of CTRMap at https://github.com/HelloOO7/CTRMap-BleedingEdge\n");
		ArgumentBuilder bld = new ArgumentBuilder(langCompilerArgConfig);
		bld.parse(args);
		ArgumentContent inputPaths = bld.getContent("inputs");
		if (inputPaths.contents.isEmpty() && bld.defaultContent.contents.isEmpty()) {
			System.out.println("No inputs given. Stopping.\n");
			bld.print();
		} else {
			//Print help when requested
			if (bld.getContent("help").booleanValue()) {
				bld.print();
			}

			CompilerArguments ca = new CompilerArguments();

			//Common arguments
			ca.optimizationLevel = bld.getContent("optLevel").intValue();
			ca.optimizationPassCount = bld.getContent("optPassCount").intValue();
			ArgumentContent log = bld.getContent("logfile", true);
			CompilerLogger.FileLogger fileLog = null;
			if (log != null) {
				fileLog = new CompilerLogger.FileLogger(new DiskFile(log.stringValue()));
				ca.logger = fileLog;
			}

			String target = bld.getContent("target").stringValue();
			String binaryExtension;

			LangPlatform plaf = LangPlatform.fromName(target);

			binaryExtension = plaf.extensionFilter.getPrimaryExtension();
			ca.platform = plaf;

			if (target == null) {
				throw new IllegalArgumentException("Invalid target platform: " + target);
			}

			//Build input file list
			List<FSFile> inputs = new ArrayList<>();
			for (int i = 0; i < inputPaths.contents.size(); i++) {
				inputs.add(new DiskFile(inputPaths.stringValue(i)));
			}
			for (int i = 0; i < bld.defaultContent.contents.size(); i++) {
				inputs.add(new DiskFile(bld.defaultContent.stringValue(i)));
			}

			//Get optional output files
			ArgumentContent output = bld.getContent("output", true);
			FSFile outputFile = null;
			if (output != null) {
				if (inputs.size() > 1) {
					throw new UnsupportedOperationException("Output file can only be specified for one source file at a time.");
				}
				outputFile = new DiskFile(output.stringValue());
			}

			//BUILD INCLUDES
			ArgumentContent incArg = bld.getContent("includes");
			List<FSFile> commonIncludes = new ArrayList<>();

			commonIncludes.add(new DiskFile("."));

			for (int i = 0; i < incArg.contents.size(); i++) {
				FSFile df = new DiskFile(incArg.stringValue(i));
				if (LangConstants.isLangLib(df.getName())) {
					df = new LibraryFile(df);
				}
				commonIncludes.add(df);
			}

			//BUILD DEFINES
			ArgumentContent defArg = bld.getContent("defines");
			List<String> commonDefinitions = new ArrayList<>();

			for (int i = 0; i < defArg.contents.size(); i++) {
				commonDefinitions.add(defArg.stringValue(i));
			}
			ca.includeRoots = commonIncludes;
			ca.preprocessorDefinitions = commonDefinitions;

			for (FSFile in : inputs) {
				CompilerArguments a2 = new CompilerArguments(ca);
				a2.includeRoots.add(in.getParent());
				System.out.println("Compiling file " + in + "...");
				FSFile out;
				if (outputFile != null) {
					out = outputFile;
				} else {
					out = in.getParent().getChild(FSUtil.getFileNameWithoutExtension(in.getName()) + binaryExtension);
				}
				byte[] exe = compileFileToBinary(in, a2);
				if (exe != null) {
					FSUtil.writeBytesToFile(out, exe);
				}
			}

			if (fileLog != null) {
				fileLog.close();
			}
		}
	}

	public static AInstructionProvider getInstructionProvider(LangPlatform platform) {
		switch (platform) {
			case AMX_CTR:
				return new CTRInstructionProvider();
			case EV_SWAN:
				return new VInstructionProvider();
			case AMX_NX:
			case EV_PL:
				throw new UnsupportedOperationException("Platform " + platform + " not yet supported!");
		}
		return null;
	}

	public static byte[] compileFileToBinary(FSFile fsf, CompilerArguments args) {
		switch (args.platform) {
			case AMX_CTR:
				return compileFileCTR(fsf, args).getScriptBytes();
			case EV_SWAN:
				return compileFileV(fsf, args).getBinaryData();
		}
		return null;
	}

	public static byte[] compileStreamToBinary(ReadableStream strm, CompilerArguments args) {
		switch (args.platform) {
			case AMX_CTR:
				return compileStreamCTR(strm, args).getScriptBytes();
			case EV_SWAN:
				return compileStreamV(strm, args).getBinaryData();
		}
		return null;
	}

	public static GFLPawnScript compileFileCTR(FSFile fsf, CompilerArguments args) {
		Preprocessor preprocessor = new Preprocessor(fsf, args);
		return compileImplCTR(preprocessor, args);
	}

	public static GFLPawnScript compileStreamCTR(ReadableStream strm, CompilerArguments args) {
		Preprocessor preprocessor = new Preprocessor(strm, "UnnamedContext", args);
		return compileImplCTR(preprocessor, args);
	}

	public static VScriptFile compileFileV(FSFile fsf, CompilerArguments args) {
		Preprocessor preprocessor = new Preprocessor(fsf, args);
		return compileImplGenV(preprocessor, args);
	}

	public static VScriptFile compileStreamV(ReadableStream strm, CompilerArguments args) {
		Preprocessor preprocessor = new Preprocessor(strm, "UnnamedContext", args);
		return compileImplGenV(preprocessor, args);
	}

	private static GFLPawnScript compileImplCTR(Preprocessor preprocessor, CompilerArguments args) {
		NCompileGraph cg = preprocessor.getCompileGraph();
		if (cg == null) {
			return null;
		}
		GFLPawnScript gflPawnScript = new GFLPawnScript();
		CTRAssembler.assemble(cg, gflPawnScript, args);
		return gflPawnScript;
	}

	private static VScriptFile compileImplGenV(Preprocessor preprocessor, CompilerArguments args) {
		NCompileGraph cg = preprocessor.getCompileGraph();
		if (cg == null) {
			return null;
		}
		return VAssembler.assemble(cg, args);
	}

	public static class CompilerArguments {

		public int optimizationPassCount = 2;
		public int optimizationLevel = 2;
		public List<FSFile> includeRoots = new ArrayList<>();
		public List<String> preprocessorDefinitions = new ArrayList<>();
		public Map<CompilerPragma, CompilerPragma.PragmaValue> pragmata = new HashMap<>();
		public CompilerLogger logger = new CompilerLogger.ConsoleLogger();

		private LangPlatform platform;
		public AInstructionProvider provider;

		public CompilerArguments() {
			setPlatform(LangPlatform.AMX_CTR);
		}

		public void setPlatform(LangPlatform plaf) {
			this.platform = plaf;
			provider = getInstructionProvider(plaf);
		}

		public LangPlatform getPlatform() {
			return platform;
		}

		public CompilerArguments(CompilerArguments mirror) {
			optimizationPassCount = mirror.optimizationPassCount;
			optimizationLevel = mirror.optimizationLevel;
			includeRoots.addAll(mirror.includeRoots);
			preprocessorDefinitions.addAll(mirror.preprocessorDefinitions);
			logger = mirror.logger;
			platform = mirror.platform;
			provider = mirror.provider;
		}
	}
}
