/**
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.Vector;

/**
 *  The OR operator for all retrieval models.
 */
public class QrySopOr extends QrySop {

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

    if (r instanceof RetrievalModelUnrankedBoolean) {
      return this.getScoreUnrankedBoolean (r);
    } else if (r instanceof RetrievalModelRankedBoolean) {
      return this.getScoreRankedBoolean(r);
    } else if (r instanceof RetrievalModelIndri) {
      return this.getScoreIndri(r);
    } else {
      throw new IllegalArgumentException
        (r.getClass().getName() + " doesn't support the OR operator.");
    }
  }

  /**
   *  Get a default score for the document that docIteratorHasMatch matched.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getDefaultScore (RetrievalModel r,long docid) throws IOException {

    if (r instanceof RetrievalModelIndri) {
      double result = 1.0;
      for (int i = 0; i < this.args.size(); i++) {
        QrySop q_i = (QrySop) this.args.get(i);
        result *= (1.0 - q_i.getDefaultScore(r, docid));
      }
      result = 1.0 - result;
      return result;
    } else {
      throw new IllegalArgumentException
              (r.getClass().getName() + " doesn't support the OR operator default score.");
    }
  }
  
  /**
   *  getScore for the UnrankedBoolean retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  private double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      return 1.0;
    }
  }

  /**
   *  getScore for the RankedBoolean retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  private double getScoreRankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      double max = 0;
      int docid = this.docIteratorGetMatch();
      for (int i = 0; i < this.args.size(); i++) {
         QrySop q_i = (QrySop) this.args.get(i);
         if (q_i.docIteratorHasMatch (r) && q_i.docIteratorGetMatch () == docid) {
            max = Math.max(max, q_i.getScore(r));
         }
      }
      return max;
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
      int len = this.args.size();
      for (int i = 0; i < this.args.size(); i++) {
        QrySop q_i = (QrySop) this.args.get(i);
        if (q_i.docIteratorHasMatch (r) && q_i.docIteratorGetMatch () == docid) {
          result *= (1.0 - q_i.getScore(r));
        } else {
          result *= (1.0 - q_i.getDefaultScore(r, docid));
        }
      }
      result = 1.0 - result;
      return result;
    }
  }

}
