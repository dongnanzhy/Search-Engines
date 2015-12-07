/**
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.lang.IllegalArgumentException;

/**
 *  The SCORE operator for all retrieval models.
 */
public class QrySopScore extends QrySop {

  /**
   *  Document-independent values that should be determined just once.
   *  Some retrieval models have these, some don't.
   */
  
  /**
   *  Indicates whether the query has a match.
   *  @param r The retrieval model that determines what is a match
   *  @return True if the query matches, otherwise false.
   */
  public boolean docIteratorHasMatch (RetrievalModel r) {
    return this.docIteratorHasMatchFirst (r);
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
    } else if (r instanceof RetrievalModelBM25) {
      return this.getScoreBM25((RetrievalModelBM25)r);
    } else if (r instanceof RetrievalModelIndri) {
      return this.getScoreIndri((RetrievalModelIndri)r);
    } else {
      throw new IllegalArgumentException
        (r.getClass().getName() + " doesn't support the SCORE operator.");
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
      int ctf =((QryIop) this.args.get(0)).getCtf();
      long collectLen = Idx.getSumOfFieldLengths(((QryIop) this.args.get(0)).getField());
      int docLen = Idx.getFieldLength(((QryIop) this.args.get(0)).getField(), (int) docid);
      float lambda = ((RetrievalModelIndri) r).getlambda();
      float mu = ((RetrievalModelIndri) r).getmu();
      double result = (1-lambda) * (0+mu*ctf/collectLen) / (docLen+mu)
              + (lambda* ctf / collectLen);
      return result;
    } else {
      throw new IllegalArgumentException
              (r.getClass().getName() + " doesn't support the SCORE operator default score.");
    }
  }

  /**
   *  getScore for the Unranked retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      return 1.0;
    }
  }

  /**
   *  getScore for the Ranked retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreRankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      return ((QryIop) this.args.get(0)).docIteratorGetMatchPosting().tf;
    }
  }

  /**
   *  getScore for the BM25 retrieval model.
   *  @param r The retrieval model of BM25 that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreBM25 (RetrievalModelBM25 r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    }
    int docid = this.docIteratorGetMatch();
    int tf =  ((QryIop) this.args.get(0)).docIteratorGetMatchPosting().tf;
    int df =((QryIop) this.args.get(0)).getDf();
    int N_field = Idx.getDocCount(((QryIop) this.args.get(0)).getField());
    long N = Idx.getNumDocs();
    float avg_docLen = Idx.getSumOfFieldLengths(((QryIop) this.args.get(0)).getField()) / (float) N_field;
    int docLen = Idx.getFieldLength(((QryIop) this.args.get(0)).getField(), docid);
    double result = Math.max(0, Math.log((N - df + 0.5) / (df + 0.5))) *
            tf / (tf + r.getK_1() * (1-r.getb()+ r.getb()*(docLen/avg_docLen)));
    return result;
  }

  /**
   *  getScore for the Indri retrieval model.
   *  @param r The retrieval model of Indri that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreIndri (RetrievalModelIndri r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    }
    int docid = this.docIteratorGetMatch();
    int tf =  ((QryIop) this.args.get(0)).docIteratorGetMatchPosting().tf;
    int ctf =((QryIop) this.args.get(0)).getCtf();
    long collectLen = Idx.getSumOfFieldLengths(((QryIop) this.args.get(0)).getField());
    int docLen = Idx.getFieldLength(((QryIop) this.args.get(0)).getField(), docid);
    double result = (1-r.getlambda()) * (tf+r.getmu()*ctf/collectLen) / (docLen+r.getmu())
            + (r.getlambda() * ctf / collectLen);
    return result;
  }

  /**
   *  Initialize the query operator (and its arguments), including any
   *  internal iterators.  If the query operator is of type QryIop, it
   *  is fully evaluated, and the results are stored in an internal
   *  inverted list that may be accessed via the internal iterator.
   *  @param r A retrieval model that guides initialization
   *  @throws IOException Error accessing the Lucene index.
   */
  public void initialize (RetrievalModel r) throws IOException {

    Qry q = this.args.get (0);
    q.initialize (r);
  }

}
