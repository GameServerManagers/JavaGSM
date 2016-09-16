package com.gameservermanagers.JavaGSM.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

public class StreamGobbler extends Thread {

    List<String> output = new LinkedList<>();
    boolean done = false;
    InputStream is;

    public StreamGobbler(InputStream is) {
        this.is = is;
    }

    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                output.add(line);
            }
            done = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}