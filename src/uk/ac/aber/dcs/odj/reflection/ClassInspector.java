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
 * <p>Class for analyzing other classes (recursively). Extracts a variety of
 * information about a class using reflection.</p>
 * 
 * <p>NOTE: The methods in this class may not work for some classes! It seems
 * that more complex classes which are final implementations of things
 * (seen a lot in the Sun CORBA classes), have static constructors that run
 * even when we're simply reflecting on those classes! If you find that strange
 * exceptions are being thrown, omit the classes causing them for now.</p>
 * 
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
      
      /*
       * Which forName() method is used is important:
       * http://stackoverflow.com/a/9550852/374153
       * Using the one which takes a boolean, so that none of the classes
       * are accidentally initialized (stops them throwing exceptions when
       * reflected upon etc.)
       */
      this(Class.forName(cls,false,null));
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
      
      /*
       * It's likely this will be called multiple times but will always return
       * the same result so keep the result cached in the "referredClasses"
       * field.
       */
      if(this.referredClasses == null) {
         Class source = this.inspectedClass;
         HashSet<Class> associatedClasses = new HashSet<Class>();
         
         // Add the actual class itself. This makes sense when making network
         // graphs showing connectivity.
         associatedClasses.add(source);
         
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
         
         /*
          * Add any other classes (NOTE: may already be done in getInnerClasses
          * - still unsure how getDeclaredX() and getX() methods differ)
          */
         associatedClasses.addAll(Arrays.asList(source.getClasses()));
         
         // Get the superclass (what this class extends).
         associatedClasses.add(source.getSuperclass());
         
         // Get the enclosing class.
         associatedClasses.add(source.getEnclosingClass());
         
         // Get all the classes used as method arguments and return values.
         associatedClasses.addAll(getInnerClasses(source));
         
         /*
          * Remove any classes that are primitive (int, bool, null, void etc.)
          * as well as any Arrays, from the list.
          */
         Iterator<Class> iter;
         for(iter = associatedClasses.iterator(); iter.hasNext();) {
            Class c = iter.next();
            if(c == null || c.isPrimitive() || c.isArray()) {
               iter.remove();
            }
         }
         
         /*
          * Add all the classes we've just picked up to a global, static set
          * of classes - this can be used at the end of inspecting
          * all the classes we want, to generate nodes/edges for a network
          * graph which will show the association/connectivity between classes
          * in the system as a whole.
          */
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
    * Recursively search for classes which are indirectly connected to the
    * specified class.
    * @param source The class to inspect/start the search from
    * @param depth Depth of recursion: -1 for infinite, >= 0 for a depth-limited
    * search
    * @return All the classes used by both the inspected class and its connected
    * classes, up to the depth specified by 'depth' parameter.
    */
   public static Class[] getAssociatedClasses(Class source,int depth) {
      return new ClassInspector(source).getAssociatedClasses(depth);
   }
   
   /*
    * Used internally to keep track of the recursive search.
    */
   private static Class[] getAssociatedClasses(Class source,
            HashSet<Class> checked,int depth,int max) {
      return new ClassInspector(source).getAssociatedClasses(checked,depth,max);
   }
   
   /**
    * Recursively search for classes which are indirectly connected to the
    * inspected class.
    * @param depth Depth of recursion: -1 for infinite, >= 0 for a depth-limited
    * search
    * @return All the classes used by both the inspected class and its connected
    * classes, up to the depth specified by the 'depth' parameter.
    */
   public Class[] getAssociatedClasses(int depth) {
      // Start the recursive search with an empty set and a current depth of 0.
      return this.getAssociatedClasses(new HashSet<Class>(),0,depth);
   }
   
   /*
    * checked: the set of already-inspected classes
    * depth: current depth
    * max: depth at which to stop
    */
   private Class[] getAssociatedClasses(HashSet<Class> checked,
            int depth,int max) {
      
      // Stop the recursion if the current depth is greater than the maximum
      if(depth > max && max != -1) return new Class[0];
      
      HashSet<Class> relatedClasses = new HashSet<Class>();
      
      /*
       * Add all the directly-connected (those which are used in the inspected
       * class itself) to a set
       */
      Class[] associatedClasses = this.getReferredClasses();
      relatedClasses.addAll(Arrays.asList(associatedClasses));
      
      /*
       * Then iterate through that list of classes and recursively inspect each
       * one.
       */
      for(Class c : associatedClasses) {
         
         /*
          * Only check a class if it hasn't been looked at before however.
          */
         if(!checked.contains(c)) {
            checked.add(c);
            Class[] tmp = ClassInspector.getAssociatedClasses(c,checked,
                     depth+1,max);
            relatedClasses.addAll(Arrays.asList(tmp));
            checked.addAll(Arrays.asList(tmp));
         }
      }
      
      /*
       * Add all the classes we've just picked up to a global, static set
       * of classes - this can be used at the end of inspecting
       * all the classes we want, to generate nodes/edges for a network
       * graph which will show the association/connectivity between classes
       * in the system as a whole.
       */
      ClassInspector.inspectedClasses.addAll(
               relatedClasses,this.inspectedClass
      );
      return relatedClasses.toArray(new Class[0]);
   }
   
   /**
    * Get all the classes directly linked to by the inspected class, which have
    * the specified modifiers - use for finding default/protected/abstract etc.
    * classes.
    * @param modifiers Bitfield of the constants in {@link Modifier}
    * @return Number of classes which have these modifiers.
    */
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

   /**
    * Get all the classes weakly associated with the inspected class, which have
    * the specified modifiers - use for finding default/protected/abstract etc.
    * classes.
    * @param modifiers Bitfield of the constants in {@link Modifier}
    * @param depth Recursion limit: -1 for infinite, >= 0 to do a depth-limited
    * search.
    * @return Number of classes which have these modifiers.
    */
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

   /**
    * Get all the members (fields) for the inspected class which have the
    * specified modifiers - can be used to get the number of public/private/
    * static/final methods etc.
    * @param modifiers Bitfield of the constants in {@link Modifier}
    * @return Number of methods in the inspected class which have these
    * modifiers.
    */
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
   
   /**
    * Get all the methods for the inspected class which have the specified
    * modifiers - can be used to get the number of public/private/static/final
    * methods etc.
    * @param modifiers Bitfield of the constants in {@link Modifier}
    * @return Number of methods in the inspected class which have these
    * modifiers.
    */
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

   /**
    * @return Number of methods, fields & interfaces implemented by the
    * inspected class.
    */
   public int getClassSize() {
      return
               inspectedClass.getDeclaredMethods().length +
               inspectedClass.getDeclaredFields().length +
               inspectedClass.getInterfaces().length;
   }

   /**
    * @param cls Class to inspect
    * @return Number of methods, fields & interfaces implemented by the
    * specified class.
    */
   public static int getClassSize(Class cls) {
      return new ClassInspector(cls).getClassSize();
   }
   
   /**
    * @return A Hashtable of all the classes EVER inspected by ALL
    * ClassInspector instances in the current runtime. Can be used to get
    * the relationships/connections between specific classes.
    */
   public static ClassMap getAllInspectedClasses() {
      return ClassInspector.inspectedClasses;
   }
}
