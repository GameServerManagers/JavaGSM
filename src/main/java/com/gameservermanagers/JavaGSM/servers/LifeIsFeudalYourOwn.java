package com.gameservermanagers.JavaGSM.servers;

import com.gameservermanagers.JavaGSM.util.SteamcmdUtil;

import java.io.File;

@SuppressWarnings("unused")
public class LifeIsFeudalYourOwn {

    public static void install(File destination) {
        boolean installedSuccessfully = SteamcmdUtil.installApp("anonymous", destination, "320850");
        System.out.println(installedSuccessfully
                ? "Finished installing Life is Feudal: Your Own server. Start it with the -s flag."
                : "Failed installing Life is Feudal: Your Own server. See above for errors generated by SteamCMD.")
        ;
        System.out.println();
    }

}