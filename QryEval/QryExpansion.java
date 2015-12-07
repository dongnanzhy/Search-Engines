/**
 * Created by dongnanzhy on 10/2/15.
 */

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

/**
 *  Pseurelevance feedback.
 */
public class QryExpansion {
    private class DocScore {
        private int docid;
        private double score;

        private DocScore(int internalDocid, double score) {
            this.docid = internalDocid;
            this.score = score;
        }
    }
    private class TermScore {
        private String term;
        private double score;

        private TermScore(String term, double score) {
            this.term = term;
            this.score = score;
        }
    }

    private int fbDocs = 0;
    private int fbTerms = 0;
    private int fbMu = 0;

    private final String field = "body";

    public QryExpansion() {
        this.fbDocs = 10;
        this.fbTerms = 10;
        this.fbMu = 0;
    }
    public QryExpansion(int fbDocs, int fbTerms, int fbMu) throws IllegalArgumentException {
        if (fbDocs <= 0) {
            throw new IllegalArgumentException
                    ("Parameter fbDocs should larger than 0");
        }
        if (fbTerms <= 0) {
            throw new IllegalArgumentException
                    ("Parameter fbTerms should larger than 0");
        }
        if (fbMu < 0) {
            throw new IllegalArgumentException
                    ("Parameter fbmu should larger than 0");
        }
        this.fbDocs = fbDocs;
        this.fbTerms = fbTerms;
        this.fbMu = fbMu;
    }

    public HashMap<Integer, String> getExpandedQry (String inputPath) throws Exception {
        HashMap<Integer, ArrayList<DocScore>> initDocs = getInitialResults(inputPath);
        return getExpandedQryHelper(initDocs);
    }
    public HashMap<Integer, String> getExpandedQry (HashMap<String, ScoreList> hm) throws Exception {
        HashMap<Integer, ArrayList<DocScore>> initDocs = getInitialResults(hm);
        return getExpandedQryHelper(initDocs);
    }

    private HashMap<Integer, String> getExpandedQryHelper (HashMap<Integer, ArrayList<DocScore>> initDocs) throws Exception {
        HashMap<Integer, String> result = new HashMap<Integer, String>();
        for (int qid : initDocs.keySet()) {
            HashMap<String, Double> hm_term = new HashMap<String, Double>();
            HashMap<Integer, TermVector> termVecs = new HashMap<Integer, TermVector>();
            for (DocScore doc: initDocs.get(qid)) {
                int docid = doc.docid;
                TermVector tm = new TermVector(docid, field);
                termVecs.put(docid, tm);
                for (int i = 1; i < tm.stemsLength(); i++) {
                    String term = tm.stemString(i);
                    if (term.contains(",") || term.contains(".")) {
                        continue;
                    }
                    if (!hm_term.containsKey(term)) {
                        hm_term.put(term, (double)tm.totalStemFreq(i));
                    }
                }
            }
            for (String term:hm_term.keySet()) {
                double termScore = 0.0;
                for (DocScore doc: initDocs.get(qid)) {
                    int docid = doc.docid;
                    double docScore = doc.score;
                    TermVector tm = termVecs.get(docid);
                    int index = tm.indexOfStem(term);
                    if (index > 0) {
                        termScore += getScore(tm, index, docid) * docScore;
                    } else {
                        termScore += getDefaultScore(hm_term.get(term), docid) * docScore;
                    }
                }
                hm_term.put(term,termScore);
            }
//            for (DocScore doc: initDocs.get(qid)) {
//                int docid = doc.docid;
//                double docScore = doc.score;
//                TermVector tm = new TermVector(docid, field);
//                for (int i = 1; i < tm.stemsLength(); i++) {
//                    String term = tm.stemString(i);
//                    if (term.contains(",") || term.contains(".")) {
//                        continue;
//                    }
//                    double termScore = getScore(tm, i, docid) * docScore;
//                    if (!hm_term.containsKey(term)) {
//                        hm_term.put(term, termScore);
//                    } else {
//                        hm_term.put(term, hm_term.get(term)+termScore);
//                    }
//                }
//            }
            ArrayList<TermScore> terms = new ArrayList<TermScore>();
            for (String term: hm_term.keySet()) {
                TermScore termS = new TermScore(term, hm_term.get(term));
                terms.add(termS);
            }
            Collections.sort(terms, new Comparator<TermScore>() {
                public int compare(TermScore s1, TermScore s2) {
                    if (s1.score > s2.score)
                        return -1;
                    else
                        if (s1.score < s2.score)
                            return 1;
                    else {
                        if (s1.term.compareTo(s2.term) > 0)
                            return 1;
                        else if (s1.term.compareTo(s2.term) < 0)
                            return -1;
                        else
                            return 0;
                    }
                }
            });
            String expandedQ = "#WAND(";
            DecimalFormat df  = new DecimalFormat("0.0000");
            for (int i = fbTerms-1; i >= 0; i--) {
                expandedQ += df.format(terms.get(i).score) + " " + terms.get(i).term + " ";
            }
            expandedQ += ")";
            result.put(qid, expandedQ);
        }
        return result;

    }

