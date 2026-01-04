package net.retrolilly.lillyFetch;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LillyFetchCommand implements CommandExecutor {

    private static final String ANSI_REGEX = "\\u001B\\[[;\\d]*m|\\[\\d+[a-z]";
    private final LillyFetch plugin;

    public LillyFetchCommand(LillyFetch plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        String headerMessage = plugin.getConfig().getString("header-message", "&6[Fastfetch info :3]");
        Component headerComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(headerMessage);
        player.sendMessage(headerComponent);

        executeFetchAsync(player);

        return true;
    }

    private void executeFetchAsync(Player player) {
        new Thread(() -> {
            try {
                long cacheDuration = plugin.getConfig().getLong("cache-duration", 60);
                boolean showCacheIndicator = plugin.getConfig().getBoolean("show-cache-indicator", true);

                List<String> output = null;
                boolean fromCache = false;

                if (cacheDuration > 0) {
                    output = plugin.getCacheManager().get("fetch", cacheDuration);
                    if (output != null) {
                        fromCache = true;
                    }
                }

                if (output == null) {
                    output = executeFetchCommand();
                    if (cacheDuration > 0) {
                        plugin.getCacheManager().cache("fetch", output);
                    }
                }

                if (fromCache && showCacheIndicator) {
                    player.sendMessage(Component.text("(Cached result)", NamedTextColor.GRAY));
                }

                displayParsedOutput(player, output);
            } catch (Exception e) {
                player.sendMessage(Component.text("Error: " + e.getMessage(), NamedTextColor.RED));
                player.sendMessage(Component.text("Make sure fastfetch or neofetch is installed.", NamedTextColor.YELLOW));
            }
        }).start();
    }

    private void displayParsedOutput(Player player, List<String> output) {
        List<String> fields = plugin.getConfig().getStringList("simple.fields");
        Map<String, String> parsedData = parseFetchOutput(output);

        int displayed = 0;
        for (String field : fields) {
            String value = parsedData.get(field.toLowerCase());
            if (value != null && !value.isEmpty()) {
                String formattedField = capitalizeField(field);
                player.sendMessage(Component.text(formattedField + ": " + value));
                displayed++;
            }
        }

        if (displayed == 0) {
            player.sendMessage(Component.text("No data could be parsed from fetch output", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Check server console for details", NamedTextColor.GRAY));
        }
    }

    private Map<String, String> parseFetchOutput(List<String> output) {
        Map<String, String> data = new HashMap<>();
        Pattern pattern = Pattern.compile("^\\s*([^:]+)\\s*:\\s*(.+)$");

        for (String line : output) {
            String cleanedLine = line.replaceAll(ANSI_REGEX, "");
            Matcher matcher = pattern.matcher(cleanedLine);
            if (matcher.find()) {
                String key = matcher.group(1).trim().toLowerCase();
                String value = matcher.group(2).trim();
                data.put(key, value);
            }
        }

        return data;
    }

    private String capitalizeField(String field) {
        if (field == null || field.isEmpty()) {
            return field;
        }
        return field.substring(0, 1).toUpperCase() + field.substring(1);
    }

    private List<String> executeFetchCommand() throws Exception {
        String command = plugin.getConfig().getString("fetch-command", "fetch");
        List<String> configArgs = plugin.getConfig().getStringList("fetch-args");

        List<String> args = new ArrayList<>();
        args.add(command);
        args.addAll(configArgs);

        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            List<String> output = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.add(line);
                }
            }

            int exitCode = process.waitFor();
            process.destroy();

            if (exitCode == 0 && !output.isEmpty()) {
                return output;
            }

            throw new Exception("Command exited with code: " + exitCode);
        } catch (Exception e) {
            throw new Exception("Failed to execute '" + command + "': " + e.getMessage());
        }
    }
}
