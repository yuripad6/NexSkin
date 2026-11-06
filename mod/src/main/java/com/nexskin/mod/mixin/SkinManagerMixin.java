package com.nexskin.mod.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.nexskin.mod.SkinFetcher;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.texture.PlayerSkinProvider")
public class SkinManagerMixin {
    // Здесь перехват метода, который загружает текстуры.
    // Точный сигнатура зависит от версии MC.
}