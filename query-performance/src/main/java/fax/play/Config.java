package fax.play;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.commons.configuration.StringConfiguration;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

public class Config {

   public static final String CACHE_NAME = "query-performance";

   private static final String CACHE_DEFINITION =
         "<local-cache name=\"" + CACHE_NAME + "\" statistics=\"true\">" +
               "    <encoding media-type=\"application/x-protostream\"/>" +
               "    <indexing enabled=\"true\" storage=\"local-heap\">" +
               "        <index-reader />" +
               "        <indexed-entities>" +
               "            <indexed-entity>Shape</indexed-entity>" +
               "        </indexed-entities>" +
               "    </indexing>" +
               "</local-cache>";

   private static Shape.ShapeSchema schema;

   public static RemoteCache<Object, Shape> setupAll() {
      schema = Shape.ShapeSchema.INSTANCE;

      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.addServer().host("127.0.0.1").port(ConfigurationProperties.DEFAULT_HOTROD_PORT)
            .security()
            .authentication()
            .username("user")
            .password("pass")
            .marshaller(ProtoStreamMarshaller.class)
            // Register proto schema && entity marshaller on client side
            .addContextInitializer(schema);

      RemoteCacheManager remoteCacheManager = new RemoteCacheManager(builder.build());

      // Register proto schema on server side
      RemoteCache<String, String> metadataCache = remoteCacheManager.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
      metadataCache.put(schema.getProtoFileName(), schema.getProtoFile());

      remoteCacheManager.administration().removeCache(CACHE_NAME);
      remoteCacheManager.administration().createCache(CACHE_NAME, new StringConfiguration(CACHE_DEFINITION));

      return remoteCacheManager.getCache(CACHE_NAME);
   }
}
