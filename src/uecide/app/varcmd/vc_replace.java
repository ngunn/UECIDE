package uecide.app.varcmd;

import uecide.app.*;

public class vc_replace {
    public static String main(Sketch sketch, String args) {
        String[] bits = args.split(",");

        if(bits.length != 3) {
            return "Syntax error in replace - bad arg count";
        } else {
            return bits[0].replaceAll(bits[1], bits[2]);
        }
    }
}