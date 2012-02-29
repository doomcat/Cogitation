/**
 *
 */
package uk.ac.aber.dcs.odj.reflection;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map.Entry;

import uk.co.slashingedge.utils.Log;

/**
 * @author Owain Jones [odj@aber.ac.uk]
 *
 */
public final class Main {
   public static final String OUTPUT = "output.txt";
   public static final String TAB = "    ";
   public static Log log;
   
   public static void main(String[] args) throws InterruptedException {
      try {
         log = new Log(OUTPUT);
         log.e("Logging to",OUTPUT);
      } catch (FileNotFoundException e1) {
         log = Log.logger;
         log.e("Logging to console");
      }
      
      log.width = -1;
      String[][] references = new String[args.length][];

      for(int i=0; i<args.length; i++) {
         String arg = args[i];
         try {
            log.pl("//START CLASS INFO: ",arg);
            detailedClassInfo(arg);
            log.pl("//END CLASS INFO:",arg,"\n");
         } catch(ClassNotFoundException e) {
            Thread.sleep(50);
            log.e("Class Not Found:",arg,"\n");
            Thread.sleep(50);
         }
      }
      log.pl("//INDIRECTLY ASSOCIATED CLASSES - Nodes");
      //printHeader();
      log.delim = ",";
      log.pl("Id,Connections");
      for(Entry<Class,HashSet<Class>> e :
         ClassInspector.getAllInspectedClasses().entrySet()) {
         log.pl(e.getKey().getName(),e.getValue().size());
      }
      
      log.pl("\n//INDIRECTLY ASSOCIATED CLASSES - Edges");
      log.pl("Source,Target");
      for(Entry<Class,HashSet<Class>>e :
         ClassInspector.getAllInspectedClasses().entrySet()) {
         for(Class c : e.getValue()) {
            log.pl(e.getKey().getName(),c.getName());
         }
      }
      
      log.delim = " ";

   }
   
   public static void classInfo(String cls) throws ClassNotFoundException {
      classInfo(Class.forName(cls));
   }
   
   public static void classInfo(Class cls) {
      String old = String.valueOf(log.delim);
      log.delim = ",";
      
      ClassInspector inspect = new ClassInspector(cls);
      log.p(cls.getName(),
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
               inspect.getAssociatedClasses().length
      );
      
      log.delim = " ";
   }
   
   public static void detailedClassInfo(String className)
            throws ClassNotFoundException {
      detailedClassInfo(Class.forName(className));
   }
   
   public static void detailedClassInfo(Class cls) {
      printHeader();
      classInfo(cls);
      log.pl("\n-----------------------");
      log.delim = ",";
      log.pl("method,number arguments");
      for(Method m : cls.getDeclaredMethods()) {
         log.pl(m.getName(),m.getParameterTypes().length);
      }
      log.delim = " ";
      log.pl();
   }
   
   public static void printHeader() {
      log.delim = ",";
      log.p("class","methods","constructors","public methods","private methods",
               "static methods","abstract methods","native methods",
               "synchronized methods","final methods","members",
               "public members","private members","protected members",
               "synchronized members","final members", "interface classes",
               "abstract classes", "directly associated classes",
               "indirectly associated classes", "# references",
               "referred classes [list]");
      
      log.delim = " ";
      log.pl();
   }

}