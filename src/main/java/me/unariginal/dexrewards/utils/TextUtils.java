package me.unariginal.dexrewards.utils;

import me.unariginal.dexrewards.DexRewards;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.text.Text;

public class TextUtils {
    public static Text deserialize(String text) {
        Component component = MiniMessage.miniMessage().deserialize(text);
        return DexRewards.INSTANCE.audience().toNative(component);
    }
}
