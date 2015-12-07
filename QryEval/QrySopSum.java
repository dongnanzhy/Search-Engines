/**
 * Created by dongnanzhy on 10/1/15.
 */

import java.io.*;
import java.util.HashMap;
import java.util.Vector;

/**
 *  The SUM operator for BM25.
 */
public class QrySopSum extends QrySop {

    /**
     *  Indicates whether the query has a match.
     *  @param r The retrieval model that determines what is a match
     *  @return True if the query matches, otherwise false.
     */
    public boolean docIteratorHasMatch (RetrievalModel r) {
        return this.docIteratorHasMatchMin (r);
    }

    /**
     *  Get a score for the document that docIteratorHasMatch matched.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    public double getScore (RetrievalModel r) throws IOException {

        if (r instanceof RetrievalModelBM25) {
            return this.getScoreBM25((RetrievalModelBM25) r);
        } else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the SUM operator.");
        }
    }

    /**
     *  Get a default score for the document that docIteratorHasMatch matched. No specific operation now.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    public double getDefaultScore (RetrievalModel r, long docid) throws IOException {

        if (r instanceof RetrievalModelIndri) {
            return 0;
        } else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the SUM operator default score.");
        }
    }
    /**
     *  getScore for the BM25 model.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    private double getScoreBM25 (RetrievalModelBM25 r) throws IOException {
        HashMap<String, Integer> hm_count = new HashMap<String, Integer>();
        HashMap<String, QrySop> map = new HashMap<String, QrySop>();
        if (! this.docIteratorHasMatchCache()) {
            return 0.0;
        } else {
            double sum = 0;
            int docid = this.docIteratorGetMatch();
            for (int i = 0; i < this.args.size(); i++) {
                QrySop q_i = (QrySop) this.args.get(i);
                if (q_i.docIteratorHasMatch (null) && q_i.docIteratorGetMatch () == docid) {
                    String q_s = q_i.toString();
                    if (hm_count.containsKey(q_s)) {
                        hm_count.put(q_s, hm_count.get(q_s)+1);
                    } else {
                        hm_count.put(q_s, 1);
                        map.put(q_s, q_i);
                    }
                }
            }
            //System.out.println(map.size());
            for (String q_s : hm_count.keySet()) {
                sum += map.get(q_s).getScore(r) * (r.getK_3()+1) * hm_count.get(q_s) / (r.getK_3() + hm_count.get(q_s));
                //System.out.println(q_s + " number : " + hm_count.get(q_s));
            }
            return sum;
        }
    }

}
