package pi2schema.crypto.providers.inmemorykms;

import org.jetbrains.annotations.NotNull;
import pi2schema.crypto.materials.SymmetricMaterial;
import pi2schema.crypto.providers.DecryptingMaterialsProvider;
import pi2schema.crypto.providers.EncryptingMaterialsProvider;
import pi2schema.crypto.support.KeyGen;

import javax.crypto.KeyGenerator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Development focused in memory key management.
 * Should not be considered to something else than tests as the keys are not accessible between the nodes.
 */
public class InMemoryKms implements EncryptingMaterialsProvider, DecryptingMaterialsProvider {

    private final KeyGenerator keyGenerator;

    private final Map<String, SymmetricMaterial> keyStore = new HashMap<>();

    public InMemoryKms() {
        this(KeyGen.aes256());
    }

    public InMemoryKms(KeyGenerator keyGenerator) {
        this.keyGenerator = keyGenerator;
    }

    @Override
    public CompletableFuture<SymmetricMaterial> encryptionKeysFor(@NotNull String subjectId) {
        return CompletableFuture.completedFuture(
                keyStore.computeIfAbsent(subjectId, missingKey ->
                        new SymmetricMaterial(keyGenerator.generateKey())));
    }

    @Override
    public CompletableFuture<SymmetricMaterial> decryptionKeysFor(@NotNull String subjectId) {
        return CompletableFuture.completedFuture(keyStore.get(subjectId));
    }

    @Override
    public void close() { }
}
