package pi2schema.crypto.providers;

import org.jetbrains.annotations.NotNull;
import pi2schema.crypto.materials.SymmetricMaterial;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

public interface DecryptingMaterialsProvider extends Closeable {

    CompletableFuture<SymmetricMaterial> decryptionKeysFor(@NotNull String subjectId);

    @Override
    default void close() { }
}
