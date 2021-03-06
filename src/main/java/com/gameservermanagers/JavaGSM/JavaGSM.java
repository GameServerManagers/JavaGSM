package com.gameservermanagers.JavaGSM;

import com.gameservermanagers.JavaGSM.objects.ConfigMap;
import com.gameservermanagers.JavaGSM.util.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.commons.io.FileUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"WeakerAccess", "Duplicates"})
public class JavaGSM {

    public static final String version = "0.1.0";
    public static final Map<String, String> flagDefinitions = new LinkedHashMap<String, String>() {{
        put("-c  (--configure)", "Configure GSM or an existing server");
        put("-fu (--forceupdate)", "Force an update check cycle");
        put("-h  (--help)", "Displays this help text");
        put("-i  (--install)", "Install a new server");
        put("-s  (--start)", "Start a non-running existing server");
        put("-st (--stop)", "Stop a running existing server");
        put("-u  (--update)", "Update an existing server");
    }};

    public static final boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("win");
    public static final boolean isMac = System.getProperty("os.name").toLowerCase().startsWith("mac");
    public static final boolean isLinux = !isWindows && !isMac;

    public static final ConfigMap<String, Object> config = new ConfigMap<>();
    public static final File configFile = new File("gsm.json");
    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        header();
        System.out.println("v" + version + " dev @ScarszRawr & collective GameServerManagers team");
        System.out.println("https://github.com/GameServerManagers/JavaGSM");
        System.out.println();

        System.out.print("Loading config...");
        loadConfig();
        config.defaults = gson.fromJson(ResourceUtil.getResourceAsString("gsm-default.json"), LinkedTreeMap.class);
        System.out.println(" done");

        // check if last update check was over a day ago
        long diff = (long) (System.currentTimeMillis() - (double) config.get("lastUpdateCheck"));
        if (TimeUnit.MILLISECONDS.toDays(diff) > 1) forceUpdate();

        // if no arguments have been given show help
        if (args.length == 0) {
            showHelp(null);
            System.exit(0);
        }

