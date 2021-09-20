package org.acme.infinispan.client;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(includeClasses = {ShopItem.class}, schemaPackageName = "quarkus_qe")
interface ShopItemSchema extends GeneratedSchema {
}