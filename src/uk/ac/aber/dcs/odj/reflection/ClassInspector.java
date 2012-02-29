/**
 *
 */
package uk.ac.aber.dcs.odj.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Class for analyzing other classes (recursively).
 * @author Owain Jones [odj@aber.ac.uk]
 *
 */
public class ClassInspector {
   private static final ClassMap inspectedClasses = new ClassMap();
   private Class inspectedClass;
   private Class[] referredClasses;
   private Class[] associatedClasses;

   /**
    * Find the class with a specified name. Throws an exception if no such
    * class can be found in the running VM's classpath.
    * @param cls Name of the class this ClassInspector will be inspecting
    * @throws ClassNotFoundException
    */
   public ClassInspector(String cls) throws ClassNotFoundException {
      this(Class.forName(cls));
   }
   
   /**
    * @param cls The Class this ClassInspector will be inspecting
    */
   public ClassInspector(Class cls) {
      inspectedClass = cls;
   }
   
   /**
    * Get the amount of inspected classes' members (fields): this counts ALL
    * fields regardless of their visibility (default/private/public etc.) or
    * annotations (i.e. abstract). 
    * @return Number of declared fields owned by the inspected class
    */
   public int getNumberOfMembers() {
      return inspectedClass.getDeclaredFields().length;
   }

   /**
    * Get the number of inspected classes' methods: this counts ALL methods
    * regardless of their visibility, so includes default & private methods
    * as well as public, abstract etc.
    * @return The number of methods declared in the inspected class
    */
   public int getNumberOfMethods() {
      return inspectedClass.getDeclaredMethods().length;
   }

   /**
    * @return The number of public methods in the inspected class
    */
   public int getNumberOfPublicMethods() {
      return this.getMethodsWithModifiers(Modifier.PUBLIC);
   }

   /**
    * @return The number of constructors in the inspected class
    */
   public int getNumberOfConstructors() {
      return inspectedClass.getDeclaredConstructors().length;
   }

   /**
    * @param methodName Name of the method to analyze
    * @return Number of arguments this method requires to be passed to it
    */
   public int getNumberOfMethodArgs(String methodName) {
      try {
         Method m = inspectedClass.getMethod(methodName);
         return m.getParameterTypes().length;
      } catch (NoSuchMethodException | SecurityException e) {
         return 0;
      }
   }

   /**
    * @param clsName Name of the class to look for the method in
    * @param methodName Name of the method to analyze
    * @return Number of arguments this method in the specified class requires
    * to be passed to it. Returns 0 if the method or class was not found.
    */
   public static int getNumberOfMethodArgs(String clsName, String methodName) {
      try {
         return new ClassInspector(clsName).getNumberOfMethodArgs(methodName);
      } catch(ClassNotFoundException e) {
         return 0;
      }
   }
   
   /**
    * @param source An array of classes
    * @return An array of strings containing the names of all the classes.
    */
   public static String[] classNames(Class[] source) {
      String[] names = new String[source.length];
      for(int i=0; i<source.length; i++) {
         if(source[i] == null) continue;
         names[i] = source[i].getName();
      }
      return names;
   }

   /**
    * Get all the "inner" Classes a specified class refers to: gets all the  
    * @param className
    * @return
    */
   public static HashSet<Class> getInnerClasses(Class className) {
      HashSet<Class> innerClasses = new HashSet<Class>();
      
      for(Method m : className.getDeclaredMethods()) {
         innerClasses.add(m.getReturnType());
         for(Class c : m.getExceptionTypes()) {
            innerClasses.add(c);
         }
         for(Class c : m.getParameterTypes()) {
            innerClasses.add(c);
         }
      }
      
      for(Field f : className.getDeclaredFields()) {
         innerClasses.add(f.getType());
      }
      
      return innerClasses;
   }

   public String[] getReferredClassesAsStrings() {
      return classNames(this.getReferredClasses());
   }
 
   public static String[] getReferredClasses(String className) {
      try {
         return new ClassInspector(className).getReferredClassesAsStrings();
      } catch(ClassNotFoundException e) {
         return null;
      }
   }

