package fax.play.config;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;

import fax.play.model.AnyContainer;
import fax.play.model.AnyContainerSchemaImpl;
import org.infinispan.Cache;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfigurationBuilder;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.globalstate.ConfigurationStorage;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.protostream.SerializationContext;

public class Config implements Closeable {

    public static final String CACHE_NAME = "any-cache";
    private DefaultCacheManager cacheManager;
    private final ProtoStreamMarshaller protoMarshaller = new ProtoStreamMarshaller();
    private Configuration cacheConfiguration;

    public Config() {

        GlobalConfigurationBuilder global = new GlobalConfigurationBuilder();
        global.serialization()
                .marshaller(protoMarshaller)
                .addContextInitializer(new AnyContainerSchemaImpl());

        global.globalState()
                .enable()
                .persistentLocation("global/state")
                .configurationStorage(ConfigurationStorage.OVERLAY);

        cacheManager = new DefaultCacheManager(global.build());

        // Create a distributed cache with synchronous replication.
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.persistence().passivation(false)
                .addSoftIndexFileStore()
                .shared(false)
                .dataLocation("cache/data")
                .indexLocation("cache/index");

        builder.unsafe().unreliableReturnValues(true);

        ClusteringConfigurationBuilder clusteringConfigurationBuilder = builder.clustering()
                .cacheMode(CacheMode.LOCAL);

        clusteringConfigurationBuilder.encoding().mediaType("application/x-protostream");

        cacheConfiguration = builder.build();
    }

    public Cache<String, AnyContainer> recreateCache() {
        cacheManager.administration().removeCache(CACHE_NAME);
        return cacheManager.administration().createCache(CACHE_NAME, cacheConfiguration); // programmatically created cacheConfiguration rather than from any-cache.yaml
    }

    public SerializationContext getSerializationContext() {
        return protoMarshaller.getSerializationContext();
    }

    @Override
    public void close() {
        try {
            cacheManager.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
