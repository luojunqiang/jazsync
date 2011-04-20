package jazsync;

import java.io.IOException;
import org.metastatic.rsync.ChecksumPair;

/**
 *
 *
 */
class Link {

    private ChecksumPair blockSums;
    public Link next;

    public Link(ChecksumPair p) {
        blockSums = p;
    }

    public ChecksumPair getKey() {
        return blockSums;
    }

    public void displayLink() {
        System.out.print(blockSums.getSequence() + " ");
    }
}

/**
 *
 *
 */
class SortedList {

    private Link first;

    public SortedList() {
        first = null;
    }

    public void insert(Link link) {
        ChecksumPair pKey = link.getKey();
        Link previous = null; // start at first
        Link current = first;
        // until end of list,
        //or current bigger than pKey,
        while (current != null && pKey.getOffset() > current.getKey().getOffset()) {
            previous = current;
            current = current.next; // go to next item
        }
        if (previous == null) // if beginning of list,
        {
            first = link;
        } else // not at beginning,
        {
            previous.next = link;
        }
        link.next = current;
    }

    public void delete(ChecksumPair key) {
        Link previous = null;
        Link current = first;

        while (current != null && key != current.getKey()) {
            previous = current;
            current = current.next;
        }
        // disconnect link
        if (previous == null) //   if beginning of list delete first link
        {
            first = first.next;
        } else //   not at beginning
        {
            previous.next = current.next; //delete current link
        }
    }

    public Link find(ChecksumPair pKey) {
        Link current = first;
        while (current != null && current.getKey().getOffset() <= pKey.getOffset()) { // or pKey too small,
            if (current.getKey() == pKey) // found, return link
            {
                return current;
            }
            current = current.next; // go to next item
        }
        return null; // cannot find it
    }

    public void displayList() {
        System.out.print("List: ");
        Link current = first;
        int i=0;
        while (current != null) {
            //current.displayLink();
            current = current.next;
            i++;
        }
        System.out.println(i);
        //System.out.println("");
    }
}

/**
 *
 * 
 */
public class ChainingHash {
    private SortedList[] hashArray;
    private int arraySize;

    public ChainingHash(int size) {
        arraySize = size;
        hashArray = new SortedList[arraySize];
        for (int i = 0; i < arraySize; i++) {
            hashArray[i] = new SortedList();
        }
    }

    public void displayTable() {
        for (int j = 0; j < arraySize; j++) {
            System.out.print(j + ". ");
            hashArray[j].displayList();
        }
    }

    public int hashFunc(ChecksumPair pKey) {
        return pKey.hashCode() % arraySize;
    }

    public void insert(Link link) {
        ChecksumPair pKey = link.getKey();
        int hashVal = hashFunc(pKey);
        hashArray[hashVal].insert(link);
    }

    public void delete(ChecksumPair pKey) {
        int hashVal = hashFunc(pKey); // hash the pKey
        hashArray[hashVal].delete(pKey);
    }

    public Link find(ChecksumPair pKey) {
        int hashVal = hashFunc(pKey); // hash the pKey
        Link theLink = hashArray[hashVal].find(pKey); // get link
        return theLink;
    }
}