   public static Class[] getReferredClasses(Class source) {
      return new ClassInspector(source).getReferredClasses();
   }
   
   public Class[] getReferredClasses() {
      if(this.referredClasses == null) {
         Class source = this.inspectedClass;
         HashSet<Class> associatedClasses = new HashSet<Class>();
         if(source.getDeclaringClass() != null) {
            associatedClasses.add(source.getDeclaringClass());
         }
         for(Class c : source.getDeclaredClasses()) {
            associatedClasses.add(c);
         }
         for(Class c : source.getInterfaces()) {
            associatedClasses.add(c);
         }
         associatedClasses.addAll(getInnerClasses(source));
         
         Iterator<Class> iter;
         for(iter = associatedClasses.iterator(); iter.hasNext();) {
            Class c = iter.next();
            if(c.isPrimitive() || c.isArray()) {
               iter.remove();
            }
         }
         
         ClassInspector.inspectedClasses.addAll(associatedClasses,source);
         this.associatedClasses = associatedClasses.toArray(new Class[0]);
      }

      return this.associatedClasses;
   }
   
   public static String[] getAssociatedClassesAsStrings(String className) {
      try {
         return new ClassInspector(className).getAssociatedClassesAsStrings();
      } catch (ClassNotFoundException e) {
         return null;
      }
   }
   
   public String[] getAssociatedClassesAsStrings() {
      return classNames(this.getAssociatedClasses());
   }
   
   public static Class[] getAssociatedClasses(Class source) {
      return new ClassInspector(source).getAssociatedClasses();
   }
   
   public static Class[] getAssociatedClasses(Class source,
            HashSet<Class> checked) {
      return new ClassInspector(source).getAssociatedClasses(checked);
   }
   
   public Class[] getAssociatedClasses() {
      return this.getAssociatedClasses(new HashSet<Class>());
   }
   
   public Class[] getAssociatedClasses(HashSet<Class> checked) {
      HashSet<Class> relatedClasses = new HashSet<Class>();
      Class[] associatedClasses = this.getReferredClasses();
      relatedClasses.addAll(Arrays.asList(associatedClasses));
      
      for(Class c : associatedClasses) {
         if(!checked.contains(c)) {
            checked.add(c);
            Class[] tmp = ClassInspector.getAssociatedClasses(c,checked);
            relatedClasses.addAll(Arrays.asList(tmp));
            checked.addAll(Arrays.asList(tmp));
         }
      }
      
      ClassInspector.inspectedClasses.addAll(
               relatedClasses,this.inspectedClass
      );
      return relatedClasses.toArray(new Class[0]);
   }
   
   public int getReferredClassesWithModifiers(int modifiers) {
      Class[] classes = this.getReferredClasses();
      int count = 0;
      for(Class c : classes) {
         if((c.getModifiers() & modifiers) == modifiers) {
            count++;
         }
      }
      return count;
   }

   public int getAssociatedClassesWithModifiers(int modifiers) {
      Class[] classes = this.getAssociatedClasses();
      int count = 0;
      for(Class c : classes) {
         if((c.getModifiers() & modifiers) == modifiers) {
            count++;
         }
      }
      return count;
   }

   public int getMembersWithModifiers(int modifiers) {
      int count = 0;
      Field[] fields = inspectedClass.getDeclaredFields();
      for(Field f : fields) {
         f.setAccessible(true);
         if((f.getModifiers() & modifiers) == modifiers) {
            count++;
         }
      }
      return count;
   }
   
   public int getMethodsWithModifiers(int modifiers) {
      int count = 0;
      Method[] methods = inspectedClass.getDeclaredMethods();
      for(Method m : methods) {
         m.setAccessible(true);
         if((m.getModifiers() & modifiers) == modifiers) {
            count++;
         }
      }
      return count;
   }
   
   public static ClassMap getAllInspectedClasses() {
      return ClassInspector.inspectedClasses;
   }
}
