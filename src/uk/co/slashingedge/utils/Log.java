/**
 *
 */
package uk.co.slashingedge.utils;

import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * Logging class, to handle logging (make it prettier, etc)
 * @author Owain Jones [odj@aber.ac.uk]
 */
public final class Log {
   public int width = 80;
   public String delim = " ";
   public static final Log logger = new Log();
   private PrintStream out;
   private PrintStream err;

   public enum Level {
      ERROR, PRINT
   }

   /**
    * Create a default Log object with output & error streams redirected to
    * System.out and System.err respectively
    */
   public Log() {
      this(System.out,System.err);
   }
   
   /**
    * Create a Log object with a custom output stream. Errors are still
    * redirected to System.err
    * @param out The PrintStream to use for output
    */
   public Log(PrintStream out) {
      this(out,System.err);
   }
   
   /**
    * Create a Log object with both output & error directed to custom
    * PrintStreams.
    * @param out The PrintStream to use for output
    * @param err The PrintStream to use for errors
    */
   public Log(PrintStream out, PrintStream err) {
      this.out = out;
      this.err = err;
   }
   
   /**
    * Create a Log object with output directed to a specified file.
    * @param out The name of the file to direct output to.
    * @throws FileNotFoundException
    */
   public Log(String out) throws FileNotFoundException {
      this(new PrintStream(out),System.err);
   }
 
   /**
    * Create a Log object with both output & error directed to specified files.
    * @param out The name of the file to direct output to.
    * @param err The name of the file to direct errors to.
    * @throws FileNotFoundException
    */
   public Log(String out, String err) throws FileNotFoundException {
      this(new PrintStream(out),new PrintStream(err));
   }
   
   /**
    * Wrap a string on word boundaries: if a line is going to be longer than
    * the specified width, append a newline, therefore wrapping
    * the contents to the second line.
    * If width is -1, then wrap() does nothing and simply returns the
    * original string.
    * @param string The un-wrapped, single-line original string
    * @param width The width to wrap the string to.
    * @return The resulting multiple-line, word-wrapped string.
    */
   public static String wrap(String string, int width) {
      if(width == -1) return string;
      if(string == "" | string == null) return "";
      String[] words = string.split(" ");
      if(words.length == 0) return "";
      StringBuilder sb = new StringBuilder();
      StringBuilder line = new StringBuilder();
      for(String word : words) {
         if(line.length()+word.length() >= width) {
            line.append("\n");
            sb.append(line);
            line = new StringBuilder();
         }
         line.append(word+" ");
      }
      sb.append(line);
      return sb.toString();
   }
   
   /**
    * Word-wrap a string and IMMEDIATELY output it... rather than
    * creating a new, wrapped string out of it first.
    * Slightly faster than the wrap() method that returns a string as no
    * StringBuilder objects are instanced.
    * @param level The output to log output too (ERROR or PRINT)
    * @param string The string to print
    * @return the same instance of Log, for method chaining
    */
   public Log wrap(Level level, String string) {
      PrintStream output = this.out;
      if(level == Level.ERROR) output = this.err;
      
      if(width == -1) {
         output.print(string);
         return this;
      }
      
      if(string == "" | string == null) {
         return this;
      }
      
      String[] words = string.split(" ");
      if(words.length == 0) {
         output.print(string);
         return this;
      }
      
      int length = 0;
      for(String word : words) {
         if(length+word.length() >= width) {
            output.print("\n");
            length = 0;
         }
         output.print(word+" ");
         length += word.length()+1;
      }
      
      return this;
   }
   
   /**
    * Print a string to one of the standard outputs
    * @param level Which output: ERROR (prints to Log.out) or PRINT (Log.err)
    * @param string The string to print
    * @return the same instance of Log, for method chaining
    */
   public Log p(Level level, String string) {
      this.wrap(level,string);
      return this;
   }
   
   /**
    * Print a string to output (Log.out).
    * @param string The string to print
    * @return the same instance of Log, for method chaining
    */
   public Log p(Object string) {
      this.p(Level.PRINT,string);
      return this;
   }
   
   /**
    * Print a list of objects to one of the standard outputs, with a delimiter
    * between strings (same as Python's print statement) 
    * @param level Which output: ERROR (prints to Log.err) or PRINT (Log.out)
    * @param strings Strings to print
    * @return the same instance of Log, for method chaining
    */
   public Log p(Level level, Object ... strings) {
      StringBuilder sb = new StringBuilder();
      for(int i=0; i<strings.length; i++) {
         sb.append(strings[i]);
         if(i<strings.length-1) {
            sb.append(this.delim);
         }
      }
      this.p(level,sb.toString());
      return this;
   }
   
   /**
    * Print a list of objects to output (Log.out), with a delimiter between
    * the strings (same as Python's print statement).
    * @param strings Objects to print
    * @return the same instance of Log, for method chaining
    */
   public Log p(Object ... strings) {
      this.p(Level.PRINT,strings);
      return this;
   }
   
   /**
    * Print a list of objects to error stream (Log.err).
    * @param strings Objects to print
    * @return the same instance of Log, for method chaining
    */
   public Log e(Object ... strings) {
      this.p(Level.ERROR,strings);
      this.p(Level.ERROR,"\n");
      return this;
   }
   
   /**
    * Print a list of objects to output to Log.out, with a delimiter between
    * the strings, followed by a newline (\n) character.
    * @param strings Objects to print
    * @return the same instance of Log, for method chaining
    */
   public Log pl(Object ... strings) {
      this.p(Level.PRINT,strings);
      this.p(Level.PRINT,"\n");
      return this;
   }

   /**
    * Print a list of objects to the error stream (Log.err) with a delimiter
    * between the strings.
    * @param strings The objects to print
    * @return Default Log object, for method chaining
    */
   public static Log err(Object ... strings) {
      logger.e(strings);
      return logger;
   }
   
   /**
    * Print a list of objects to the default output (System.out), with a
    * delimiter between them.
    * @param strings The objects to print
    * @return Default Log object, for method chaining
    */
   public static Log print(Object ... strings) {
      logger.p(strings);
      return logger;
   }
   
   /**
    * Print a list of objects, with a delimiter between each one, and append
    * a newline (\n) character, to the default output (System.out).
    * @param strings The objects to print
    * @return Default Log object, for method chaining
    */
   public static Log println(Object ... strings) {
      logger.pl(strings);
      return logger;
   }
   
}
