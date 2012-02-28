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
 * Class for analyzing other classes (recursively), for instance Sun Java Class
 * Libraries.
 * @author Owain Jones [odj@aber.ac.uk]
 *
 */
public class ClassInspector {
   private static final ClassMap inspectedClasses = new ClassMap();
   private Class inspectedClass;
   private Class[] referredClasses;
   private Class[] associatedClasses;

   public ClassInspector(String cls) throws ClassNotFoundException {
      this(Class.forName(cls));
   }
   
   public ClassInspector(Class cls) {
      inspectedClass = cls;
   }
   
   public static String[] getReferredClasses(String className) {
      try {
         return new ClassInspector(className).getReferredClassesAsStrings();
      } catch(ClassNotFoundException e) {
         return null;
      }
   }
   
   public String[] getReferredClassesAsStrings() {
      return classNames(this.getReferredClasses());
   }
 
   public static String[] classNames(Class[] source) {
      String[] names = new String[source.length];
      for(int i=0; i<source.length; i++) {
         if(source[i] == null) continue;
         names[i] = source[i].getName();
      }
      return names;
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

   public int getNumberOfMembers() {
      return inspectedClass.getDeclaredFields().length;
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
   
   public int getNumberOfPublicMethods() {
      return this.getMethodsWithModifiers(Modifier.PUBLIC);
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
   
   public int getNumberOfMethods() {
      return inspectedClass.getDeclaredMethods().length;
   }
   
   public int getNumberOfConstructors() {
      return inspectedClass.getDeclaredConstructors().length;
   }
   
   public int getNumberOfMethodArgs(String methodName) {
      try {
         Method m = inspectedClass.getMethod(methodName);
         return m.getParameterTypes().length;
      } catch (NoSuchMethodException | SecurityException e) {
         return 0;
      }
   }
   
   public static ClassMap getAllInspectedClasses() {
      return ClassInspector.inspectedClasses;
   }
}
