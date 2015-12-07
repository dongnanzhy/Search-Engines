/**
 * Created by dongnanzhy on 10/2/15.
 */
/**
 *  An object that stores parameters for the Indri Model
 *  retrieval model (there are none) and indicates to the query
 *  operators how the query should be evaluated.
 */
public class RetrievalModelIndri extends RetrievalModel {

    private float mu = 0;
    private float lambda = 0;

    public RetrievalModelIndri() {
        this.mu = 2500;
        this.lambda = (float)0.4;
    }
    public RetrievalModelIndri(float mu, float lambda) throws IllegalArgumentException  {
        if (mu < 0) {
            throw new IllegalArgumentException
                    ("Parameter mu should larger than 0");
        }
        if (lambda < 0 || lambda > 1) {
            throw new IllegalArgumentException
                    ("Parameter lambda should between 0 and 1");
        }
        this.mu = mu;
        this.lambda = lambda;
    }
    public String defaultQrySopName () {
        return new String ("#AND");
    }

    public float getmu() {return this.mu;}
    public float getlambda() {return this.lambda;}

}
