/**
 *
 */
package uk.ac.aber.dcs.odj.reflection;
import java.io.FileNotFoundException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map.Entry;

import org.omg.CORBA.IntHolder;

import uk.co.slashingedge.utils.Log;

/**
 * @author Owain Jones [odj@aber.ac.uk]
 *
 */
public final class Main {
   public static final String TAB = "    ";
   public static Log log;
   
   public static void main(String[] args) throws InterruptedException {
      try {
         log = new Log("output.csv");
      } catch (FileNotFoundException e1) {
         log = Log.logger;
      }
      log.width = -1;
      String[][] references = new String[args.length][];
      
      String old = String.valueOf(log.delim);
      log.delim = ",";
      
      log.p("class","methods","constructors","public methods","private methods",
               "static methods","abstract methods","native methods",
               "synchronized methods","final methods","members",
               "public members","private members","protected members",
               "synchronized members","final members",
               "directly referred classes", "implicitly referred classes",
               "# references","referred classes [list]");
      
      log.delim = " ";
      log.pl();
      
      for(int i=0; i<args.length; i++) {
         String arg = args[i];
         try {
            classInfo(arg);
            log.pl();
         } catch(ClassNotFoundException e) {
            Thread.sleep(50);
            log.e("Class Not Found:",arg,"\n");
            Thread.sleep(50);
         }
      }
      log.pl();
      for(Entry<Class,HashSet<Class>> e : ClassInspector.getAllInspectedClasses().entrySet()) {
         //log.pl(e.getKey().getName(),"-",e.getValue().value);
         classInfo(e.getKey());
         log.p(","+e.getValue().size());
         //log.p(","+Arrays.toString(e.getValue().toArray(new Class[0])));
         log.pl();
      }

   }
   
   public static void classInfo(String cls) throws ClassNotFoundException {
      classInfo(Class.forName(cls));
   }
   
   public static void classInfo(Class cls) {
      String old = String.valueOf(log.delim);
      log.delim = ",";
      
      ClassInspector inspect = new ClassInspector(cls);
      log.p(cls,
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
               inspect.getReferredClasses().length,
               inspect.getAssociatedClasses().length
      );
      
      log.delim = " ";
   }

}
