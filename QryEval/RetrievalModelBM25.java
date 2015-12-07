/**
 * Created by dongnanzhy on 9/30/15.
 */
/**
 *  An object that stores parameters for the BM25
 *  retrieval model (there are none) and indicates to the query
 *  operators how the query should be evaluated.
 */
public class RetrievalModelBM25 extends RetrievalModel {

    private float k_1 = 0;
    private float b = 0;
    private float k_3 = 0;

    public RetrievalModelBM25() {
        this.k_1 = (float) 1.2;
        this.b = (float) 0.75;
        this.k_3 = 0;
    }
    public RetrievalModelBM25(float k_1, float b, float k_3) throws IllegalArgumentException  {
        if (k_1 < 0) {
            throw new IllegalArgumentException
                    ("Parameter k_1 should larger than 0");
        }
        if (b < 0 || b > 1) {
            throw new IllegalArgumentException
                    ("Parameter b should between 0 and 1");
        }
        if (k_3 < 0) {
            throw new IllegalArgumentException
                    ("Parameter k_3 should larger than 0");
        }
        this.k_1 = k_1;
        this.b = b;
        this.k_3 = k_3;
    }
    public String defaultQrySopName () {
        return new String ("#SUM");
    }

    public float getK_1() {return this.k_1;}
    public float getb() {return this.b;}
    public float getK_3() {return this.k_3;}
}