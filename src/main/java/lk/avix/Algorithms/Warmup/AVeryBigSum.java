package lk.avix.Algorithms.Warmup;

import java.util.Scanner;

/**
 * @author Chanaka
 */
public class AVeryBigSum {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        long sum = 0;
        for (int i = 0; i < n; i++) {
            sum += sc.nextLong();
        }
        System.out.println(sum);
    }
}
