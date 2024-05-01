package com.yangline.catskin;

import com.google.common.collect.Iterables;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.skinsrestorer.bukkit.SkinsRestorer;
import net.skinsrestorer.shared.storage.SkinStorage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.mineskin.MineskinClient;
import org.mineskin.Model;
import org.mineskin.SkinOptions;
import org.mineskin.Visibility;
import org.mineskin.data.SkinCallback;
import org.mineskin.data.Texture;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Set;

public final class Bukkit extends JavaPlugin implements Listener {

    private MineskinClient skinClient;
    private SkinsRestorer skinsRestorer;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        skinClient = new MineskinClient();
        skinsRestorer = (SkinsRestorer) org.bukkit.Bukkit.getPluginManager().getPlugin("SkinsRestorer");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            this.reloadConfig();
            getLogger().info("[CatSkin] " + "Loading skin for player \"" + event.getPlayer().getName() + "\"...");
            String texturesUrl = "https://api.owospace.com/textures/";
            Skin skin = getSkin(event.getPlayer().getName());
            if (skin == null) {
                getLogger().warning("[CatSkin] " + "Failed load skin for \"" + event.getPlayer().getName() +
                        "\"! Please check the network!");
                return;
            }
            String urlString = texturesUrl + skin.getTexture();
            setSkin(event.getPlayer(), urlString, skin.getModel().equals("slim") ? Model.SLIM : Model.DEFAULT);
        });
    }


    public void setSkin(Player player, String urlString, Model model) {
        try {
            URL url = new URL(urlString);
            skinClient.generateUrl(url.toString(), SkinOptions.create(player.getName(), model, Visibility.PRIVATE), new SkinCallback() {
                @Override
                public void error(String errorMessage) {
                    getLogger().warning(errorMessage);
                }

                @Override
                public void exception(Exception exception) {
                    exception.printStackTrace();
                }

                @Override
                public void done(org.mineskin.data.Skin skin) {
                    SkinStorage skinStorage = skinsRestorer.getSkinStorage();
                    Texture texture = skin.data.texture;
                    Object property = skinStorage.createProperty(player.getName().toLowerCase(), texture.value, texture.signature);
                    skinStorage.setSkinData(player.getName().toLowerCase(), property);
                    skinsRestorer.getFactory().applySkin(player, skinStorage.getSkinData(player.getName().toLowerCase()));
                    getLogger().info("[CatSkin] " + "Loaded skin for \"" + player.getName() + "\"!");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Skin getSkin(String username) {
        try {
            String profileUrl = "https://api.owospace.com/" + username + ".json";
            URL url = new URL(profileUrl);
            InputStream inputStream = url.openStream();
            byte[] bytes = readInputStream(inputStream);
            int read = inputStream.read(bytes);
            JsonParser jsonParser = new JsonParser();
            JsonElement parse = jsonParser.parse(new String(bytes));
            JsonObject jsonObject = parse.getAsJsonObject();
            JsonObject skins = jsonObject.getAsJsonObject("skins");
            Set<Map.Entry<String, JsonElement>> entries = skins.entrySet();
            Map.Entry<String, JsonElement> first = Iterables.getFirst(entries, null);
            if (first == null)
                return null;
            JsonElement value = first.getValue();
            String key = first.getKey();
            return new Skin(value.getAsString(), key);
        } catch (Throwable ignore) {
            return null;
        }
    }

    public static byte[] readInputStream(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while ((len = inputStream.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        bos.close();
        return bos.toByteArray();
    }
}
