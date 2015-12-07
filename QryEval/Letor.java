/**
 * Created by dongnanzhy on 11/27/15.
 */

import org.apache.lucene.index.Term;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

/**
 *  Learning to rank
 */
public class Letor {

    HashMap<String, Double> pageRankScore;
    HashSet<Integer> disabledF;

    public Letor() {
        this.pageRankScore = new HashMap<String, Double>();
        disabledF = new HashSet<Integer>();
    }

    public class Feature {
        public int fID = 0;
        public double score = 0.0;
        boolean isValid = true;
        public Feature (int fID, double score) {
            this.fID = fID;
            this.score =score;
        }
        public Feature (Feature feature) {
            this.fID = feature.fID;
            this.score = feature.score;
        }
        public void setScore (double score) {
            this.score = score;
        }
        public void setFalse() {
            this.isValid = false;
        }
    }
    public class FeatureVec<Feature> extends Vector<Feature> {
        public int label = 0;
        public int qid = 0;
        public String docID = "";

        public FeatureVec (int qid, String docID, int label) {
            this.label = label;
            this.qid = qid;
            this.docID = docID;
        }
        public FeatureVec (int qid, String docID) {
            this.label = 0;
            this.qid = qid;
            this.docID = docID;
        }
//        public FeatureVec (FeatureVec v) {
//            super(v);
//            this.label = v.label;
//            this.qid = v.qid;
//            this.docID = v.docID;
//        }

    }

