package me.kanuunankuulaspluginhead.headHunter;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Base64;

public class ItemSerializer {

    public static String serialize(ItemStack item) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BukkitObjectOutputStream oos = new BukkitObjectOutputStream(out);
            oos.writeObject(item);
            oos.close();
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ItemStack deserialize(String base64) {
        try {
            byte[] data = Base64.getDecoder().decode(base64);
            ObjectInputStream ois = new BukkitObjectInputStream(new ByteArrayInputStream(data));
            ItemStack item = (ItemStack) ois.readObject();
            ois.close();
            return item;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
