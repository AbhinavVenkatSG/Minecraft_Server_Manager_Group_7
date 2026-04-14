package app.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class FakeMinecraftProcessMain {
    private FakeMinecraftProcessMain() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("[Server thread/INFO]: Starting fake Minecraft server");
        System.out.println("[Server thread/INFO]: Done (0.100s)! For help, type \"help\"");
        System.out.flush();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String command = line.trim();
                if (command.isEmpty()) {
                    continue;
                }
                if ("stop".equalsIgnoreCase(command)) {
                    System.out.println("[Server thread/INFO]: Stopping the server");
                    System.out.flush();
                    return;
                }
                if ("list".equalsIgnoreCase(command)) {
                    System.out.println("[Server thread/INFO]: There are 0 of a max of 20 players online:");
                    System.out.flush();
                    continue;
                }

                System.out.println("[Server thread/INFO]: Command: " + command);
                System.out.flush();
            }
        }
    }
}
