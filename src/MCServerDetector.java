import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.bind.util.ISO8601Utils;

import static java.nio.file.Files.readString;

public class MCServerDetector {




    public static boolean isMinecraftServer(String ip, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {


            socket.connect(new InetSocketAddress(ip, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());


            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream hs = new DataOutputStream(buf);

            writeVarInt(hs, 0x00);
            writeVarInt(hs, 763);
            writeString(hs, ip);
            hs.writeShort(port);
            writeVarInt(hs, 1);
            writeVarInt(out, buf.size());
            out.write(buf.toByteArray());
            out.writeByte(0x01);
            out.writeByte(0x00);
            readVarInt(in);
            int packetId = readVarInt(in);
            if (packetId != 0x00) return false;


            int jsonLength = readVarInt(in);
            byte[] jsonBytes = new byte[jsonLength];
            in.readFully(jsonBytes);

            String json = new String(jsonBytes, StandardCharsets.UTF_8);

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            String version = root.getAsJsonObject("version").get("name").getAsString();
            JsonObject players = root.getAsJsonObject("players").getAsJsonObject();
            int online = players.get("online").getAsInt();
            int max = players.get("max").getAsInt();

         // TODO PARA EL NICO DEL FUTURO: Icons en database

            String motd = "";
            JsonElement desc = root.get("description");
            if (desc != null) {
                if(desc.isJsonObject() && desc.getAsJsonObject().has("text")) {
                    motd = desc.getAsJsonObject().get("text").getAsString();
                }else {
                    motd = desc.getAsString();
                }
            }

            DataBaseClass.insertServer(

                    ip,
                    motd,
                    max,
                    online,
                    version

            );
            //saveJson(ip, json);
            return true;


        } catch (Exception e) {
            return false;
        }
    }

    public static void updateOnlineAllServers(){
        //TODO
    }




    private static void saveJson(String ip, String json) {
        try {
            Files.createDirectories(Path.of("json"));
            Files.writeString(
                    Path.of("json", ip  + ".json"),
                    json
            );
        } catch (Exception ignored) {
        }
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = in.readByte();
            result |= (read & 0x7F) << (7 * numRead++);
            if (numRead > 5) throw new IOException("VarInt grande");
        } while ((read & 0x80) != 0);
        return result;
    }

    private static void writeString(DataOutputStream out, String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }
}
