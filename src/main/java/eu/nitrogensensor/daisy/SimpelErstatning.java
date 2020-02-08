package eu.nitrogensensor.daisy;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpelErstatning extends Erstatning {
    public SimpelErstatning(String søgestreng, String erstatning) {
        super(søgestreng, erstatning);
    }

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


    public String erstat(String scriptIndhold) throws IOException {

        if (søgestreng.startsWith("(") && søgestreng.endsWith("*)"))
        {
            String søgEfterDirektivStart = søgestreng.substring(0, søgestreng.length()-2);
            int start = scriptIndhold.indexOf(søgEfterDirektivStart);
            if (start==-1) throw new IOException("Start på direktiv ikke fundet: "+søgEfterDirektivStart);
            int slutparentes = findUdenforParenteser(')', scriptIndhold, start+1);
            if (slutparentes==-1) throw new IOException("Slut på direktiv ikke fundet: "+søgestreng);

            StringBuilder sb = new StringBuilder(scriptIndhold.length()+128);
            sb.append(scriptIndhold.substring(0, start));
            sb.append(erstatning);
            sb.append(scriptIndhold.substring(slutparentes+1));
            return sb.toString();
        }

        if (!scriptIndhold.contains(søgestreng)) throw new IOException("Søgestreng ikke fundet: "+søgestreng);
        String res = scriptIndhold.replace(søgestreng, erstatning);
        return res;
    }

}
