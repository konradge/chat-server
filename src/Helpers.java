import java.io.BufferedWriter;
import java.io.FileWriter;

public class Helpers {
    public static void writeFile(String fileName, String text) throws Exception {

        BufferedWriter writer = new BufferedWriter(
                new FileWriter(fileName, true)
        );
        writer.newLine();
        writer.write(text);
        writer.close();
    }
}
