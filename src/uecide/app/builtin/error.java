package uecide.app.builtin;

import uecide.app.*;

public class error implements BuiltinCommand {
    public boolean main(Sketch sketch, String[] arg) {
        StringBuilder sb = new StringBuilder();

        for(String s : arg) {
            sb.append(s);
            sb.append(" ");
        }

        sketch.error(sb.toString());
        return true;
    }
}
