package pi2schema.crypto.materials;

public class DecryptingMaterialNotFoundException extends RuntimeException {

    private final String missingSubject;

    public DecryptingMaterialNotFoundException(String missingSubject) {
        this.missingSubject = missingSubject;
    }

    @Override
    public String getMessage() {
        return String.format("Decrypting materials for the subject %s was not found", missingSubject);
    }
}
