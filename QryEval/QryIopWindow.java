/**
 * Created by dongnanzhy on 10/23/15.
 */

import java.io.*;
import java.util.*;

/**
 *  The Window operator for all retrieval models.
 */
public class QryIopWindow extends QryIop {

    private int distance = 0;

    public QryIopWindow(int n) {
        distance = n;
    }

    /**
     *  Evaluate the query operator; the result is an internal inverted
     *  list that may be accessed via the internal iterators.
     *  @throws IOException Error accessing the Lucene index.
     */
    protected void evaluate () throws IOException {

        //  Create an empty inverted list.  If there are no query arguments,
        //  that's the final result.

        this.invertedList = new InvList (this.getField());

        if (args.size () == 0) {
            return;
        }

        //  Each pass of the loop adds 1 document to result inverted list
        //  until all of the argument inverted lists are depleted.

        while (this.docIteratorHasMatchAll(null)) {

            int docid = this.args.get(0).docIteratorGetMatch();

            if (docid == Qry.INVALID_DOCID)
                break;				// All docids have been processed.  Done.


            List<Integer> positions = new ArrayList<Integer>();
            List<Vector<Integer>> locations = new ArrayList<Vector<Integer>>();

            for (Qry q_i: this.args) {
                if (q_i.docIteratorHasMatch (null) &&
                        (q_i.docIteratorGetMatch () == docid)) {
                    Vector<Integer> locations_i =
                            ((QryIop) q_i).docIteratorGetMatchPosting().positions;
                    locations.add(locations_i);
                    q_i.docIteratorAdvancePast (docid);
                }
            }

            int[] pointers = new int[locations.size()];
            for (int i = 0; i < locations.size(); i++) {
                pointers[i] = 0;
            }
            boolean exhausted = false;
            // stop loop if one posting get to end
            while (!exhausted) {
                int windowStart = Integer.MAX_VALUE;
                int windowEnd = Integer.MIN_VALUE;
                int windowStart_index = 0;
                int windowEnd_index = 0;
                for (int i = 0; i < locations.size(); i++) {
                    if (pointers[i] >= locations.get(i).size() || exhausted) {
                        exhausted =  true;
                        break;
                    }
                    if (windowStart > locations.get(i).get(pointers[i])) {
                        windowStart = locations.get(i).get(pointers[i]);
                        windowStart_index = i;
                    }
                    if (windowEnd < locations.get(i).get(pointers[i])) {
                        windowEnd = locations.get(i).get(pointers[i]);
                        windowEnd_index = i;
                    }
                }
                if (exhausted)
                    break;
                if (1 + windowEnd - windowStart <= distance) {
                    positions.add(locations.get(windowEnd_index).get(pointers[windowEnd_index]));
                    for (int i = 0; i < locations.size(); i++) {
                        pointers[i]++;
                    }
                } else {
                    pointers[windowStart_index]++;
                }
            }

            if (positions.size() == 0) {
                continue;
            }
            Collections.sort(positions);
            this.invertedList.appendPosting(docid, positions);
        }
    }

}