    private double getScore (TermVector tm, int index, int docid) throws IOException {
        int tf = tm.stemFreq(index);
        long ctf = tm.totalStemFreq(index);
        long collectLen = Idx.getSumOfFieldLengths(field);
        int docLen = Idx.getFieldLength(field, docid);
        double p_td = (tf + fbMu * (double)ctf/(double)collectLen) / (docLen + fbMu);
        double idf = Math.log((double)collectLen/ctf);
        return p_td*idf;
    }
    private double getDefaultScore (double ctf, int docid) throws IOException {
        long collectLen = Idx.getSumOfFieldLengths(field);
        int docLen = Idx.getFieldLength(field, docid);
        double p_td = (0 + fbMu * ctf/(double)collectLen) / (docLen + fbMu);
        double idf = Math.log((double)collectLen/ctf);
        return p_td*idf;
    }

    private HashMap<Integer, ArrayList<DocScore>> getInitialResults (String inputPath) throws Exception {
        HashMap<Integer, ArrayList<DocScore>> results = new HashMap<Integer, ArrayList<DocScore>>();

        File inputFile = new File (inputPath);

        if (! inputFile.canRead ()) {
            throw new IllegalArgumentException
                    ("Can't read " + inputPath);
        }

        Scanner scan = new Scanner(inputFile);
        String line = null;
        do {
            line = scan.nextLine();
            String[] info = line.split (" ");
            int qid = Integer.valueOf(info[0]);
            if (!results.containsKey(qid)) {
                ArrayList<DocScore> list = new ArrayList<DocScore>();
                DocScore ds = new DocScore(Idx.getInternalDocid(info[2]),Double.valueOf(info[4]));
                list.add(ds);
                results.put(qid,list);
            } else {
                if (results.get(qid).size() >= fbDocs) {
                    continue;
                } else {
                    results.get(qid).add(new DocScore(Idx.getInternalDocid(info[2]),Double.valueOf(info[4])) );
                }
            }
        } while (scan.hasNext());

        scan.close();
        return results;
    }
    private HashMap<Integer, ArrayList<DocScore>> getInitialResults (HashMap<String, ScoreList> hm) throws Exception {
        HashMap<Integer, ArrayList<DocScore>> results = new HashMap<Integer, ArrayList<DocScore>>();

        for (String s:hm.keySet()) {
            ArrayList<DocScore> list = new ArrayList<DocScore>();
            int qid = Integer.valueOf(s);
            ScoreList r = hm.get(s);
            for (int i = 0; i < Math.min(fbDocs,r.size()); i++) {
                DocScore ds = new DocScore(r.getDocid(i), r.getDocidScore(i));
                list.add(ds);
            }
            results.put(qid, list);
        }

        return results;
    }

}