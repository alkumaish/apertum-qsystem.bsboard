/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.qsystem.bsboard;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;

/**
 *
 * @author Evgeniy Egorov
 */
public class AddrProp {

    final private HashMap<String, String> addrs = new HashMap<>();
    final private HashMap<String, String> ids = new HashMap<>();

    public HashMap<String, String> getAddrs() {
        return addrs;
    }
    final static private File addrFile = new File("config/bs.adr");

    private AddrProp() {
        try (FileInputStream fis = new FileInputStream(addrFile); Scanner s = new Scanner(fis)) {
            while (s.hasNextLine()) {
                final String line = s.nextLine().trim();
                if (!line.startsWith("#")) {
                    final String[] ss = line.split("=");
                    addrs.put(ss[0], ss[1]);
                    ids.put(ss[1], ss[0]);
                    System.out.println(ss[0] + " " + ss[1]);
                }
            }
        } catch (IOException ex) {
            System.err.println(ex);
            throw new RuntimeException(ex);
        }

        File f = new File("config/bsPluginContent.properties");
        if (f.exists()) {
            final FileInputStream inStream;
            try {
                inStream = new FileInputStream(f);
            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            }
            final Properties settings = new Properties();
            try {
                settings.load(new InputStreamReader(inStream, "UTF-8"));
            } catch (IOException ex) {
                throw new RuntimeException("Cant read version. " + ex);
            }

            topSize = Integer.parseInt(settings.getProperty("top.size", "0"));
            topUrl = settings.getProperty("top.url");
            leftSize = Integer.parseInt(settings.getProperty("left.size", "0"));
            leftUrl = settings.getProperty("left.url");
            rightSize = Integer.parseInt(settings.getProperty("right.size", "0"));
            rightUrl = settings.getProperty("right.url");
            bottomSize = Integer.parseInt(settings.getProperty("bottom.size", "0"));
            bottomUrl = settings.getProperty("bottom.url");

        }
    }

    public static AddrProp getInstance() {
        return AddrPropHolder.INSTANCE;
    }

    private static class AddrPropHolder {

        private static final AddrProp INSTANCE = new AddrProp();
    }

    public String getId(String adr) {
        return ids.get(adr);
    }

    public String getAddr(String id) {
        return addrs.get(id);
    }

    public static void main(String[] ss) {
        String s = "[123|names]asd [123|name] asd [321|ext] asd asd [222|discription] asd [555|blink] asd[2222|blink]";
        System.out.println(s);
        System.out.println("");
        s = s.replaceAll("\\[\\d+\\|(name|discription|point|blink|ext)\\]", "#");
        System.out.println(s);
        /*
         ArrayList<String> allMatches = new ArrayList<>();
         Matcher m = Pattern.compile("\\[\\d+\\|(name|discription|point|blink|ext)\\]").matcher(s);
         while (m.find()) {
         allMatches.add(m.group());
         }
         int i = 0;
         for (String string : allMatches) {
         System.out.println(string);
         s = s.replaceAll(string.replace("[", "\\[").replace("]", "\\]").replace("|", "\\|"), "b_" + i++);
         }
         System.out.println(s);
         /*
         allMatches = new ArrayList<>();
         m = Pattern.compile("\\[\\d+\\|\\d+\\]").matcher(s);
         while (m.find()) {
         allMatches.add(m.group());
         }
         for (String string : allMatches) {
         System.out.println(string);
         s = s.replaceAll(string.replace("[", "\\[").replace("]", "\\]").replace("|", "\\|"), "A123");
         }
         System.out.println(s);
         /*
         System.out.println("addrs:");
         for (Long l : getInstance().addrs.keySet()) {
         System.out.println(l + "=" + getInstance().getAddr(l).getName());

         }
         */
    }

    int topSize = 0;
    String topUrl = "";
    int leftSize = 0;
    String leftUrl = "";
    int rightSize = 0;
    String rightUrl = "";
    int bottomSize = 0;
    String bottomUrl = "";

    public int getTopSize() {
        return topSize;
    }

    public String getTopUrl() {
        return topUrl;
    }

    public int getLeftSize() {
        return leftSize;
    }

    public String getLeftUrl() {
        return leftUrl;
    }

    public int getRightSize() {
        return rightSize;
    }

    public int getBottomSize() {
        return bottomSize;
    }

    public String getBottomUrl() {
        return bottomUrl;
    }

    public String getRightUrl() {
        return rightUrl;
    }
    

    

}
