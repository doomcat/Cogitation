/**
 *
 */
package uk.ac.aber.dcs.odj.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
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
    * Method argument types, return types and field types used by the class.
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

   /**
    * Get all the names of the classes associated with the class you're
    * currently inspecting, as an array of strings. Same as the static method
    * {@code getReferredClasses(String className)}.
    * @return Array of class names (Strings) associated with this class
    */
   public String[] getReferredClassesAsStrings() {
      return classNames(this.getReferredClasses());
   }
 
   /**
    * Get all the names of the classes associated with a specified class.
    * @param className The class to inspect
    * @return Array of class names (Strings) associated with this class
    */
   public static String[] getReferredClasses(String className) {
      try {
         return new ClassInspector(className).getReferredClassesAsStrings();
      } catch(ClassNotFoundException e) {
         return null;
      }
   }

   /**
    * Get all the classes that a specified class links to in its code.
    * @param source The class to inspect
    * @return Array of {@link Class} objects associated with this class
    */
   public static Class[] getReferredClasses(Class source) {
      return new ClassInspector(source).getReferredClasses();
   }
   
   /**
    * Get all the classes the currently inspected class refers to in its code.
    * Same as the static method {@code getReferredClasses(Class source)}.
    * @return Array of {@link Class} objects associated with this class
    */
   public Class[] getReferredClasses() {
      
      // It's likely this will be called multiple times but will always return
      // the same result so keep the result cached in the "referredClasses"
      // field.
      if(this.referredClasses == null) {
         Class source = this.inspectedClass;
         HashSet<Class> associatedClasses = new HashSet<Class>();
         
         // If this class is internal to another class (Delegate class),
         // get its parent.
         if(source.getDeclaringClass() != null) {
            associatedClasses.add(source.getDeclaringClass());
         }
         
         // Get all the classes this class has declared as members. 
         for(Class c : source.getDeclaredClasses()) {
            associatedClasses.add(c);
         }
         
         // Get any interfaces this class implements.
         for(Class c : source.getInterfaces()) {
            associatedClasses.add(c);
         }
         
         // Add any other classes (NOTE: may already be done in getInnerClasses
         // - still unsure how getDeclaredX() and getX() methods differ
         associatedClasses.addAll(Arrays.asList(source.getClasses()));
         
         // Get the superclass (what this class extends).
         associatedClasses.add(source.getSuperclass());
         
         // Get the enclosing class.
         associatedClasses.add(source.getEnclosingClass());
         
         // Get all the classes used as method arguments and return values.
         associatedClasses.addAll(getInnerClasses(source));
         
         // Remove any classes that are primitive (int, bool, null, void etc.)
         // as well as any Arrays, from the list.
         Iterator<Class> iter;
         for(iter = associatedClasses.iterator(); iter.hasNext();) {
            Class c = iter.next();
            if(c == null || c.isPrimitive() || c.isArray()) {
               iter.remove();
            }
         }
         
         // Add all the classes we've just picked up to a global, static set
         // of classes - this can be used at the end of inspecting
         // all the classes we want, to generate nodes/edges for a network
         // graph which will show the association/connectivity between classes
         // in the system as a whole.
         ClassInspector.inspectedClasses.addAll(associatedClasses,source);
         
         this.associatedClasses = associatedClasses.toArray(new Class[0]);
      }

      return this.associatedClasses;
   }
   
   /**
    * Recursively get a list of class names 'associated' with the specified
    * class: this gets the classes reffered to by the classes reffered to by
    * the classes refferred to [...] referred to by the specified class.
    * @param className Name of the class to inspect
    * @param depth Depth of search: -1 for infinite, >= 0 for a depth-limited
    * recursive search.
    * @return Array of strings representing all class names found
    */
   public static String[] getAssociatedClassesAsStrings(String className,
            int depth) {
      try {
         return new ClassInspector(className)
         .getAssociatedClassesAsStrings(depth);
      } catch (ClassNotFoundException e) {
         return null;
      }
   }
   
   /**
    * Recursively get a list of class names associated with the inspected
    * class: this gets the classes referred to by the classes referred to [...]
    * referred to by the inspected class.
    * @param depth Depth of search: -1 for infinite >= 0 for depth-limited
    * recursive search.
    * @return Array of strings representing all class names found
    */
   public String[] getAssociatedClassesAsStrings(int depth) {
      return classNames(this.getAssociatedClasses(depth));
   }
   
   /**
    * 
    * @param source
    * @param depth
    * @return
    */
   public static Class[] getAssociatedClasses(Class source,int depth) {
      return new ClassInspector(source).getAssociatedClasses(depth);
   }
   
   public static Class[] getAssociatedClasses(Class source,
            HashSet<Class> checked,int depth,int max) {
      return new ClassInspector(source).getAssociatedClasses(checked,depth,max);
   }
   
   public Class[] getAssociatedClasses(int depth) {
      return this.getAssociatedClasses(new HashSet<Class>(),0,depth);
   }
   
   public Class[] getAssociatedClasses(HashSet<Class> checked,
            int depth,int max) {
      if(depth > max && max != -1) return new Class[0];
      HashSet<Class> relatedClasses = new HashSet<Class>();
      Class[] associatedClasses = this.getReferredClasses();
      relatedClasses.addAll(Arrays.asList(associatedClasses));
      
      for(Class c : associatedClasses) {
         if(!checked.contains(c)) {
            checked.add(c);
            Class[] tmp = ClassInspector.getAssociatedClasses(c,checked,
                     depth+1,max);
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

   public int getAssociatedClassesWithModifiers(int modifiers, int depth) {
      Class[] classes = this.getAssociatedClasses(depth);
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
