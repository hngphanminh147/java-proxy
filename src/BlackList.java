import java.io.*;

public class BlackList {

    static public String[] list = null;
    static public int count = 0;
    static public boolean isOpen = false;

    public static void init() {

        try {
            list = new String[30];

            Reader r = new FileReader("blacklist.conf");

            BufferedReader rd = new BufferedReader(r);

            String inputLine = null;

            while((inputLine = rd.readLine()) != null) {
                list[count] = inputLine;
                count++;
            }

            r.close();
            rd.close();

            isOpen = true;

        } catch (IOException e) {
            isOpen = false;
        }
    }


    public static void test() {
        for(int i = 0; i < count; i++) {
            System.out.println(list[i]);
        }
    }

    public static boolean inList(String str) {
        boolean b = false;

        for(int i = 0; i < count; i++) {
            if(str.indexOf(list[i]) != -1) {
                b = true;
                break;
            }
        }

        return b;
    }

}
