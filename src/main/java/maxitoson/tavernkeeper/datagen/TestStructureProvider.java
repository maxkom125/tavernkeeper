package maxitoson.tavernkeeper.datagen;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashCode;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Generates NBT structure files used by GameTests.
 * Run ./gradlew runData to regenerate after adding new structures here.
 */
public class TestStructureProvider implements DataProvider {
    private final PackOutput packOutput;

    public TestStructureProvider(PackOutput packOutput) {
        this.packOutput = packOutput;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Flat stone floor used as the base for most GameTests
                writeFlat(output, "gametest/flat_7x5x7", 7, 5, 7);
            } catch (IOException e) {
                throw new RuntimeException("Failed to generate test structure", e);
            }
        });
    }

    /**
     * Writes a structure NBT with a solid stone floor at y=0 and air above.
     * Blocks in the test are placed on top of this via GameTestHelper.setBlock().
     */
    private void writeFlat(CachedOutput output, String name, int sizeX, int sizeY, int sizeZ) throws IOException {
        CompoundTag root = new CompoundTag();
        root.putInt("DataVersion", 3955); // MC 1.21.1

        ListTag sizeTag = new ListTag();
        sizeTag.add(IntTag.valueOf(sizeX));
        sizeTag.add(IntTag.valueOf(sizeY));
        sizeTag.add(IntTag.valueOf(sizeZ));
        root.put("size", sizeTag);

        // palette[0] = stone
        ListTag palette = new ListTag();
        CompoundTag stone = new CompoundTag();
        stone.putString("Name", "minecraft:stone");
        palette.add(stone);
        root.put("palette", palette);

        // Stone floor at y=0, everything else is implicitly air
        ListTag blocks = new ListTag();
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                CompoundTag block = new CompoundTag();
                block.putInt("state", 0);
                ListTag pos = new ListTag();
                pos.add(IntTag.valueOf(x));
                pos.add(IntTag.valueOf(0));
                pos.add(IntTag.valueOf(z));
                block.put("pos", pos);
                blocks.add(block);
            }
        }
        root.put("blocks", blocks);
        root.put("entities", new ListTag());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        NbtIo.writeCompressed(root, baos);
        byte[] bytes = baos.toByteArray();

        Path path = packOutput.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve("tavernkeeper/structure/" + name + ".nbt");

        @SuppressWarnings("deprecation")
        HashCode hash = Hashing.sha1().hashBytes(bytes);
        output.writeIfNeeded(path, bytes, hash);
    }

    @Override
    public String getName() {
        return "TavernKeeper Test Structures";
    }
}
