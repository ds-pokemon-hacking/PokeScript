
package ctrmap.pokescript;

import ctrmap.stdlib.fs.FSFile;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class CompilerLogger {
	public abstract void stdout(String text);
	public abstract void stderr(String text);
	
	public void println(LogLevel level, String text){
		print(level, text + '\n');
	}
	
	public void print(LogLevel level, String text){
		text = "[" + level + "] " + text;
		if (level == LogLevel.ERROR){
			stderr(text);
		}
		else {
			stdout(text);
		}
	}
	
	public static enum LogLevel {
		DEBUG,
		INFO,
		WARNING,
		ERROR
	}
	
	public static class ConsoleLogger extends CompilerLogger{

		@Override
		public void stdout(String text) {
			System.out.print(text);
		}

		@Override
		public void stderr(String text) {
			System.err.print(text);
		}
		
	}
	
	public static class FileLogger extends CompilerLogger{

		private FSFile file;
		private Writer writer;
		
		public FileLogger(FSFile out){
			file = out;
			writer = new BufferedWriter(new OutputStreamWriter(out.getNativeOutputStream()));
		}
		
		public void close(){
			try {
				writer.close();
			} catch (IOException ex) {
				Logger.getLogger(CompilerLogger.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		
		@Override
		public void stdout(String text) {
			try {
				writer.write(text);
				writer.write(System.lineSeparator());
			} catch (IOException ex) {
				Logger.getLogger(CompilerLogger.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		@Override
		public void stderr(String text) {
			stdout(text);
		}
		
	}
}
