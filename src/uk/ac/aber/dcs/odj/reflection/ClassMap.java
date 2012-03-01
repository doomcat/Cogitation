/**
 *
 */
package uk.ac.aber.dcs.odj.reflection;

import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;

/**
 * <p>ClassMap extends {@link Hashtable}, making it simpler to use from other
 * code. A ClassMap is a hash-table with {@link Class} objects as keys and
 * {@link HashSet}s of Classes as values.</p>
 * 
 * <p>This is used to keep track of the relationships between classes - the
 * HashSet for a given key will be the list of classes that refer to the Class
 * which is the key.</p>
 * 
 * @author Owain Jones [odj@aber.ac.uk]
 */
public class ClassMap extends Hashtable<Class, HashSet<Class>> {
   private static final long serialVersionUID = 6019232687340137611L;

   /**
    * @param cls Class to use as the key
    * @param parent Class which has references to the key-class
    */
   public void add(Class cls, Class parent) {
      if(this.containsKey(cls)) {
         this.get(cls).add(parent);
      } else {
         HashSet<Class> list = new HashSet<Class>();
         list.add(parent);
         this.put(cls, list);
      }
   }
   
   /**
    * Get the number of connections for a class
    * @param c The Class to look for in the Hashtable's keys.
    * @return Size of the HashSet for the given key (Class).
    */
   public int getValue(Class c) {
      return this.get(c).size();
   }
   
   public Class[] getClasses(Class c) {
      return this.get(c).toArray(new Class[0]);
   }
   
   public void addAll(Collection<Class> collection, Class parent) {
      Class[] coll = collection.toArray(new Class[0]);
      for(Class c : coll) this.add(c,parent);
   }
}
