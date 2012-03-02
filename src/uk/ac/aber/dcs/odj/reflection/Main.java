/**
 *
 */
package uk.ac.aber.dcs.odj.reflection;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Vector;

import uk.co.slashingedge.utils.Log;

/**
 * @author Owain Jones [odj@aber.ac.uk]
 *
 */
public final class Main {
   public static final String OUTPUT = "output.txt";
   public static final String TAB = "    ";
   public static Log log;
   public static Log nodes;
   public static Log edges;
   public static int recursion = -1;
   public static String file = "input.txt";
   
   public static void main(String[] args)
            throws InterruptedException, FileNotFoundException {
      if(args.length == 0) {
         printHelp();
         return;
      }
      if(args.length > 0) file = args[0];
      if(args.length > 1) recursion = Integer.parseInt(args[1]);
      for(String arg : args) {
         if(arg == "/?" || arg.equalsIgnoreCase("-h") ||
                  arg.equalsIgnoreCase("--help")) {
            printHelp();
            return;
         }
      }
      try {
         log = new Log(OUTPUT);
         nodes = new Log("nodes.txt");
         edges = new Log("edges.txt");
         log.e("Logging to",OUTPUT);
      } catch (FileNotFoundException e1) {
         log = Log.logger;
         nodes = Log.logger;
         edges = Log.logger;
         log.e("Logging to console");
      }

      /**
       * Parse input text file for list of classes to inspect
       */
      BufferedReader input = new BufferedReader(new FileReader(file));
      Vector<String> inputArgs = new Vector<String>();
      try {
         while(input.ready()) {
            String line = input.readLine();
            // Ignore empty lines & lines which are comments
            if(!line.startsWith("//") && line != "") {
               inputArgs.add(line);
            }
         }
      } catch (IOException e1) {
         log.e("Error reading input file");
      }
      args = inputArgs.toArray(new String[0]);
      
      // Disable word-wrapping on all the output loggers.
      log.width = -1;
      nodes.width = -1;
      edges.width = -1;
      
      String[][] references = new String[args.length][];

      // "Warm up" the table 
      for(String arg : args) {
         try {
            ClassInspector inspect = new ClassInspector(arg);
            inspect.getAssociatedClasses(recursion);
         } catch (ClassNotFoundException e1) {
         }
      }
      
      // Print information about each class in CSV form so I can paste the lines
      // into a spreadsheet easily.
      for(String arg : args) {
         try {
            log.pl("//START CLASS INFO: ",arg);
            log.e(arg);
            detailedClassInfo(arg);
            log.pl("//END CLASS INFO:",arg,"\n");
         } catch(Exception e) {
            Thread.sleep(10);
            log.e(e.toString());
            Thread.sleep(10);
         }
      }
      
      // Print nodes list to nodes.txt - to be used by Gephi
      printHeader(nodes);
      nodes.delim = ",";
      ClassMap map = (ClassMap) ClassInspector.getAllInspectedClasses().clone();
      for(Class c : map.keySet()) {
         classInfo(nodes,c);
         nodes.pl();
      }
      
      // Print edges list to edges.txt - to be used by Gephi
      edges.pl("Source,Target,Weight");
      edges.delim = ",";
      for(Entry<Class,HashSet<Class>>e :
         ClassInspector.getAllInspectedClasses().entrySet()) {
         for(Class c : e.getValue()) {
           if(e.getKey() != c && e.getKey().getName() != null &&
                     c.getName() != null) {
              int weight = 1;
              if(inputArgs.contains(c.getName())) weight = 3;
              edges.pl(c.getName(),e.getKey().getName(),weight);
            }
         }
      }
      
      // Reset the delimiters for all the output loggers in case I want to more
      // logging later which isn't in comma-seperated-values form.
      nodes.delim = " " ;
      edges.delim = " ";
      log.delim = " ";

   }
   
   public static void classInfo(Log l, String cls)
            throws ClassNotFoundException {
      classInfo(log,Class.forName(cls));
   }
   
   public static void classInfo(Log l, Class cls) {
      if(l != null) l.delim = ",";
      
      /*
       * Get the number of connections to this class, from the global table.
       * NOTE: This won't be accurate until you've run through your classes
       * once!
       */
      ClassMap map = ClassInspector.getAllInspectedClasses();
      int connections = 0;
      if(map.containsKey(cls) && map.get(cls) != null) {
         connections = map.get(cls).size();
      }
      
      ClassInspector inspect = new ClassInspector(cls);
      l.p(cls.getName(),
               cls.getSimpleName(),
               cls.getName(),
               inspect.getNumberOfMethods(),
               inspect.getNumberOfConstructors(),
               inspect.getNumberOfPublicMethods(),
               inspect.getMethodsWithModifiers(Modifier.PRIVATE),
               inspect.getMethodsWithModifiers(Modifier.STATIC),
               inspect.getMethodsWithModifiers(Modifier.ABSTRACT),
               inspect.getMethodsWithModifiers(Modifier.NATIVE),
               inspect.getMembersWithModifiers(Modifier.SYNCHRONIZED),
               inspect.getMembersWithModifiers(Modifier.FINAL),
               inspect.getNumberOfMembers(),
               inspect.getMembersWithModifiers(Modifier.PUBLIC),
               inspect.getMembersWithModifiers(Modifier.PRIVATE),
               inspect.getMembersWithModifiers(Modifier.PROTECTED),
               inspect.getMembersWithModifiers(Modifier.SYNCHRONIZED),
               inspect.getMembersWithModifiers(Modifier.FINAL),
               inspect.getReferredClassesWithModifiers(Modifier.INTERFACE),
               inspect.getReferredClassesWithModifiers(Modifier.ABSTRACT),
               inspect.getReferredClasses().length,
               inspect.getAssociatedClasses(recursion).length,
               inspect.getClassSize(),
               connections
      );
      
      l.delim = " ";
   }
   
   public static void detailedClassInfo(String className)
            throws ClassNotFoundException {
      detailedClassInfo(Class.forName(className));
   }
   
   public static void detailedClassInfo(Class cls) {
      printHeader(log);
      classInfo(log,cls);
      log.pl("\n-----------------------");
      log.delim = ",";
      log.pl("method,number arguments");
      for(Method m : cls.getDeclaredMethods()) {
         log.pl(m.getName(),m.getParameterTypes().length);
      }
      log.delim = " ";
      log.pl();
   }
   
   public static void printHeader(Log l) {
      l.delim = ",";
      l.p("Id","Label","class","methods","constructors","public methods",
               "private methods","static methods","abstract methods",
               "native methods","synchronized methods","final methods",
               "members","public members","private members","protected members",
               "synchronized members","final members", "interface classes",
               "abstract classes", "directly associated classes",
               "indirectly associated classes", "class size", "connections");
      
      l.delim = " ";
      l.pl();
   }
   
   public static void printHelp() {
      System.out.println("No arguments given to program. Usage:\n");
      System.out.println("\tjava Main <input file> [recursion depth=-1]\n");
      System.out.println("Where recursion depth is the maximum depth any" +
      		" recursive searches should\n" +
      		"terminate at. -1 for infinite recursion.");
      System.out.println("Input file should be a list of java classes " +
      		"(including their package names),");
      System.out.println("one class per line.");
   }

}
