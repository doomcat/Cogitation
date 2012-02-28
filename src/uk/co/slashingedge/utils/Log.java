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

   public Log() {
      this(System.out,System.err);
   }
   
   public Log(PrintStream out) {
      this(out,System.err);
   }
   
   public Log(PrintStream out, PrintStream err) {
      this.out = out;
      this.err = err;
   }
   
   public Log(String out) throws FileNotFoundException {
      this(new PrintStream(out),System.err);
   }
   
   public Log(String out, String err) throws FileNotFoundException {
      this(new PrintStream(out),new PrintStream(err));
   }
   
   /**
    * Wrap a string on word boundaries: if a line is going to be longer than
    * Log.WIDTH (default: 80 characters), append a newline, therefore wrapping
    * the contents to the second line.
    * If Log.WIDTH is -1, then wrap() does nothing and simply returns the
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
    * Word-wrap a string and IMMEDIATELY print it to console... rather than
    * creating a string out of it first. Slightly faster than the wrap() method
    * that returns a string as no StringBuilder objects are instanced.
    * @param level The output to log output too (ERROR or PRINT)
    * @param string The string to print
    */
   public void wrap(Level level, String string) {
      PrintStream output = this.out;
      if(level == Level.ERROR) output = this.err;
      
      if(width == -1) {
         output.print(string);
         return;
      }
      
      if(string == "" | string == null) {
         return;
      }
      
      String[] words = string.split(" ");
      if(words.length == 0) {
         output.print(string);
         return;
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
   }
   
   /**
    * Print a string to one of the standard outputs
    * @param level Which output: ERROR (prints to stderr) or PRINT (stdout)
    * @param string The string to print
    * @throws BrainOverflowException 
    */
   public void p(Level level, String string) {
      this.wrap(level,string);
   }
   
   /**
    * Print a string to standard output (stdout).
    * @param string The string to print
    */
   public void p(Object string) {
      this.p(Level.PRINT,string);
   }
   
   /**
    * Print a list of strings to one of the standard outputs, with a space
    * between strings (same as Python's print statement) 
    * @param level Which output: ERROR (prints to stderr) or PRIT (stdout)
    * @param strings Strings to print
    */
   public void p(Level level, Object ... strings) {
      StringBuilder sb = new StringBuilder();
      for(int i=0; i<strings.length; i++) {
         sb.append(strings[i]);
         if(i<strings.length-1) {
            sb.append(this.delim);
         }
      }
      this.p(level,sb.toString());
   }
   
   /**
    * Print a list of strings to standard output (stdout), with a space between
    * the strings (same as Python's print statement).
    * @param strings Strings to print
    */
   public void p(Object ... strings) {
      this.p(Level.PRINT,strings);
   }
   
   /**
    * Print a list of strings to standard error (stderr).
    * @param strings Strings to print
    */
   public void e(Object ... strings) {
      this.p(Level.ERROR,strings);
      this.p(Level.ERROR,"\n");
   }
   
   public void pl(Object ... strings) {
      this.p(Level.PRINT,strings);
      this.p(Level.PRINT,"\n");
   }

   public static void err(Object ... strings) {
      logger.e(strings);
   }
   
   public static void print(Object ... strings) {
      logger.p(strings);
   }
   
   public static void println(Object ... strings) {
      logger.pl(strings);
   }
   
}
