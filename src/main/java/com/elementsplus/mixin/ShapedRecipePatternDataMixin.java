package com.elementsplus.mixin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Function;

@Mixin(ShapedRecipePattern.Data.class)
public class ShapedRecipePatternDataMixin {

    @Redirect(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/serialization/Codec;comapFlatMap(Ljava/util/function/Function;Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;"
            )
    )
    private static Codec<List<String>> redirectComapFlatMap(
            Codec<List<String>> codec,
            Function<List<String>, DataResult<?>> originalFlatMapper,
            Function<List<String>, List<String>> originalToMapper) {

        return codec.comapFlatMap(list -> {
            if (list.size() > 5) {
                return DataResult.error(() -> "Invalid pattern: too many rows, 5 is maximum");
            }

            if (list.isEmpty()) {
                return DataResult.error(() -> "Invalid pattern: empty pattern not allowed");
            }

            int i = list.getFirst().length();

            for (String string : list) {
                if (string.length() > 5) {
                    return DataResult.error(() -> "Invalid pattern: too many columns, 5 is maximum");
                }

                if (i != string.length()) {
                    return DataResult.error(() -> "Invalid pattern: each row must be the same width");
                }
            }

            return DataResult.success(list);
        }, originalToMapper);
    }
}