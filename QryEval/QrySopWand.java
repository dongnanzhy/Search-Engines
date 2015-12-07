/**
 * Created by dongnanzhy on 10/23/15.
 */

import java.io.*;
import java.util.ArrayList;

/**
 *  The WAND operator for all retrieval models.
 */
public class QrySopWand extends QrySop {

    private ArrayList<Double> weights;
    private double totalW;
    private boolean isWeightPara;

    public QrySopWand() {
        this.weights = new ArrayList<Double>();
        this.totalW = 0;
        this.isWeightPara = true;
    }
    public void appendArg(double w) {
        weights.add(w);
        totalW += w;
        isWeightPara = false;
    }
    public void appendArg(Qry q) {
        super.appendArg(q);
        isWeightPara = true;
    }
    public void appendWeightedArg(Qry q) {
        double w = weights.get(weights.size()-1);
        weights.add(w);
        totalW += w;
        super.appendArg(q);
        isWeightPara = true;
    }
    public boolean isWeight () {
        return this.isWeightPara;
    }
    public void delPrevWeight () {
        totalW -= weights.get(weights.size()-1);
        weights.remove(weights.size()-1);
        this.isWeightPara = true;
    }
    /**
     *  Indicates whether the query has a match.
     *  @param r The retrieval model that determines what is a match
     *  @return True if the query matches, otherwise false.
     */
    public boolean docIteratorHasMatch (RetrievalModel r) {
        if (r instanceof RetrievalModelIndri) {
            return this.docIteratorHasMatchMin(r);
        } else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the WAND operator.");
        }
    }

    /**
     *  Get a score for the document that docIteratorHasMatch matched.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    public double getScore (RetrievalModel r) throws IOException {
        if (r instanceof RetrievalModelIndri) {
            return this.getScoreIndri(r);
        } else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the WAND operator.");
        }
    }

    /**
     *  Get a default score for the document that docIteratorHasMatch matched.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    public double getDefaultScore (RetrievalModel r, long docid) throws IOException {
        if (r instanceof RetrievalModelIndri) {
            double result = 1.0;
            for (int i = 0; i < this.args.size(); i++) {
                QrySop q_i = (QrySop) this.args.get(i);
                result *= Math.pow(q_i.getDefaultScore(r, docid), weights.get(i) / totalW);
            }
            return result;
        } else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the WAND operator default score.");
        }
    }

    /**
     *  getScore for the Indri retrieval model.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    private double getScoreIndri (RetrievalModel r) throws IOException {
        if (! this.docIteratorHasMatchCache()) {
            return 0.0;
        } else {
            int docid = this.docIteratorGetMatch();
            double result = 1.0;
            for (int i = 0; i < this.args.size(); i++) {
                QrySop q_i = (QrySop) this.args.get(i);
                if (q_i.docIteratorHasMatch (r) && q_i.docIteratorGetMatch () == docid) {
                    result *= Math.pow(q_i.getScore(r), weights.get(i)/totalW);
                } else {
                    result *= Math.pow(q_i.getDefaultScore(r, docid), weights.get(i)/totalW);
                }
            }
            return result;
        }
    }

    @Override public String toString(){

        String result = new String ();

        for (int i=0; i<this.args.size(); i++)
            result += String.valueOf(weights.get(i)) + " " + this.args.get(i) + " ";

        return (this.displayName + "( " + result + ")");
    }

}
