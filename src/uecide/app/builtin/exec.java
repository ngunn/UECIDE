package uecide.app.builtin;

import uecide.app.*;

public class exec {
    public static boolean main(Sketch sketch, String[] arg) {
        if(arg.length != 1) {
            sketch.error("Usage: __builtin_exec::<script key>");
            return false;
        }

        String key = arg[0];

        return sketch.executeScript(key);

    }
}