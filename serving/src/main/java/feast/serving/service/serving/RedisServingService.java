// /*
//  * Copyright 2019 The Feast Authors
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  *     https://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */
//
// package feast.serving.service.serving;
//
// import com.google.protobuf.AbstractMessageLite;
// import com.google.protobuf.ByteString;
// import com.google.protobuf.InvalidProtocolBufferException;
// import com.google.protobuf.Timestamp;
// import feast.serving.ServingAPIProto.BatchFeaturesJob.GetDownloadUrlRequest;
// import feast.serving.ServingAPIProto.BatchFeaturesJob.GetDownloadUrlResponse;
// import feast.serving.ServingAPIProto.BatchFeaturesJob.GetStatusRequest;
// import feast.serving.ServingAPIProto.BatchFeaturesJob.GetStatusResponse;
// import feast.serving.ServingAPIProto.BatchFeaturesJob.GetUploadUrlRequest;
// import feast.serving.ServingAPIProto.BatchFeaturesJob.GetUploadUrlResponse;
// import feast.serving.ServingAPIProto.BatchFeaturesJob.SetUploadCompleteRequest;
// import feast.serving.ServingAPIProto.BatchFeaturesJob.SetUploadCompleteResponse;
// import feast.serving.ServingAPIProto.GetBatchFeaturesResponse;
// import feast.serving.ServingAPIProto.GetFeastServingTypeResponse;
// import feast.serving.ServingAPIProto.GetFeaturesRequest;
// import feast.serving.ServingAPIProto.GetFeaturesRequest.EntityDataSetRow;
// import feast.serving.ServingAPIProto.GetFeaturesRequest.FeatureSet;
// import feast.serving.ServingAPIProto.GetOnlineFeaturesResponse;
// import feast.serving.ServingAPIProto.GetOnlineFeaturesResponse.FeatureDataSet;
// import feast.serving.exception.FeatureRetrievalException;
// import feast.storage.RedisProto.RedisKey;
// import feast.types.FieldProto.Field;
// import feast.types.FeatureRowProto.FeatureRow;
// import feast.types.FeatureRowProto.FeatureRow.Builder;
// import feast.types.ValueProto.Value;
// import io.opentracing.Scope;
// import io.opentracing.Tracer;
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.stream.Collectors;
// import lombok.extern.slf4j.Slf4j;
// import redis.clients.jedis.Jedis;
// import redis.clients.jedis.JedisPool;
//
// @Slf4j
// public class RedisServingService implements ServingService {
//
//   private final JedisPool jedisPool;
//   private final Tracer tracer;
//
//   public RedisServingService(JedisPool jedisPool, Tracer tracer) {
//     this.jedisPool = jedisPool;
//     this.tracer = tracer;
//   }
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public GetFeastServingTypeResponse getFeastServingType() {
// //    return GetFeastServingTypeResponse.newBuilder().setType().build();
//     return null;
//   }
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public GetOnlineFeaturesResponse getOnlineFeatures(GetFeaturesRequest request) {
//     try (Scope scope = tracer.buildSpan("Redis-getOnlineFeatures").startActive(true)) {
//       List<String> entityNames = request.getEntityDataSet().getFieldNamesList();
//       List<EntityDataSetRow> entityDataSetRows = request.getEntityDataSet()
//           .getEntityDataSetRowsList();
//       GetOnlineFeaturesResponse.Builder getOnlineFeatureResponseBuilder = GetOnlineFeaturesResponse
//           .newBuilder();
//
//       // Create a list of keys to be fetched from Redis
//       List<FeatureSet> featureSets = request.getFeatureSetsList();
//       for (FeatureSet featureSet : featureSets) {
//         List<RedisKey> redisKeys = getRedisKeys(entityNames, entityDataSetRows, featureSet);
//
//         // Convert ProtocolStringList to list of Strings
//         List<String> requestedColumns = featureSet.getFeatureNamesList()
//             .asByteStringList().stream()
//             .map(ByteString::toStringUtf8)
//             .collect(Collectors.toList());
//
//         List<FeatureRow> featureRows = new ArrayList<>();
//         try {
//           featureRows = sendAndProcessMultiGet(redisKeys, requestedColumns, featureSet);
//         } catch (InvalidProtocolBufferException e) {
//           log.error("Unable to parse protobuf", e);
//           throw new FeatureRetrievalException("Unable to parse protobuf while retrieving feature",
//               e);
//         } finally {
//           FeatureDataSet featureDataSet = FeatureDataSet.newBuilder().setName(featureSet.getName())
//               .setVersion(featureSet.getVersion()).addAllFeatureRows(featureRows).build();
//           getOnlineFeatureResponseBuilder.addFeatureDataSets(featureDataSet);
//         }
//       }
//       return getOnlineFeatureResponseBuilder.build();
//     }
//   }
//
//   private List<RedisKey> getRedisKeys(List<String> entityNames,
//       List<EntityDataSetRow> entityDataSetRows, FeatureSet featureSet) {
//     try (Scope scope = tracer.buildSpan("Redis-makeRedisKeys").startActive(true)) {
//       String featureSetId = String.format("%s:%s", featureSet.getName(), featureSet.getVersion());
//       List<RedisKey> redisKeys = entityDataSetRows.parallelStream()
//           .map(row -> makeRedisKey(featureSetId, entityNames, row))
//           .collect(Collectors.toList());
//       return redisKeys;
//     }
//   }
//
//   @Override
//   public GetBatchFeaturesResponse getBatchFeatures(GetFeaturesRequest request) {
//     // Not implemented
//     return null;
//   }
//
//   @Override
//   public GetStatusResponse getBatchFeaturesJobStatus(GetStatusRequest request) {
//     // Not implemented
//     return null;
//   }
//
//   @Override
//   public GetDownloadUrlResponse getBatchFeaturesDownloadUrl(GetDownloadUrlRequest request) {
//     return null;
//   }
//
//   @Override
//   public GetUploadUrlResponse getBatchFeaturesJobUploadUrl(GetUploadUrlRequest request) {
//     // Not implemented
//     return null;
//   }
//
//   @Override
//   public SetUploadCompleteResponse setBatchFeaturesJobUploadComplete(
//       SetUploadCompleteRequest request) {
//     // Not implemented
//     return null;
//   }
//
//   /**
//    * Create a list of {@link FeatureRow}
//    *
//    * @param redisKeys list of {@link RedisKey} to be retrieved from Redis
//    * @param requestedColumns list of String of requested entity and feature names
//    * @param featureSet {@link FeatureSet} so that featureSetName and featureSerVersion can be
//    * retrieved
//    * @return list of {@link FeatureRow}
//    * @throws InvalidProtocolBufferException Exception that is thrown the FeatureRow cannot be parsed
//    * from the byte array response
//    */
//   private List<FeatureRow> sendAndProcessMultiGet(List<RedisKey> redisKeys,
//       List<String> requestedColumns, FeatureSet featureSet) throws InvalidProtocolBufferException {
//     List<byte[]> jedisResps = sendMultiGet(redisKeys);
//
//     try (Scope scope = tracer.buildSpan("Redis-processResponse").startActive(true)) {
//       List<FeatureRow> featureRows = new ArrayList<>();
//       String featureSetName = String.format("%s:%s", featureSet.getName(), featureSet.getVersion());
//       for (int i = 0; i < jedisResps.size(); i++) {
//         featureRows.add(
//             buildFeatureRow(jedisResps.get(i), featureSetName, redisKeys.get(i).getEntitiesList(),
//                 requestedColumns));
//       }
//       return featureRows;
//     }
//   }
//
//   /**
//    * Build a featureRow given the request and the
//    * @param jedisResponse
//    * @param featureSet
//    * @param entities
//    * @param requestedColumns
//    * @return
//    * @throws InvalidProtocolBufferException
//    */
//   private FeatureRow buildFeatureRow(byte[] jedisResponse, String featureSet, List<Field> entities,
//       List<String> requestedColumns) throws InvalidProtocolBufferException {
//     Builder featureRowBuilder = FeatureRow.newBuilder()
//         .setFeatureSet(featureSet)
//         .addAllFields(entities)
//         .setEventTimestamp(Timestamp.newBuilder().setSeconds(0).build());
//
//     if (jedisResponse == null) {
//       for (String requestedColumn : requestedColumns) {
//         featureRowBuilder.addFields(Field.newBuilder().setName(requestedColumn));
//       }
//       return featureRowBuilder.build();
//     }
//     FeatureRow featureRow = FeatureRow.parseFrom(jedisResponse);
//     List<Field> fields = featureRow.getFieldsList().stream()
//         .filter(f -> requestedColumns.contains(f.getName())).collect(Collectors.toList());
//     return featureRowBuilder
//         .addAllFields(entities)
//         .addAllFields(fields)
//         .setEventTimestamp(featureRow.getEventTimestamp())
//         .setFeatureSet(featureSet)
//         .build();
//   }
//
//   /**
//    * Send a list of get request as an mget
//    *
//    * @param keys list of {@link RedisKey}
//    * @return list of {@link FeatureRow} in primitive byte representation for each {@link RedisKey}
//    */
//   private List<byte[]> sendMultiGet(List<RedisKey> keys) {
//     try (Scope scope = tracer.buildSpan("Redis-sendMultiGet").startActive(true)) {
//       try (Jedis jedis = jedisPool.getResource()) {
//         byte[][] binaryKeys =
//             keys.stream()
//                 .map(AbstractMessageLite::toByteArray)
//                 .collect(Collectors.toList())
//                 .toArray(new byte[0][0]);
//         return jedis.mget(binaryKeys);
//       } catch (Exception e) {
//         log.error("Exception while retrieving feature from Redis", e);
//         throw new FeatureRetrievalException("Unable to retrieve feature from Redis", e);
//       }
//     }
//   }
//
//   /**
//    * Create {@link RedisKey}
//    *
//    * @param featureSet featureSet reference of the feature. E.g. feature_set_1:1
//    * @param entityNames list of entityName
//    * @param entityDataSetRow entityDataSetRow to build the key from
//    * @return {@link RedisKey}
//    */
//   private RedisKey makeRedisKey(String featureSet, List<String> entityNames,
//       EntityDataSetRow entityDataSetRow) {
//     RedisKey.Builder builder = RedisKey.newBuilder().setFeatureSet(featureSet);
//     for (int i = 0; i < entityNames.size(); i++) {
//       String entityName = entityNames.get(i);
//       Value entityVal = entityDataSetRow.getValue(i);
//       builder.addEntities(Field.newBuilder().setName(entityName).setValue(entityVal));
//     }
//     return builder.build();
//   }
//
// }
