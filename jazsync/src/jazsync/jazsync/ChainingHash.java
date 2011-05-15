package jazsync.jazsync;

import java.io.IOException;
import java.util.Arrays;
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

    public int getWeakKey() {
        return blockSums.getWeak();
    }

    public byte[] getStrongKey(){
        return blockSums.getStrong();
    }

    public ChecksumPair getKey() {
        return blockSums;
    }

    public void displayLink() {
        System.out.print(blockSums.toString() + " ");
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
        int hashVal = hashFunc(pKey);
        hashArray[hashVal].delete(pKey);
    }

    public Link find(ChecksumPair pKey) {
        int hashVal = hashFunc(pKey);
        Link theLink = hashArray[hashVal].find(pKey); 
        return theLink;
    }

    public Link findMatch(ChecksumPair pKey) {
        int hashVal = hashFunc(pKey);
        Link theLink = hashArray[hashVal].findMatch(pKey);
        return theLink;
    }
}