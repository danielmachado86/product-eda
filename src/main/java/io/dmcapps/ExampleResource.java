package io.dmcapps;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.protobuf.KafkaProtobufSerde;
import io.dmcapps.proto.Catalog;

@ApplicationScoped
public class ExampleResource {

    private static final Logger LOGGER = Logger.getLogger(ExampleResource.class);

    static final String PRODUCTS_STORE = "products-store";

    private static final String PRODUCTS_TOPIC = "products";
    private static final String VALIDATED_PRODUCTS_TOPIC = "validated_products";

    @ConfigProperty(name = "quarkus.kafka-streams.schema-registry-url")
    private String schemaRegistryURL;

    @Produces
    public Topology buildTopology() {

        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, Catalog.Product> inputProducts = builder
                .stream(PRODUCTS_TOPIC, Consumed.with(Serdes.String(), productSerde())).map((key, value) -> {
                    LOGGER.info(value);
                return new KeyValue<>(key, value);
            });
        
        KStream<String, Catalog.Product> validatedProducts = inputProducts
            .map((key, value) ->{
                LOGGER.info(value);
                return new KeyValue<>(key, value);

            });
        validatedProducts.to(VALIDATED_PRODUCTS_TOPIC, Produced.with(Serdes.String(), productSerde()));

        return builder.build();
    }

    private static KafkaProtobufSerde<Catalog.Product> productSerde() {

        KafkaProtobufSerde<Catalog.Product> serde = new KafkaProtobufSerde<>(Catalog.Product.class);

        Map<String, Object> serdeConfig = new HashMap<>();
        serdeConfig.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
        serde.configure(serdeConfig, false);
        return serde;
      }
}