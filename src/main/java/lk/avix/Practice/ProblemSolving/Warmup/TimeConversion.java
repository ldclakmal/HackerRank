package lk.avix.Practice.ProblemSolving.Warmup;

import java.util.Scanner;

/**
 * @author Chanaka
 */
public class TimeConversion {

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        String time[] = in.nextLine().split(":");
        String h = time[0];
        String m = time[1];
        String s = time[2].substring(0, 2);
        String c = time[2].substring(2, 4);

        if (c.equals("AM")) {
            if (h.equals("12")) {
                System.out.println("00:" + m + ":" + s);
            } else {
                System.out.println(h + ":" + m + ":" + s);
            }
        } else if (c.equals("PM")) {
            if (h.equals("12")) {
                System.out.println("12:" + m + ":" + s);
            } else {
                System.out.println(Integer.parseInt(h) + 12 + ":" + m + ":" + s);
            }
        }
    }
}
