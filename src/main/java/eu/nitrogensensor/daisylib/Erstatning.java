package eu.nitrogensensor.daisylib;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Beskriver en erstatning af en tekst i en inputfil med en anden
 * Søgestrengene kan være simple søgeudtryk eller regex
 */
public class Erstatning {


    /**
     * Finder første position af en operator, f.eks +, -, * eller /.
     * Går uden om de operatorer, der er inde i en parentes.
     * Simplel løsning, der ikke tager højde for parenteser: udtryk.indexOf(tegn)
     */
    public static int findUdenforParenteser(char tegn, String tekst, int startPos)
    {
        int par = 0;
        for (int i = startPos; i<tekst.length(); i++)
        {
            char t = tekst.charAt(i);
            if (t == tegn && par==0) return i; // tegn fundet udenfor parenteser!
            else if (t == '(') par++;          // vi går ind i en parentes
            else if (t == ')') par--;          // vi går ud af en parentes
        }
        return -1; // tegn ikke fundet udenfor parenteser
    }

    public static String erstat(String scriptIndhold, String søgestreng, String erstatning) {
        // Prøv først super simpel erstatning
        int pos = scriptIndhold.indexOf(søgestreng);
        if (pos!=-1) {
            String res = scriptIndhold.replace(søgestreng, erstatning); // burde egnelig kun erstatte første forekomst
            return res;
        }

        // Simpel erstatning af f.eks. "(path *)"
        if (søgestreng.startsWith("(") && søgestreng.endsWith("*)"))
        {
            String søgEfterDirektivStart = søgestreng.substring(0, søgestreng.length()-2);
            int start = scriptIndhold.indexOf(søgEfterDirektivStart);
            if (start==-1) throw new IllegalArgumentException("Start på direktiv ikke fundet: "+søgEfterDirektivStart);
            int slutparentes = findUdenforParenteser(')', scriptIndhold, start+1);
            if (slutparentes==-1) throw new IllegalArgumentException("Slut på direktiv ikke fundet: "+søgestreng);

            StringBuilder sb = new StringBuilder(scriptIndhold.length()+128);
            sb.append(scriptIndhold.substring(0, start));
            sb.append(erstatning);
            sb.append(scriptIndhold.substring(slutparentes+1));
            return sb.toString();
        }


        // Regex-erstatning - f.eks. "\\(path .+?\\)"
        // Undgå replaceAll, da det kan være det erstattede indhold bliver erstattet igen
        //String scriptIndholdNy = scriptIndhold.replaceFirst(regexp, erstatning);
        //String scriptIndholdNy = matcher.replaceFirst(erstatning);
        Matcher matcher = Pattern.compile(søgestreng, Pattern.DOTALL).matcher(scriptIndhold);
        if (matcher.find()) {
            StringBuilder sb = new StringBuilder();
            matcher.appendReplacement(sb, erstatning);
            matcher.appendTail(sb);
            return sb.toString();
        }

        throw new IllegalArgumentException("Søgestreng ikke fundet: "+søgestreng);
    }
}