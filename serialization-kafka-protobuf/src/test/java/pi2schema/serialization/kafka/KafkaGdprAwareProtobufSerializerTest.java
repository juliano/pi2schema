package pi2schema.serialization.kafka;

import com.acme.FarmerRegisteredEventFixture;
import com.acme.FarmerRegisteredEventOuterClass.FarmerRegisteredEvent;
import com.acme.FruitFixture;
import com.acme.FruitOuterClass.Fruit;
import com.acme.TimestampFixture;
import com.google.protobuf.Message;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializerConfig;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import pi2schema.crypto.EncryptedData;
import pi2schema.crypto.Encryptor;

import javax.crypto.spec.IvParameterSpec;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KafkaGdprAwareProtobufSerializerTest {

    private final Map<String, Object> configs;

    private final String topic = "test";
    private final MockSchemaRegistryClient schemaRegistry = new MockSchemaRegistryClient();

    private final byte[] encrypted = "mockEncryption".getBytes();
    private final Encryptor encryptorMock = (subjectId, data) ->
            CompletableFuture.completedFuture(
                    new EncryptedData(encrypted, "AES/CBC/PKCS5Padding", new IvParameterSpec(new byte[0]))
            );

    KafkaGdprAwareProtobufSerializerTest() {
        var initial = new HashMap<String, Object>();
        initial.put(KafkaProtobufSerializerConfig.AUTO_REGISTER_SCHEMAS, true);
        initial.put(KafkaProtobufSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "bogus");
        this.configs = Collections.unmodifiableMap(initial);
    }

    @Test
    void shouldSupportNullRecordReturningNullData() {
        var deserializer = pi2schemaProtobufSerializerFor(Fruit.class);

        assertNull(deserializer.serialize(topic, null));
        assertNull(deserializer.serialize(topic, new RecordHeaders(), null));
    }

    @Test
    void shouldBeCompatibleWithPlainProtobufDeserializer() {
        var preferredMelon = FruitFixture.waterMelon().build();
        var serializer = pi2schemaProtobufSerializerFor(Fruit.class);

        var deserializer = new KafkaProtobufDeserializer<>(
                schemaRegistry,
                configs,
                Fruit.class
        );

        var serialized = serializer.serialize(topic, preferredMelon);
        assertEquals(preferredMelon, deserializer.deserialize(topic, serialized));
    }

    @Test
    void shouldEncryptOneOfFieldsContainingEncryptedPersonalData() {
        //given
        var uuid = UUID.randomUUID().toString();

        var original = FarmerRegisteredEventFixture.johnDoe()
                .setUuid(uuid)
                .setRegisteredAt(TimestampFixture.now())
                .build();

        var serializer = pi2schemaProtobufSerializerFor(FarmerRegisteredEvent.class);
        var deserializer = new KafkaProtobufDeserializer<>(
                schemaRegistry,
                configs,
                FarmerRegisteredEvent.class
        );

        //when
        byte[] data = serializer.serialize(topic, original);
        FarmerRegisteredEvent actual = deserializer.deserialize(topic, data);

        //then
        assertThat(actual.getUuid())
                .isEqualTo(uuid);

        assertThat(actual.getPersonalDataCase())
                .isEqualTo(FarmerRegisteredEvent.PersonalDataCase.ENCRYPTEDPERSONALDATA);

        assertThat(actual.getEncryptedPersonalData().getSubjectId())
                .isEqualTo(uuid);

        assertThat(actual.getEncryptedPersonalData().getData().toByteArray())
                .isEqualTo(encrypted);

        assertThat(actual.getEncryptedPersonalData().getPersonalDataFieldNumber())
                .isEqualTo(2);

    }

    private <T extends Message> KafkaGdprAwareProtobufSerializer<T> pi2schemaProtobufSerializerFor(Class<T> aClazz) {
        return new KafkaGdprAwareProtobufSerializer<>(
                encryptorMock,
                schemaRegistry,
                configs, aClazz);
    }
}