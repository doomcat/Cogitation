/**
 *
 */
package uk.ac.aber.dcs.odj.reflection;

import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import org.omg.CORBA.IntHolder;

/**
 * @author Owain Jones [odj@aber.ac.uk]
 *
 */
public class ClassMap extends Hashtable<Class, HashSet<Class>> {
   public void add(Class cls, Class parent) {
      if(this.containsKey(cls)) {
         this.get(cls).add(parent);
      } else {
         HashSet<Class> list = new HashSet<Class>();
         list.add(parent);
         this.put(cls, list);
      }
   }
   
   public int getValue(Class c) {
      return this.get(c).size();
   }
   
   public Class[] getClasses(Class c) {
      return this.get(c).toArray(new Class[0]);
   }
   
   public void addAll(Collection<Class> collection, Class parent) {
      /*Iterator<Class> iter = collection.iterator();
      for(Class c = iter.next(); iter.hasNext();) {
         this.add(c);
      }*/
      Class[] coll = collection.toArray(new Class[0]);
      for(Class c : coll) this.add(c,parent);
   }
}
