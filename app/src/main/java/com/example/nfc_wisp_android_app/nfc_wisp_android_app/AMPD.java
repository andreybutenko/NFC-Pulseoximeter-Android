package com.example.nfc_wisp_android_app.nfc_wisp_android_app;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class AMPD {
    private float[] X;
    private int Lambda_max;
    private int Lambda_min;
    private static final float alpha = 1;
    public AMPD(float[] x) {
        this.X = new float[x.length + 1];
        for(int i = 1; i <= x.length; i++) {
            this.X[i] = x[i - 1];
        }
        /*StdStats stat = new StdStats(x);
        float mean = (float) stat.getMean();
        float stddev = (float) stat.getStddev();
        float threshold = mean - (float) 0.2 * stddev;

        for(int i = 1; i <= x.length; i++) {
            X[i] -= threshold;
        }*/
    }

    public int[] getMaxima() {
        return getPeak(this.X);
    }

    public int[] getMinima() {

        return getValley(this.X);
    }

    private int[] getPeak(float[] x) {
        int N = x.length - 1;
        int L = N / 2 + N % 2 - 1;
        float[][] M = new float[L + 1][N + 1];
        Random rand = new Random();

        //Log.d("Debug", "Step 1");
        for(int k = 1; k <= L; k++) {
            for(int i = 1; i <= k; i++) {
                M[k][i] = rand.nextFloat() + alpha;
            }
            for(int i = k + 1; i <= N - k/* + 1*/; i++) {
                if((x[i/* - 1*/] > x[i - k/* - 1*/]) && (x[i/* - 1*/] > x[i + k/* - 1*/])) {
                    M[k][i] = 0;
                } else {
                    M[k][i] = rand.nextFloat() + alpha;
                }
            }
            for(int i = N - k + 1; i <= N; i++) {
                M[k][i] = rand.nextFloat() + alpha;
            }
        }

        //Log.d("Debug", "Step 2");

        float[] gamma = new float[L + 1];
        float min = Float.MAX_VALUE;
        int lambda = L + 1;
        for(int k = 1; k <= L; k++) {
            for(int i = 1; i <= N; i++) {
                gamma[k] += M[k][i];
            }
            if(gamma[k] <= min) {
                min = gamma[k];
                lambda = k;
            }
        }
        Lambda_max = lambda;
        Log.d("Debug", "lambda = " + Integer.toString(lambda));
        float[][] Mr = new float[lambda + 1][N + 1];


        for(int k = 1; k <= lambda; k++) {
            for(int i = 1; i <= N; i++) {
                Mr[k][i] = M[k][i];
            }
        }

        //Log.d("Debug", "Step 3");

        float[] sigma = new float[N + 1];
        for(int i = 1; i <= N; i++) {
            /*float sum = 0;
            for(int k = 1; k <= lambda; k++) {
                sum += Mr[k][i]; //
            }

            sum /= lambda;
            float local_sigma = 0;
            for(int k = 1; k <= lambda; k++) {
                float diff = Mr[k][i] - sum;
                local_sigma += Math.abs(diff);//Math.sqrt(diff * diff);
            }
            sigma[i] = local_sigma / (lambda - 1);*/
            float[] col = new float[lambda];
            for(int j = 0; j < lambda; j++) {
                col[j] = Mr[j + 1][i];
            }
            StdStats stat = new StdStats(col);
            sigma[i] = (float) stat.getStddev();
        }
        Log.d("Debug", "sigma_max = " + Arrays.toString(sigma));
        //Log.d("Debug", "Step 4");
        ArrayList<Integer> p = new ArrayList<Integer>();
        for(int i = 1; i <= N; i++) {
            if(sigma[i] == 0) {
                p.add(i - 1);
            }
        }
        int[] P = new int[p.size()];
        for(int i = 0; i < p.size(); i++) {
            P[i] = p.get(i);
        }
        return P;
    }

    private int[] getValley(float[] x) {
        int N = x.length - 1;
        int L = N / 2 + N % 2 - 1;
        float[][] M = new float[L + 1][N + 1];
        Random rand = new Random();

        //Log.d("Debug", "Step 1");
        for(int k = 1; k <= L; k++) {
            for(int i = 1; i <= k; i++) {
                M[k][i] = rand.nextFloat() + alpha;
            }
            for(int i = k + 1; i <= N - k/* + 1*/; i++) {
                if((x[i/* - 1*/] < x[i - k/* - 1*/]) && (x[i/* - 1*/] < x[i + k/* - 1*/])) {
                    M[k][i] = 0;
                } else {
                    M[k][i] = rand.nextFloat() + alpha;
                }
            }
            for(int i = N - k + 1; i <= N; i++) {
                M[k][i] = rand.nextFloat() + alpha;
            }
        }

        //Log.d("Debug", "Step 2");

        float[] gamma = new float[L + 1];
        float min = Float.MAX_VALUE;
        int lambda = L + 1;
        for(int k = 1; k <= L; k++) {
            for(int i = 1; i <= N; i++) {
                gamma[k] += M[k][i];
            }
            if(gamma[k] <= min) {
                min = gamma[k];
                lambda = k;
            }
        }
        Lambda_min = lambda;
        Log.d("Debug", "lambda = " + Integer.toString(lambda));

        float[][] Mr = new float[lambda + 1][N + 1];


        for(int k = 1; k <= lambda; k++) {
            for(int i = 1; i <= N; i++) {
                Mr[k][i] = M[k][i];
            }
        }

        //Log.d("Debug", "Step 3");

        float[] sigma = new float[N + 1];
        for(int i = 1; i <= N; i++) {
            /*float sum = 0;
            for(int k = 1; k <= lambda; k++) {
                sum += Mr[k][i]; //
            }

            sum /= lambda;
            float local_sigma = 0;
            for(int k = 1; k <= lambda; k++) {
                float diff = Mr[k][i] - sum;
                local_sigma += Math.sqrt(diff * diff);
            }
            sigma[i] = local_sigma / (lambda - 1);*/

            float[] col = new float[lambda];
            for(int j = 0; j < lambda; j++) {
                col[j] = Mr[j + 1][i];
            }
            StdStats stat = new StdStats(col);
            sigma[i] = (float) stat.getStddev();
        }

        Log.d("Debug", "sigma_min = " + Arrays.toString(sigma));
        //Log.d("Debug", "Step 4");
        ArrayList<Integer> p = new ArrayList<Integer>();
        for(int i = 1; i <= N; i++) {
            if(sigma[i] == 0) {
                p.add(i - 1);
            }
        }
        int[] P = new int[p.size()];
        for(int i = 0; i < p.size(); i++) {
            P[i] = p.get(i);
        }
        return P;
    }

    public int getLambda_max() {
        return Lambda_max;
    }

    public int getLambda_min() {
        return Lambda_min;
    }
}