/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.qsystem.bsboard;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
}
