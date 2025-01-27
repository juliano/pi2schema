package pi2schema.schema.subject;

public class SubjectIdentifierNotFoundException extends RuntimeException {

    private final String strategyName;
    private final String fieldName;

    public SubjectIdentifierNotFoundException(Class<?> strategy, String fieldName) {
        this.strategyName = strategy.getName();
        this.fieldName = fieldName;
    }

    @Override
    public String getMessage() {
        return String.format("The strategy %s has not found any possible identifiers for the field %s while exact one is required",
                strategyName,
                fieldName);
    }
}
