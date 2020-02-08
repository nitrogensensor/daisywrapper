package eu.nitrogensensor.daisy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Erstatning {
    public final String regexp;
    public final String erstatning;

    public Erstatning(String regexp, String erstatning) {
        this.regexp = regexp;
        this.erstatning = erstatning;
    }

    public static String udfør(String scriptIndhold, ArrayList<Erstatning> erstatningsliste) throws IOException {
        for (Erstatning e : erstatningsliste) {
            scriptIndhold = e.erstat(scriptIndhold);
        }
        return scriptIndhold;
    }

    String erstat(String scriptIndhold) throws IOException {
        // undgå replaceAll, da det kan være det erstattede indhold bliver erstattet igen
        //String scriptIndholdNy = scriptIndhold.replaceFirst(regexp, erstatning);


        Matcher matcher = Pattern.compile(regexp, Pattern.DOTALL).matcher(scriptIndhold);
        //String scriptIndholdNy = matcher.replaceFirst(erstatning);

        if (!matcher.find())  throw new IOException("Fik ikke erstattet "+ regexp +" med "+ erstatning);

        StringBuilder sb = new StringBuilder();
        matcher.appendReplacement(sb, erstatning);
        matcher.appendTail(sb);

        scriptIndhold = sb.toString();
        return scriptIndhold;
    }
}
