package com.gameservermanagers.JavaGSM.util;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"WeakerAccess", "unused"})
public class SteamcmdUtil {

    public static Map<String, String> errorResolutions = new HashMap<String, String>(){{
        put("Invalid platform", "This server does not support this OS; nothing we can do about it.");
    }};

    private static boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("win");
    private static boolean isMac = System.getProperty("os.name").toLowerCase().startsWith("mac");
    private static boolean isLinux = !isWindows && !isMac;

    private static String steamcmdArchive = "steamcmd" + (isWindows ? ".zip" : isMac ? "_osx.tar.gz" : "_linux.tar.gz");
    private static String steamcmdExtension = isWindows ? ".exe" : ".sh";
    private static String steamcmdExecutable = "steamcmd" + steamcmdExtension;
    private static String steamcmdCommand = "+login anonymous +force_install_dir {DESTINATION} +app_update {APP} validate +exit";
    private static String steamcmdUrl = "https://steamcdn-a.akamaihd.net/client/installer/" + steamcmdArchive;

    public static boolean check() {
        return check(true);
    }
    public static boolean check(boolean attemptInstall) {
        System.out.print("Checking if SteamCMD is installed...");

        boolean validSteamcmd = true;
        if (!new File("steamcmd").exists()) validSteamcmd = false;
        if (!new File("steamcmd/steamcmd" + steamcmdExtension).exists()) validSteamcmd = false;

        System.out.println(" " + (validSteamcmd ? "installed" : "not installed"));

        return validSteamcmd || attemptInstall && installSteamcmd();
    }

    public static boolean installSteamcmd() {
        System.out.println("Installing SteamCMD...");

        DownloadUtil.download(steamcmdUrl);
        DownloadUtil.unzip(new File(steamcmdArchive), new File("steamcmd"));
        new File(steamcmdArchive).delete();

        return check(false);
    }

    public static boolean installApp(File destination, String app) {
        try {
            SteamcmdUtil.check();
            System.out.println("\nInstalling app " + app + " to " + destination + "...");

            Process steamcmdProcess = Runtime.getRuntime().exec("steamcmd/" + steamcmdExecutable + " " + steamcmdCommand.replace("{DESTINATION}", destination.getAbsolutePath()).replace("{APP}", app));

            StreamGobbler errorGobbler = new StreamGobbler(steamcmdProcess.getErrorStream());
            StreamGobbler outputGobbler = new StreamGobbler(steamcmdProcess.getInputStream());
            errorGobbler.start();
            outputGobbler.start();
            steamcmdProcess.waitFor();

            System.out.println("Steam finished with exit code " + steamcmdProcess.exitValue() + "\n");
            
            System.out.print("Scanning output for errors...");
            List<String> errors = scanForErrors(outputGobbler.output);
            if (errors.size() != 0) {
                System.out.println(" " + errors.size() + " errors found:");
                for (String error : errors) {
                    System.out.println("- " + error);
                    System.out.println("  Resolution: " + getResolutionForError(error));
                }
                System.out.println();
                return false;
            } else {
                System.out.println(" none found");
                return true;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<String> scanForErrors(List<String> output) {
        return output.stream().filter(s -> s.startsWith("ERROR!")).collect(Collectors.toCollection(LinkedList::new));
    }

    public static String getResolutionForError(String error) {
        for (Map.Entry<String, String> entry : errorResolutions.entrySet())
            if (error.contains(entry.getKey())) return entry.getValue();
        return "Unknown. :(";
    }

}
