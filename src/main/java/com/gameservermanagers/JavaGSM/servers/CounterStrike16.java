package com.gameservermanagers.JavaGSM.servers;

import com.gameservermanagers.JavaGSM.ServerInstaller;
import com.gameservermanagers.JavaGSM.util.SteamcmdUtil;

import java.io.File;

@SuppressWarnings("unused")
public class CounterStrike16 implements ServerInstaller {

    public static void install(File destination) {
        boolean installedSuccessfully = SteamcmdUtil.installApp("anonymous", destination, "60");
        System.out.println(installedSuccessfully
                ? "Finished installing Counter-Strike 1.6 server. Start it with the -s flag."
                : "Failed installing Counter-Strike 1.6 server. See above for errors generated by SteamCMD.")
        ;
    }

}