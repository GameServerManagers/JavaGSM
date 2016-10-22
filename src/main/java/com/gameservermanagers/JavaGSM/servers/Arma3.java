package com.gameservermanagers.JavaGSM.servers;

import com.gameservermanagers.JavaGSM.ServerInstaller;
import com.gameservermanagers.JavaGSM.util.SteamcmdUtil;

import java.io.File;

/**
 * Created by Sigalita on 22-Oct-16.
 */
public class Arma3 implements ServerInstaller {

    public static void install(File destination) {
        boolean installedSuccessfully = SteamcmdUtil.installApp("anonymous", destination, "233780");
        System.out.println(installedSuccessfully
                ? "Finished installing Arma 3 server. Start it with the -s flag."
                : "Failed installing Arma 3. See above for errors generated by SteamCMD.");
        System.out.println();
    }

}