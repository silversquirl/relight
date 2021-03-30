package vktec.relight;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.ColumnPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;

import static net.minecraft.command.argument.ColumnPosArgumentType.columnPos;
import static net.minecraft.command.argument.ColumnPosArgumentType.getColumnPos;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class RelightMod implements ModInitializer {
	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			dispatcher.register(literal("relight")
				.requires(source -> source.hasPermissionLevel(4))
				.then(argument("from", columnPos())
					.executes(ctx -> this.relightChunk(ctx.getSource(), getColumnPos(ctx, "from")))
					.then(argument("to", columnPos())
						.executes(ctx -> this.relightChunks(ctx.getSource(), getColumnPos(ctx, "from"), getColumnPos(ctx, "to"))))));
		});
	}

	private int relightChunk(ServerCommandSource source, ColumnPos pos) {
		return this.relightChunks(source, pos, pos);
	}
	private int relightChunks(ServerCommandSource source, ColumnPos from, ColumnPos to) {
		int x0 = Math.min(from.x, to.x) >> 4;
		int x1 = Math.max(from.x, to.x) >> 4;
		int z0 = Math.min(from.z, to.z) >> 4;
		int z1 = Math.max(from.z, to.z) >> 4;

		ServerWorld world = source.getWorld();
		ServerLightingProvider lightingProvider = world.getChunkManager().getLightingProvider();
		ChunkNibbleArray lightData = new ChunkNibbleArray();
		for (int z = z0; z <= z1; z++) {
			for (int x = x0; x <= x1; x++) {
				// Delete existing light data
				for (int y = 0; y < 16; y++) {
					lightingProvider.enqueueSectionData(LightType.BLOCK, ChunkSectionPos.from(x, y, z), lightData, false);
				}
				// Recalculate light data
				lightingProvider.light(world.getChunk(x, z), false);
			}
		}

		int count = (x1 - x0 + 1) * (z1 - z0 + 1);
		source.sendFeedback(new LiteralText(String.format("Relighting %d chunks", count)), true);

		return 1;
	}
}
