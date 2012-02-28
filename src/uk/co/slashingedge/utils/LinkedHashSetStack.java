/**
 *
 */
package uk.co.slashingedge.utils;

import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * A 
 * @author Owain Jones [odj@aber.ac.uk]
 */
public class LinkedHashSetStack<T> extends LinkedHashSet<T> {
   public T pop() {
      Iterator<T> iter = this.iterator();
      if(iter.hasNext()) {
         T obj = iter.next();
         this.remove(obj);
         return obj;
      }
      return null;
   }
}
