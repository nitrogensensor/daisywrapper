package eu.nitrogensensor.daisy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Erstatning {
    public final String søgestreng;
    public final String erstatning;

    public Erstatning(String søgestreng, String erstatning) {
        this.søgestreng = søgestreng;
        this.erstatning = erstatning;
    }

    public static String udfør(String scriptIndhold, ArrayList<Erstatning> erstatningsliste) throws IOException {
        for (Erstatning e : erstatningsliste) {
            scriptIndhold = e.erstat(scriptIndhold);
        }
        return scriptIndhold;
    }

    public String erstat(String scriptIndhold) throws IOException {
        // undgå replaceAll, da det kan være det erstattede indhold bliver erstattet igen
        //String scriptIndholdNy = scriptIndhold.replaceFirst(regexp, erstatning);


        Matcher matcher = Pattern.compile(søgestreng, Pattern.DOTALL).matcher(scriptIndhold);
        //String scriptIndholdNy = matcher.replaceFirst(erstatning);

        if (!matcher.find())  throw new IOException("Fik ikke erstattet "+ søgestreng +" med "+ erstatning);

        StringBuilder sb = new StringBuilder();
        matcher.appendReplacement(sb, erstatning);
        matcher.appendTail(sb);

        scriptIndhold = sb.toString();
        return scriptIndhold;
    }
}
