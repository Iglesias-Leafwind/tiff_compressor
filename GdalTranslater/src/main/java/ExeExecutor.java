import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ExeExecutor {
    public static String exec(String cmd) {
        BufferedReader br;
        BufferedReader brError;
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            String line = null;
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            brError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((line = br.readLine()) != null || (line = brError.readLine()) != null) {
                System.out.println(line);
            }
            return null;
        } catch (IOException e) {
            return e.getMessage();
        }
    }
}
