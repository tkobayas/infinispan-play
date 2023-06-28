package fax.play.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Map;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.descriptors.GenericDescriptor;
import org.infinispan.protostream.types.protobuf.AnySchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import fax.play.config.Config;
import fax.play.model.AnyContainer;
import fax.play.model.Hotel;
import fax.play.model.House;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AnyContainerTest {

   private Config config;
   private RemoteCache<String, AnyContainer> cache;

   @BeforeAll
   public void beforeAll() throws Exception {
      config = new Config();
      cache = config.recreateCache();
   }

   @AfterAll
   public void afterAll() {
      if (config != null) {
         config.close();
      }
   }

   @Test
   public void test() throws Exception {
      SerializationContext serializationContext = config.getSerializationContext();
      Map<String, GenericDescriptor> genericDescriptors = serializationContext.getGenericDescriptors();
      assertThat(genericDescriptors).containsKey("google.protobuf.Any")
            .containsKey("fax.play.any.House")
            .containsKey("fax.play.any.House");

      Hotel hotel = new Hotel("Boutique Hotel", "lion street, 39, Sydney", 70, (byte) 5);
      House house = new House("221B Baker Street, London", "red", 13);
      ArrayList list = new ArrayList();
      list.add("hello");

      byte[] hotelBytes = ProtobufUtil.toByteArray(serializationContext, hotel);
      byte[] houseBytes = ProtobufUtil.toByteArray(serializationContext, house);
      byte[] listBytes = ProtobufUtil.toByteArray(serializationContext, list);

      AnySchema.Any anyHotel = new AnySchema.Any("fax.play.any.Hotel", hotelBytes);
      AnySchema.Any anyHouse = new AnySchema.Any("fax.play.any.House", houseBytes);
        AnySchema.Any anyList = new AnySchema.Any("java.util.ArrayList", listBytes);

      AnyContainer hotelContainer = new AnyContainer(anyHotel, 7, "other info");
      AnyContainer houseContainer = new AnyContainer(anyHouse, 9, "bla bla bla");
        AnyContainer listContainer = new AnyContainer(anyList, 11, "list of something");

      cache.put("hotel-1", hotelContainer);
      cache.put("house-1", houseContainer);
        cache.put("list-1", listContainer);

      hotelContainer = cache.get("hotel-1");
      houseContainer = cache.get("house-1");
        listContainer = cache.get("list-1");

      assertThat(hotelContainer.getCounter()).isEqualTo(7);
      assertThat(hotelContainer.getDescription()).isEqualTo("other info");
      assertThat(houseContainer.getCounter()).isEqualTo(9);
      assertThat(houseContainer.getDescription()).isEqualTo("bla bla bla");
        assertThat(listContainer.getCounter()).isEqualTo(11);
        assertThat(listContainer.getDescription()).isEqualTo("list of something");

      anyHotel = hotelContainer.getObject();
      anyHouse = houseContainer.getObject();
        anyList = listContainer.getObject();

      assertThat(anyHotel.getTypeUrl()).isEqualTo("fax.play.any.Hotel");
      assertThat(anyHouse.getTypeUrl()).isEqualTo("fax.play.any.House");
        assertThat(anyList.getTypeUrl()).isEqualTo("java.util.ArrayList");

      hotelBytes = anyHotel.getValue();
      houseBytes = anyHouse.getValue();
        listBytes = anyList.getValue();

      Hotel hotelLoad = ProtobufUtil.fromByteArray(serializationContext, hotelBytes, Hotel.class);
      House houseLoad = ProtobufUtil.fromByteArray(serializationContext, houseBytes, House.class);
        ArrayList listLoad = ProtobufUtil.fromByteArray(serializationContext, listBytes, ArrayList.class);

      assertThat(hotelLoad).isEqualTo(hotel);
      assertThat(houseLoad).isEqualTo(house);
        assertThat(listLoad).isEqualTo(list);
   }
}
