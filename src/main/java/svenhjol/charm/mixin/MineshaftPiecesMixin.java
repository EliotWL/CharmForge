package svenhjol.charm.mixin;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.structure.MineshaftPieces;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.feature.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import svenhjol.charm.module.MineshaftImprovements;

import java.util.Random;

@Mixin(value = {
        MineshaftPieces.Corridor.class,
        MineshaftPieces.Room.class
})
public class MineshaftPiecesMixin {
    @Inject(
            method = "func_230383_a_(Lnet/minecraft/world/ISeedReader;Lnet/minecraft/world/gen/feature/structure/StructureManager;Lnet/minecraft/world/gen/ChunkGenerator;Ljava/util/Random;Lnet/minecraft/util/math/MutableBoundingBox;Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/util/math/BlockPos;)Z",
            at = @At("RETURN")
    )
    private void hookGenerate(ISeedReader world, StructureManager structure, ChunkGenerator gen, Random rand, MutableBoundingBox box, ChunkPos chunkPos, BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) // if a piece did generate
            MineshaftImprovements.generatePiece((StructurePiece) (Object) this, world, structure, gen, rand, box, chunkPos, blockPos);
    }
}