    public void getPageRankS (String pageRankFile) throws IOException {
        BufferedReader input = null;
        try {
            String line = null;
            input = new BufferedReader(new FileReader(pageRankFile));
            while ((line = input.readLine()) != null) {
                String[] arr = line.split("\t");
                if (pageRankScore.containsKey(arr[0])) {
//                    throw new IllegalArgumentException
//                            ("page rank score file error");
                    pageRankScore.put(arr[0], Double.valueOf(arr[1]));
                } else {
                    pageRankScore.put(arr[0], Double.valueOf(arr[1]));
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            input.close();
        }
    }

    public void getDisabledF (String input) {
        String[] arr = input.split(",");
        for (String s : arr) {
            disabledF.add(Integer.valueOf(s));
        }
    }

    public void getTrainFile (String queryFilePath, String queryRelFilePath, String trainOutputPath) throws Exception {
        HashMap<Integer, String> queries = readQry(queryFilePath);
        ArrayList<FeatureVec> docRel = readRelevanceFile(queryRelFilePath);
        HashMap<Integer, ArrayList<FeatureVec>> trainData = getData(queries, docRel);
        HashMap<Integer, ArrayList<FeatureVec>> normTrainData = normalize(trainData);
        writeData(trainOutputPath, normTrainData);
    }

    public void getTestFile (String queryFilePath, HashMap<String, ScoreList> hm, String testOutputPath) throws Exception {
        HashMap<Integer, String> queries = readQry(queryFilePath);
        ArrayList<FeatureVec> initDocs = readInitialResults(hm);
        HashMap<Integer, ArrayList<FeatureVec>> testData = getData(queries, initDocs);
        HashMap<Integer, ArrayList<FeatureVec>> normTestData = normalize(testData);
        writeData(testOutputPath, normTestData);
    }

    public void runSVMTrain (String svmRankLearnPath, String CValue, String svmRankModelFile, String trainFeatureVectorsFile) throws Exception {
        Process cmdProc = Runtime.getRuntime().exec(
                new String[] { svmRankLearnPath, "-c", String.valueOf(CValue), trainFeatureVectorsFile,
                        svmRankModelFile });

        // The stdout/stderr consuming code MUST be included.
        // It prevents the OS from running out of output buffer space and stalling.

        // consume stdout and print it out for debugging purposes
        BufferedReader stdoutReader = new BufferedReader(
                new InputStreamReader(cmdProc.getInputStream()));
        String line;
        while ((line = stdoutReader.readLine()) != null) {
            System.out.println(line);
        }
        // consume stderr and print it for debugging purposes
        BufferedReader stderrReader = new BufferedReader(
                new InputStreamReader(cmdProc.getErrorStream()));
        while ((line = stderrReader.readLine()) != null) {
            System.out.println(line);
        }

        // get the return value from the executable. 0 means success, non-zero
        // indicates a problem
        int retValue = cmdProc.waitFor();
        if (retValue != 0) {
            throw new Exception("SVM Rank crashed.");
        }
    }

    public void runSVMTest (String svmRankClassifyPath, String svmRankModelFile, String testFeatureVectorsFile, String testDocumentScores) throws Exception {
        Process cmdProc = Runtime.getRuntime().exec(
                new String[] { svmRankClassifyPath, testFeatureVectorsFile, svmRankModelFile, testDocumentScores });

        // The stdout/stderr consuming code MUST be included.
        // It prevents the OS from running out of output buffer space and stalling.

        // consume stdout and print it out for debugging purposes
        BufferedReader stdoutReader = new BufferedReader(
                new InputStreamReader(cmdProc.getInputStream()));
        String line;
        while ((line = stdoutReader.readLine()) != null) {
            System.out.println(line);
        }
        // consume stderr and print it for debugging purposes
        BufferedReader stderrReader = new BufferedReader(
                new InputStreamReader(cmdProc.getErrorStream()));
        while ((line = stderrReader.readLine()) != null) {
            System.out.println(line);
        }

        // get the return value from the executable. 0 means success, non-zero
        // indicates a problem
        int retValue = cmdProc.waitFor();
        if (retValue != 0) {
            throw new Exception("SVM Rank crashed.");
        }
    }

    public void getFinalResult (String svmScorePath, HashMap<String, ScoreList> hm, String testOutputPath) throws Exception {
        ArrayList<Double> svmScores = readSVMScore (svmScorePath);
        HashMap<Integer, ScoreList> finalResult = getRerankDoc (hm, svmScores);
        writeResult(testOutputPath, finalResult);
    }

    private HashMap<Integer, String> readQry(String queryFilePath) throws IOException {
        BufferedReader input = null;
        HashMap<Integer, String> hm = new HashMap<Integer, String>();
        try {
            String qLine = null;
            input = new BufferedReader(new FileReader(queryFilePath));
            //  Each pass of the loop processes one query.
            while ((qLine = input.readLine()) != null) {
                int d = qLine.indexOf(':');
                if (d < 0) {
                    throw new IllegalArgumentException
                            ("Syntax error:  Missing ':' in query line.");
                }
                String qid = qLine.substring(0, d);
                String query = qLine.substring(d + 1);
                hm.put(Integer.valueOf(qid), query);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            input.close();
        }
        return hm;
    }

    private ArrayList<FeatureVec> readRelevanceFile (String queryRelFilePath) throws IOException {
        BufferedReader input = null;
        ArrayList<FeatureVec> list = new ArrayList<FeatureVec>();
        try {
            String line = null;
            input = new BufferedReader(new FileReader(queryRelFilePath));
            while ((line = input.readLine()) != null) {
                String[] arr = line.split(" ");
                int qid = Integer.valueOf(arr[0]);
                String docID = arr[2];
                int label = Integer.valueOf(arr[3]);
                FeatureVec featureVec = new FeatureVec(qid, docID, label);
                list.add(featureVec);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            input.close();
        }
        return list;
    }

    private ArrayList<FeatureVec>  readInitialResults (HashMap<String, ScoreList> hm) throws Exception {
        ArrayList<FeatureVec> list = new ArrayList<FeatureVec>();
        for (String s:hm.keySet()) {
            int qid = Integer.valueOf(s);
            ScoreList r = hm.get(s);
            for (int i = 0; i < Math.min(r.size(), 100); i++) {
                String docID = Idx.getExternalDocid(r.getDocid(i));
                int label = 0;
                FeatureVec featureVec = new FeatureVec(qid, docID, label);
                list.add(featureVec);
            }
        }
        return list;
    }

    private ArrayList<Double> readSVMScore (String testFileScore) throws IOException {
        BufferedReader input = null;
        ArrayList<Double> list = new ArrayList<Double>();
        try {
            String line = null;
            input = new BufferedReader(new FileReader(testFileScore));
            while ((line = input.readLine()) != null) {
                list.add(Double.valueOf(line));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            input.close();
        }
        //System.out.println(list.size());
        return list;
    }

    private void writeData(String outPath, HashMap<Integer, ArrayList<FeatureVec>> featureVecs) throws IOException {
        File file = new File(outPath);
        FileWriter writer = new FileWriter(file);
        List sortedKeys = new ArrayList(featureVecs.keySet());
        Collections.sort(sortedKeys);
        for (int i = 0; i < sortedKeys.size(); i++) {
            for (FeatureVec featureVec : featureVecs.get(sortedKeys.get(i))) {
                writer.write(featureVec.label + " qid:" + featureVec.qid + " ");
                for (int k = 0; k < featureVec.size(); k++) {
                    Feature f = (Feature) featureVec.get(k);
                    if (disabledF.isEmpty() || !disabledF.contains(f.fID)) {
                        writer.write(f.fID + ":" + f.score + " ");
                    }
                }
                writer.write("# " + featureVec.docID + "\n");
            }
        }
        writer.close();
    }

    private void writeResult (String outPath, HashMap<Integer, ScoreList> hm) throws IOException {
        File file = new File(outPath);
        FileWriter writer = new FileWriter(file);
        List<Integer> sortedKeys = new ArrayList<Integer>(hm.keySet());
        Collections.sort(sortedKeys);
        for (int i = 0; i < sortedKeys.size(); i++) {
            int qid = sortedKeys.get(i);
            ScoreList r = hm.get(qid);
            if (r.size() < 1) {
                writer.write(qid + "\tQ0\tdummy\t1\t0\trun-1\n");
            } else {
                for (int j = 0; j < r.size() && j < 100; j++) {
                    writer.write(qid + "\tQ0\t" + Idx.getExternalDocid(r.getDocid(j)) + "\t"
                            + String.valueOf(j+1) + "\t" + r.getDocidScore(j) + "\trun-1\n");
                }
            }
        }
        writer.close();
    }

    private HashMap<Integer, ArrayList<FeatureVec>> getData (HashMap<Integer, String> queries, ArrayList<FeatureVec> featureVecs) throws Exception {
        HashMap<Integer, ArrayList<FeatureVec>> result = new HashMap<Integer, ArrayList<FeatureVec>>();
        for (int qid: queries.keySet()) {
            ArrayList<FeatureVec> list = new ArrayList<FeatureVec>();
            result.put(qid,list);
        }
        //ArrayList<FeatureVec> result = new ArrayList<FeatureVec>();
        for (FeatureVec featureVec : featureVecs) {
            if (!queries.containsKey(featureVec.qid)) {
                continue;
            }
            String query = queries.get(featureVec.qid);
            String[] queryStems = QryEval.tokenizeQuery(query);
            String externalID = featureVec.docID;
            int docid = Idx.getInternalDocid(externalID);
            String rawUrl = Idx.getAttribute ("rawUrl", docid);

            Feature f_1 = new Feature(1, Double.parseDouble(Idx.getAttribute ("score", docid))); featureVec.addElement(f_1);
            Feature f_2 = new Feature(2, urlDepth(rawUrl)); featureVec.addElement(f_2);
            Feature f_3 = new Feature(3, wikiScore(rawUrl)); featureVec.addElement(f_3);
            if (!pageRankScore.containsKey(externalID)) {
                Feature f_4 = new Feature(4, 0);
                f_4.setFalse();
                featureVec.addElement(f_4);
            } else {
                Feature f_4 = new Feature(4, pageRankScore.get(externalID));
                featureVec.addElement(f_4);
            }
            double score = getBM25Score(queryStems, docid, "body");
            if (score < 0) {
                Feature f_5 = new Feature(5, 0);
                f_5.setFalse();
                featureVec.addElement(f_5);
            } else {
                Feature f_5 = new Feature(5, score);
                featureVec.addElement(f_5);
            }
            score = getIndriScore(queryStems, docid, "body");
            if (score < 0) {
                Feature f_6 = new Feature(6, 0);
                f_6.setFalse();
                featureVec.addElement(f_6);
            } else {
                Feature f_6 = new Feature(6, score);
                featureVec.addElement(f_6);
            }
            score = getOverlapScore(queryStems, docid, "body");
            if (score < 0) {
                Feature f_7 = new Feature(7, 0);
                f_7.setFalse();
                featureVec.addElement(f_7);
            } else {
                Feature f_7 = new Feature(7, score);
                featureVec.addElement(f_7);
            }
            score = getBM25Score(queryStems, docid, "title");
            if (score < 0) {
                Feature f_8 = new Feature(8, 0);
                f_8.setFalse();
                featureVec.addElement(f_8);
            } else {
                Feature f_8 = new Feature(8, score);
                featureVec.addElement(f_8);
            }
            score = getIndriScore(queryStems, docid, "title");
            if (score < 0) {
                Feature f_9 = new Feature(9, 0);
                f_9.setFalse();
                featureVec.addElement(f_9);
            } else {
                Feature f_9 = new Feature(9, score);
                featureVec.addElement(f_9);
            }
            score = getOverlapScore(queryStems, docid, "title");
            if (score < 0) {
                Feature f_10 = new Feature(10, 0);
                f_10.setFalse();
                featureVec.addElement(f_10);
            } else {
                Feature f_10 = new Feature(10, score);
                featureVec.addElement(f_10);
            }
            score = getBM25Score(queryStems, docid, "url");
            if (score < 0) {
                Feature f_11 = new Feature(11, 0);
                f_11.setFalse();
                featureVec.addElement(f_11);
            } else {
                Feature f_11 = new Feature(11, score);
                featureVec.addElement(f_11);
            }
            score = getIndriScore(queryStems, docid, "url");
            if (score < 0) {
                Feature f_12 = new Feature(12, 0);
                f_12.setFalse();
                featureVec.addElement(f_12);
            } else {
                Feature f_12 = new Feature(12, score);
                featureVec.addElement(f_12);
            }
            score = getOverlapScore(queryStems, docid, "url");
            if (score < 0) {
                Feature f_13 = new Feature(13, 0);
                f_13.setFalse();
                featureVec.addElement(f_13);
            } else {
                Feature f_13 = new Feature(13, score);
                featureVec.addElement(f_13);
            }
            score = getBM25Score(queryStems, docid, "inlink");
            if (score < 0) {
                Feature f_14 = new Feature(14, 0);
                f_14.setFalse();
                featureVec.addElement(f_14);
            } else {
                Feature f_14 = new Feature(14, score);
                featureVec.addElement(f_14);
            }
            score = getIndriScore(queryStems, docid, "inlink");
            if (score < 0) {
                Feature f_15 = new Feature(15, 0);
                f_15.setFalse();
                featureVec.addElement(f_15);
            } else {
                Feature f_15 = new Feature(15, score);
                featureVec.addElement(f_15);
            }
            score = getOverlapScore(queryStems, docid, "inlink");
            if (score < 0) {
                Feature f_16 = new Feature(16, 0);
                f_16.setFalse();
                featureVec.addElement(f_16);
            } else {
                Feature f_16 = new Feature(16, score);
                featureVec.addElement(f_16);
            }
            score = getSequenOverlapScore(queryStems, docid);
            if (score < 0) {
                Feature f_17 = new Feature(17, 0);
                f_17.setFalse();
                featureVec.addElement(f_17);
            } else {
                Feature f_17 = new Feature(17, score);
                featureVec.addElement(f_17);
            }
            Feature f_18 = new Feature(18, getAvgDFScore(docid)); featureVec.addElement(f_18);
            result.get(featureVec.qid).add(featureVec);

        }
        return result;
    }

    private HashMap<Integer, ArrayList<FeatureVec>>  normalize (HashMap<Integer, ArrayList<FeatureVec>> featureVecs) {
        HashMap<Integer, FeatureVec> maxF = new HashMap<Integer, FeatureVec>();
        HashMap<Integer, FeatureVec> minF = new HashMap<Integer, FeatureVec>();
        for (int qid : featureVecs.keySet()) {
            for (FeatureVec featureVec : featureVecs.get(qid)) {
                if (!maxF.containsKey(qid)) {
                    FeatureVec maxFeature = new FeatureVec(featureVec.qid, featureVec.docID, featureVec.label);
                    for (int i = 0; i < featureVec.size(); i++) {
                        Feature f = (Feature) featureVec.get(i);
                        maxFeature.addElement(new Feature(f));
                    }
                    maxF.put(qid, maxFeature);
                } else {
                    for (int i = 0; i < featureVec.size(); i++) {
                        Feature f = (Feature) featureVec.get(i);
                        Feature fMax = (Feature) maxF.get(qid).get(i);
                        if (f.score > fMax.score && f.isValid) {
                            fMax.setScore(f.score);
                        }
                    }
                }
                if (!minF.containsKey(qid)) {
                    FeatureVec minFeature = new FeatureVec(featureVec.qid, featureVec.docID, featureVec.label);
                    for (int i = 0; i < featureVec.size(); i++) {
                        Feature f = (Feature) featureVec.get(i);
                        minFeature.addElement(new Feature(f));
                    }
                    minF.put(qid, minFeature);
                } else {
                    for (int i = 0; i < featureVec.size(); i++) {
                        Feature f = (Feature) featureVec.get(i);
                        Feature fMin = (Feature) minF.get(qid).get(i);
                        if (f.score < fMin.score && f.isValid) {
                            fMin.setScore(f.score);
                        }
                    }
                }
            }
        }
        for (int qid : featureVecs.keySet()) {
            for (FeatureVec featureVec : featureVecs.get(qid)) {
                FeatureVec maxVec = maxF.get(qid);
                FeatureVec minVec = minF.get(qid);
                for (int i = 0; i < featureVec.size(); i++) {
                    Feature f = (Feature) featureVec.get(i);
                    Feature fMax = (Feature) maxVec.get(i);
                    Feature fMin = (Feature) minVec.get(i);
                    if (fMax.score == fMin.score || !f.isValid) {
                        f.setScore(0);
                    } else {
                        f.setScore((f.score - fMin.score) / (fMax.score - fMin.score));
                    }
                }
            }
        }

        return featureVecs;
    }

    private HashMap<Integer, ScoreList> getRerankDoc (HashMap<String, ScoreList> initRank, ArrayList<Double> svmScore) {
        HashMap<Integer, ScoreList> result = new HashMap<Integer, ScoreList>();
        List<Integer> sortedKeys = new ArrayList<Integer>();
        for (String query : initRank.keySet()) {
            sortedKeys.add(Integer.valueOf(query));
        }
        Collections.sort(sortedKeys);
        int index = 0;
        for (int i = 0; i < sortedKeys.size(); i++) {
            ScoreList rerankedR = new ScoreList();
            ScoreList initR = initRank.get(String.valueOf(sortedKeys.get(i)));
            for (int j = 0; j < Math.min(initR.size(), 100); j++) {
                rerankedR.add(initR.getDocid(j), svmScore.get(index++));
            }
            rerankedR.sort();
            result.put(sortedKeys.get(i), rerankedR);
        }
        return result;
    }

    private double getBM25Score (String[] queryStems, int docid, String field) throws IOException {
        RetrievalModelBM25 r = new RetrievalModelBM25();
        double score = 0;
        TermVector tm = new TermVector(docid, field);
        if (tm.stemsLength() <= 1) return -1;
        HashMap<String, Integer> hm_qryStems = new HashMap<String, Integer>();
        for (int i = 0; i < queryStems.length; i++) {
            if (!hm_qryStems.containsKey(queryStems[i])) {
                hm_qryStems.put(queryStems[i], 1);
            } else {
                hm_qryStems.put(queryStems[i], hm_qryStems.get(queryStems[i])+1);
            }
        }
        for (int i = 1; i < tm.stemsLength(); i++) {
            String stem = tm.stemString(i);
            if (hm_qryStems.containsKey(stem)) {
                int tf = tm.stemFreq(i);
                int df = tm.stemDf(i);
                long N = Idx.getNumDocs();
                float avg_docLen = Idx.getSumOfFieldLengths(field) / (float) Idx.getDocCount(field);
                int docLen = Idx.getFieldLength(field, docid);
                score += Math.max(0, Math.log((N - df + 0.5) / (df + 0.5))) *
                        tf / (tf + r.getK_1() * (1-r.getb()+ r.getb()*(docLen/avg_docLen))) *
                        (r.getK_3()+1) * hm_qryStems.get(stem) / (r.getK_3() + hm_qryStems.get(stem));;
            }
        }
        return score;
    }

    private double getIndriScore (String[] queryStems, int docid, String field) throws IOException {
        RetrievalModelIndri r = new RetrievalModelIndri();
        double score = 1.0;
        int queryLen = queryStems.length;
        TermVector tm = new TermVector(docid, field);
        if (tm.stemsLength() <= 1) return -1;
        HashMap<String, Integer> hm_qryStems = new HashMap<String, Integer>();
        for (int i = 0; i < queryStems.length; i++) {
            if (!hm_qryStems.containsKey(queryStems[i])) {
                hm_qryStems.put(queryStems[i], 1);
            } else {
                hm_qryStems.put(queryStems[i], hm_qryStems.get(queryStems[i])+1);
            }
        }
        boolean flag = false;
        HashMap<String, Integer> overLapStems = new HashMap<String, Integer>();
        for (int i = 1; i < tm.stemsLength(); i++) {
            String stem = tm.stemString(i);
            if (hm_qryStems.containsKey(stem)) {
                int tf = tm.stemFreq(i);
                long ctf = tm.totalStemFreq(i);
                long collectLen = Idx.getSumOfFieldLengths(field);
                int docLen = Idx.getFieldLength(field, docid);
                score *= (1 - r.getlambda()) * (tf + r.getmu() * ctf / collectLen) / (docLen + r.getmu())
                        + (r.getlambda() * ctf / collectLen);
                overLapStems.put(stem, 0);
                flag = true;
            }
        }
        if (flag) {
            for (String queryStem : hm_qryStems.keySet()) {
                if (!overLapStems.containsKey(queryStem)) {
                    Term queryTerm = new Term(field,queryStem);
                    long ctf = Idx.INDEXREADER.totalTermFreq(queryTerm);
                    long collectLen = Idx.getSumOfFieldLengths(field);
                    int docLen = Idx.getFieldLength(field, docid);
                    score *= (1 - r.getlambda()) * (0 + r.getmu() * ctf / collectLen) / (docLen + r.getmu())
                            + (r.getlambda() * ctf / collectLen);
                }
            }
            score = Math.pow(score, 1.0/queryLen);
        } else {
            score = 0;
        }
        return score;
    }

    private double getOverlapScore (String[] queryStems, int docid, String field) throws IOException {
        int numOverlaps = 0;
        int queryLen = queryStems.length;
        TermVector tm = new TermVector(docid, field);
        if (tm.stemsLength() <= 1) return -1;
        HashMap<String, Integer> hm_qryStems = new HashMap<String, Integer>();
        for (int i = 0; i < queryStems.length; i++) {
            if (!hm_qryStems.containsKey(queryStems[i])) {
                hm_qryStems.put(queryStems[i], 1);
            } else {
                hm_qryStems.put(queryStems[i], hm_qryStems.get(queryStems[i])+1);
            }
        }
        for (int i = 1; i < tm.stemsLength(); i++) {
            String stem = tm.stemString(i);
            if (hm_qryStems.containsKey(stem)) {
                numOverlaps += hm_qryStems.get(stem);
            }
        }
        return numOverlaps/(double) queryLen;
    }

    private double urlDepth (String rawUrl) {
        double count = 0;
        for (int i = 0; i < rawUrl.length(); i++) {
            if (rawUrl.charAt(i) == '/') {
                count++;
            }
        }
        return count;
    }

    private double wikiScore (String rawUrl) {
        if (rawUrl.contains("wikipedia.org")) return 1;
        else return 0;
    }

    private double getAvgDFScore (int docid) throws IOException {
        TermVector tm = new TermVector(docid, "title");
        double avgDF = 0.0;
        double curDF;
        double len = 0.0;
        for (int i = 1; i < tm.stemsLength(); i++) {
            int tf = tm.stemFreq(i);
            curDF = (double) tm.stemDf(i);
            avgDF = avgDF == 0? 0: avgDF/(1+tf/len);
            avgDF = avgDF+ curDF/(tf+len);
            len += tf;
        }
        return avgDF;
    }

    private double getSequenOverlapScore (String[] queryStems, int docid) throws IOException {
        double numOverlaps = 0;
        int window = 8;
        int queryLen = queryStems.length;
        TermVector tm = new TermVector(docid, "body");
        if (tm.stemsLength() <= 1) return -1;
        HashMap<String, Integer> hm_qryStems = new HashMap<String, Integer>();
        for (int i = 0; i < queryStems.length; i++) {
            if (!hm_qryStems.containsKey(queryStems[i])) {
                hm_qryStems.put(queryStems[i], 1);
            } else {
                hm_qryStems.put(queryStems[i], hm_qryStems.get(queryStems[i])+1);
            }
        }
        int i = 0; int j = 1;
        while (i < (tm.positionsLength()-window)) {
            int index = tm.stemAt(i);
            String stem = tm.stemString(index);
            if (hm_qryStems.containsKey(stem)) {
                for (j = 1; j <= window; j++) {
                    if (hm_qryStems.containsKey(tm.stemString(tm.stemAt(i+j)))) {
                        numOverlaps++;
                        break;
                    }
                }
                i = i+j+1;
            } else {
                i++;
            }
        }
        return numOverlaps;
    }

    /**
     * For test
     * */
    public static void main(String[] args) throws IOException {
//        FeatureVec v = new FeatureVec(1, "sfagas");
//        v.addElement(new Integer(1));
//        v.addElement(new Integer(2));
//        v.addElement(new Integer(3));
//        v.addElement(new Integer(4));
//        FeatureVec v2 = new FeatureVec(v);
//        for (int i = 0; i < v.size(); i++) {
//            System.out.println(v2.docID);
//            System.out.println(v2.get(i));
//        }
    }

}