        for (int i = 0; i < args.length; i++) {
            // only doing processing on flags
            if (!args[i].startsWith("-")) continue;

            // check if the next argument from args doesn't start with a -, if it doesn't, it should be treated as an argument for this flag
            String argument = null;
            if (args.length > i + 1 && !args[i + 1].startsWith("-")) argument = args[i + 1];

            switch (args[i]) {
                case "-c":
                case "--configure":
                    configure(argument);
                    break;
                case "-fu":
                case "--forceupdate":
                    forceUpdate();
                    break;
                case "-h":
                case "--help":
                    showHelp(argument);
                    break;
                case "-i":
                case "--install":
                    install(argument);
                    break;
                case "-s":
                case "--start":
                    start(argument);
                    break;
                case "-st":
                case "--stop":
                    stop(argument);
                    break;
                case "-u":
                case "--update":
                    update(argument);
                    break;
                default:
                    System.out.println("Unknown flag \"" + args[i] + (argument == null ? "" : " " + argument) + "\"");
                    break;
            }

            if (i != args.length - 1 || !isWindows) System.out.println();
        }
    }

    /**
     * Display the configuration management menu
     * @param argument Advanced automatic menu traversal, WIP
     */
    private static void configure(@Nullable String argument) {
        // TODO
    }

    /**
     * Forces an update check cycle
     */
    private static void forceUpdate() {
        // check for updates
        UpdateUtil.checkForUpdates();
    }

    /**
     * Display JavaGSM's help text
     * @param argument The flag to be elaborated on if not null
     */
    private static void showHelp(@Nullable String argument) {
        System.out.println("syntax: -flag [optional value] -flag [optional value] -flag [optional value] etc");
        System.out.println("hint: to install a new server use -i");
        System.out.println();

        // TODO: make this respond to the given argument
        // TODO: also appropriate the space before the (-argument)'s
        int maxSpace = 0;
        for (Map.Entry<String, String> definition : flagDefinitions.entrySet())
            if (maxSpace < definition.getKey().length()) maxSpace = definition.getKey().length();

        for (Map.Entry<String, String> entry : flagDefinitions.entrySet()) {
            System.out.print(entry.getKey());
            for (int i = 0; i < maxSpace - entry.getKey().length(); i++) System.out.print(" ");
            System.out.println(" | " + entry.getValue());
        }
    }

    /**
     * Install the specified server. If a null argument is given, the user will be prompted for the game to install.
     * @param argument The server class name to install
     */
    private static void install(@Nullable String argument) {
        String gameServerName;
        if (argument != null && argument.length() > 0) gameServerName = argument; else {
            // don't attempt to understand this
            Reflections reflections = new Reflections(new ConfigurationBuilder().setScanners(new SubTypesScanner(false), new ResourcesScanner()).setUrls(ClasspathHelper.forClassLoader(Arrays.asList(ClasspathHelper.contextClassLoader(), ClasspathHelper.staticClassLoader()).toArray(new ClassLoader[0]))).filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix("com.gameservermanagers.JavaGSM.servers"))));
            Set<Class<? extends ServerInstaller>> availableServerClasses = reflections.getSubTypesOf(ServerInstaller.class);

            List<String> choices = new LinkedList<>();
            for (Class<?> serverClass : availableServerClasses) choices.add(serverClass.getSimpleName());
            Collections.sort(choices);
            gameServerName = choices.get(UserInputUtil.questionList("Which server do you want to install", choices));
        }

        System.out.println("Installing " + gameServerName + " server...");
        System.out.println();

        boolean serversFolderAvailable = new File("servers").exists() || new File("servers").mkdir();
        if (!serversFolderAvailable) {
            System.out.print("An error occurred creating the servers directory, aborting installation");
            SleepUtil.printlnEllipsis();
            return;
        }

        Method installer;
        try {
            installer = Class.forName("com.gameservermanagers.JavaGSM.servers." + gameServerName).getDeclaredMethod("install", File.class);
        } catch (NoSuchMethodException e) {
            System.out.println("An error occurred reflecting the installer method: method \"install\" not found in com.gameservermanagers.JavaGSM.servers." + gameServerName);
            return;
        } catch (ClassNotFoundException e) {
            System.out.println("Invalid server \"" + gameServerName + "\"");
            return;
        }
        File destination = new File("servers/" + UserInputUtil.questionString("What should the server's main directory be in ./servers/"));

        if (destination.exists()) {
            System.out.print("An error occurred creating the destination folder " + destination.getAbsolutePath() + ": directory already exists. Aborting installation");
            SleepUtil.printlnEllipsis();
            return;
        }
        if (!destination.mkdir()) {
            System.out.print("An error occurred creating the destination folder " + destination.getAbsolutePath() + ": could not create directory. Aborting installation");
            SleepUtil.printlnEllipsis();
            return;
        }

        System.out.println();
        try {
            installer.invoke(null, destination);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            System.out.print("An unknown error occurred, please contact the developers with information about what you were doing");
            SleepUtil.printlnEllipsis();
        }
    }

    /**
     * Start the given server. If the argument is null, prompt the user for selecting a server to start.
     * @param argument The given server to start
     */
    private static void start(@Nullable String argument) {
        String gameServerName;
        if (argument != null && argument.length() > 0) gameServerName = argument; else {
            if (new File("servers").listFiles() != null) {
                List<String> choices = new LinkedList<>();
                for (File server : new File("servers").listFiles()) if (server.isDirectory() && new File(server, "gsm.json").exists()) choices.add(server.getPath().substring(8));
                Collections.sort(choices);
                gameServerName = choices.get(UserInputUtil.questionList("Which server do you want to start", choices));
            } else {
                System.out.println("No servers are installed. Try installing one with the -i flag.");
                return;
            }
        }
        File target = new File("servers/" + gameServerName);
        String game = (String) ConfigUtil.getConfigOptionFromFile(new File(target, "gsm.json"), "game");

        System.out.println("Starting server at \"" + target + "\" as game " + game + "...");
        System.out.println();

        Method starter;
        try {
            starter = Class.forName("com.gameservermanagers.JavaGSM.servers." + game).getDeclaredMethod("start", File.class);
        } catch (NoSuchMethodException e) {
            System.out.println("An error occurred reflecting the starter method: method \"start\" not found in com.gameservermanagers.JavaGSM.servers." + game);
            return;
        } catch (ClassNotFoundException e) {
            System.out.println("An error occurred reflecting the starter method: class not found com.gameservermanagers.JavaGSM.servers." + game);
            return;
        }

        try {
            starter.invoke(null, target);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            System.out.print("An unknown error occurred, please contact the developers with information about what you were doing");
            SleepUtil.printlnEllipsis();
        }
    }

    /**
     * Stop the given server. If the argument is null, prompt the user for selecting a server to stop.
     * @param argument The given server to stop
     */
    private static void stop(@Nullable String argument) {
        // TODO
    }

    /**
     * Update the given server. If the argument is null, prompt the user for selecting a server to update.
     * @param argument The given server to update
     */
    private static void update(@Nullable String argument) {
        String gameServerName;
        if (argument != null && argument.length() > 0) gameServerName = argument; else {
            if (new File("servers").listFiles() != null) {
                List<String> choices = new LinkedList<>();
                for (File server : new File("servers").listFiles()) if (server.isDirectory() && new File(server, "gsm.json").exists()) choices.add(server.getPath().substring(8));
                Collections.sort(choices);
                gameServerName = choices.get(UserInputUtil.questionList("Which server do you want to update", choices));
            } else {
                System.out.println("No servers are installed. Try installing one with the -i flag.");
                return;
            }
        }
        File target = new File("servers/" + gameServerName);
        String game = (String) ConfigUtil.getConfigOptionFromFile(new File(target, "gsm.json"), "game");

        System.out.println("Updating server at \"" + target + "\" as game " + game + "...");
        System.out.println();

        Method updater;
        try {
            updater = Class.forName("com.gameservermanagers.JavaGSM.servers." + game).getDeclaredMethod("update", File.class);
        } catch (NoSuchMethodException e) {
            System.out.println("An error occurred reflecting the updater method: method \"update\" not found in com.gameservermanagers.JavaGSM.servers." + game);
            return;
        } catch (ClassNotFoundException e) {
            System.out.println("An error occurred reflecting the updater method: class not found com.gameservermanagers.JavaGSM.servers." + game);
            return;
        }

        System.out.println();
        try {
            updater.invoke(null, target);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            System.out.print("An unknown error occurred, please contact the developers with information about what you were doing");
            SleepUtil.printlnEllipsis();
        }
    }

    //region Utilities
    //region Config
    private static void loadConfig() {
        if (!configFile.exists()) ResourceUtil.copyResourceToFile("gsm-default.json", configFile);

        try {
            config.clear();
            for (Map.Entry<String, Object> configOption : ((LinkedTreeMap<String, Object>) gson.fromJson(FileUtils.readFileToString(configFile, Charset.defaultCharset()), LinkedTreeMap.class)).entrySet())
                config.put(configOption.getKey(), configOption.getValue());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void saveConfig() {
        if (!configFile.exists()) {
            ResourceUtil.copyResourceToFile("gsm-default.json", configFile);
            return;
        }

        try {
            FileUtils.writeStringToFile(configFile, gson.toJson(config), Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //endregion
    // region Header
    private static void header() {
        // Looks weird due to escape characters, run to see actual ascii "Art"
        System.out.println("       _                   _____  _____ __  __ ");
        System.out.println("      | |                 / ____|/ ____|  \\/  |");
        System.out.println("      | | __ ___   ____ _| |  __| (___ | \\  / |");
        System.out.println("  _   | |/ _` \\ \\ / / _` | | |_ |\\___ \\| |\\/| |");
        System.out.println(" | |__| | (_| |\\ V / (_| | |__| |____) | |  | |");
        System.out.println("  \\____/ \\__,_| \\_/ \\__,_|\\_____|_____/|_|  |_|");
        System.out.println("                                               ");
    }
    //endregion
    //endregion

}
