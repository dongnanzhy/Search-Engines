/**
 * Created by dongnanzhy on 9/19/15.
 */
import java.io.*;
import java.util.*;

/**
 *  The NEAR operator for all retrieval models.
 */
public class QryIopNear extends QryIop {

    private int distance = 0;

    public QryIopNear(int n) {
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
            while (pointers[0] < locations.get(0).size() && !exhausted) {
                for (int i = 1; i < locations.size(); i++) {
                    if (pointers[i] >= locations.get(i).size() || exhausted) {
                        exhausted = true;
                        break;
                    }
                    while (locations.get(i).get(pointers[i]) < locations.get(i-1).get(pointers[i-1])) {
                        pointers[i]++;
                        if (pointers[i] >= locations.get(i).size()) {
                            exhausted = true;
                            break;
                        }
                    }
                }
                if (exhausted)
                    break;
                boolean near = true;
                for (int i = 1; i < locations.size(); i++) {
                    near = near & (locations.get(i).get(pointers[i]) - locations.get(i-1).get(pointers[i-1]) <= distance);
                    if (!near)
                        break;
                }
                if (near) {
                    positions.add(locations.get(locations.size()-1).get(pointers[locations.size()-1]));
                    for (int i = 0; i < locations.size(); i++) {
                        pointers[i]++;
                    }
                } else {
                    pointers[0]++;
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