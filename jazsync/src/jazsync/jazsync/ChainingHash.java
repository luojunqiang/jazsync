/* ChainingHash.java

   ChainingHash: Chaining hash table for storing checksums from metafile
   Copyright (C) 2011 Tomas Hlavnicka <hlavntom@fel.cvut.cz>

   This file is a part of Jazsync.

   Jazsync is free software; you can redistribute it and/or modify it
   under the terms of the GNU General Public License as published by the
   Free Software Foundation; either version 2 of the License, or (at
   your option) any later version.

   Jazsync is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Jazsync; if not, write to the

      Free Software Foundation, Inc.,
      59 Temple Place, Suite 330,
      Boston, MA  02111-1307
      USA
 */

package jazsync.jazsync;

import java.util.Arrays;
import org.jarsync.ChecksumPair;

/**
 * Chaining hash table used to store block checksums loaded from metafile
 * @author Tomáš Hlavnička
 */
public class ChainingHash {
    private SortedList[] hashArray;
    private int arraySize;

    /**
     * Initializing chaining hash table of <code>size</code>
     * @param size Size of the hash table
     */
    public ChainingHash(int size) {
        arraySize = size;
        hashArray = new SortedList[arraySize];
        for (int i = 0; i < arraySize; i++) {
            hashArray[i] = new SortedList();
        }
    }

    /**
     * Method used to display the hash table
     */
    public void displayTable() {
        for (int j = 0; j < arraySize; j++) {
            System.out.print(j+". ");
            hashArray[j].displayList();
        }
    }

    /**
     * Hashing function
     * @param pKey Key object that will be hashed into the table
     * @return Index in hash table where the object will be put
     */
    public int hashFunction(ChecksumPair pKey) {
        return pKey.hashCode() % arraySize;
    }

    /**
     * Method inserting link into the table
     * @param link Link containing the object we want to insert into the table
     */
    public void insert(Link link) {
        ChecksumPair pKey = link.getKey();
        int hashValue = hashFunction(pKey);
        hashArray[hashValue].insert(link);
    }

    /**
     * Method used to delete an object from hash table
     * @param pKey
     */
    public void delete(ChecksumPair pKey) {
        int hashValue = hashFunction(pKey);
        hashArray[hashValue].delete(pKey);
    }

    /**
     * Method used to find an object in hash table using only weakSum
     * @param pKey Object that we are finding
     * @return Link where the object is
     */
    public Link find(ChecksumPair pKey) {
        int hashValue = hashFunction(pKey);
        Link theLink = hashArray[hashValue].find(pKey);
        return theLink;
    }

    /**
     * Method used to find an object in hash table using weakSum and strongSum
     * @param pKey Object that we are finding
     * @return Link where the object is
     */
    public Link findMatch(ChecksumPair pKey) {
        int hashValue = hashFunction(pKey);
        Link theLink = hashArray[hashValue].findMatch(pKey);
        return theLink;
    }
}

class Link {

    private ChecksumPair blockSums;
    public Link next;

    /**
     * Link constructor
     * @param p ChecksumPair object
     */
    public Link(ChecksumPair p) {
        blockSums = p;
    }

    /**
     * Weak rolling checksum getter
     * @return Weak checksum
     */
    public int getWeakKey() {
        return blockSums.getWeak();
    }

    /**
     * Strong checksum getter
     * @return Strong checksum
     */
    public byte[] getStrongKey(){
        return blockSums.getStrong();
    }

    /**
     * Key object getter
     * @return The key object
     */
    public ChecksumPair getKey() {
        return blockSums;
    }

    /**
     * Displaying contain of the link
     */
    public void displayLink() {
        System.out.print(blockSums.toString() + " ");
    }
}

class SortedList {

    private Link first;

    /**
     * SortedList constructor
     */
    public SortedList() {
        first = null;
    }

    /**
     * Method used to insert link with ChecksumPair object into the list
     * @param link Link with ChecksumPair object
     */
    public void insert(Link link) {
        Link previous = null;
        Link current = first;
        while (current != null) {
            previous = current;
            current = current.next;
        }
        if (previous == null) {
            first = link;
        } else {
            previous.next = link;
        }
        link.next = current;
    }

    /**
     * Method used to delete a ChecksumPair object from sorted list
     * @param key ChecksumPair object that we want to delete from list
     */
    public void delete(ChecksumPair key) {
        Link previous = null;
        Link current = first;
        while (current != null && !key.equals(current.getKey())) {
            previous = current;
            current = current.next;
            
        }
        if (previous == null) {
            first = first.next;
        } else {
            previous.next = current.next;
        }
    }

    /**
     * Method used to find a ChecksumPair object in list using only weakSum
     * @param pKey Object that we are finding
     * @return Link Returns the link with object
     */
    public Link find(ChecksumPair pKey) {
        Link current = first;
        while (current != null) {
            if (current.getWeakKey() == pKey.getWeak()) {
                return current;
            }
            current = current.next;
        }
        return null;
    }

    /**
     * Method used to find a ChecksumPair object in list using weakSum and strongSum
     * @param pKey Object that we are finding
     * @return Link Returns the link with object
     */
    public Link findMatch(ChecksumPair pKey) {
        Link current = first;
        while (current != null) {
            if (current.getWeakKey() == pKey.getWeak() &&
                    Arrays.equals(current.getStrongKey(),pKey.getStrong())) {
                return current;
            }
            current = current.next;
        }
        return null;
    }

    /**
     * Method used to display list
     */
    public void displayList() {
        System.out.print("List: ");
        Link current = first;
        while (current != null) {
            current.displayLink();
            current = current.next;
        }
        System.out.println();
    }
}
