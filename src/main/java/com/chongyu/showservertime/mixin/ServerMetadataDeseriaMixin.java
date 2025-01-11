package com.chongyu.showservertime.mixin;

import com.chongyu.showservertime.core.IServerMetadataAssor;
import com.google.gson.*;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Text;
import net.minecraft.util.JsonHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.lang.reflect.Type;

@Mixin(targets = "net.minecraft.server.ServerMetadata$Deserializer")
public abstract class ServerMetadataDeseriaMixin  implements JsonDeserializer<ServerMetadata>, JsonSerializer<ServerMetadata>{

    //该注入方式无法同步新加的时间
//    @Inject(at = @At("HEAD"), method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/server/ServerMetadata;")
//    public void deserialize(JsonElement functionJson, Type unused, JsonDeserializationContext context, CallbackInfoReturnable<ServerMetadata> cir) {
//        JsonObject jsonobjectTime = JsonHelper.asObject(functionJson, "status");
//        ServerMetadata servermetadata = new ServerMetadata();
//
//        if (jsonobjectTime.has("timeAliveWell")) {
//            ((IServerMetadataAssor)(Object)servermetadata).aliveandwell$setTime(JsonHelper.getInt(jsonobjectTime, "timeAliveWell"));
//        }
//    }
//    @Inject(at = @At("HEAD"), method = "serialize(Lnet/minecraft/server/ServerMetadata;Ljava/lang/reflect/Type;Lcom/google/gson/JsonSerializationContext;)Lcom/google/gson/JsonElement;")
//    public void serialize(ServerMetadata serverMetadata, Type type, JsonSerializationContext jsonSerializationContext, CallbackInfoReturnable<JsonElement> cir){
//        JsonObject jsonObjecttimeAliveWell = new JsonObject();
//
//        jsonObjecttimeAliveWell.addProperty("timeAliveWell", ((IServerMetadataAssor)(Object)serverMetadata).aliveandwell$getTime());
//    }

    @Overwrite
    public ServerMetadata deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = JsonHelper.asObject(jsonElement, "status");
        ServerMetadata serverMetadata = new ServerMetadata();
        if (jsonObject.has("description")) {
            serverMetadata.setDescription((Text)jsonDeserializationContext.deserialize(jsonObject.get("description"), (Type)((Object)Text.class)));
        }
        if (jsonObject.has("players")) {
            serverMetadata.setPlayers((ServerMetadata.Players)jsonDeserializationContext.deserialize(jsonObject.get("players"), (Type)((Object) ServerMetadata.Players.class)));
        }
        if (jsonObject.has("version")) {
            serverMetadata.setVersion((ServerMetadata.Version)jsonDeserializationContext.deserialize(jsonObject.get("version"), (Type)((Object) ServerMetadata.Version.class)));
        }
        if (jsonObject.has("favicon")) {
            serverMetadata.setFavicon(JsonHelper.getString(jsonObject, "favicon"));
        }
        if (jsonObject.has("previewsChat")) {
            serverMetadata.setPreviewsChat(JsonHelper.getBoolean(jsonObject, "previewsChat"));
        }
        if (jsonObject.has("enforcesSecureChat")) {
            serverMetadata.setSecureChatEnforced(JsonHelper.getBoolean(jsonObject, "enforcesSecureChat"));
        }

        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        if (jsonObject.has("client_servertime_aliveandwell")) {
            ((IServerMetadataAssor)(Object)serverMetadata).aliveandwell$setTime(JsonHelper.getInt(jsonObject, "client_servertime_aliveandwell"));
        }
        return serverMetadata;
    }

    @Overwrite
    public JsonElement serialize(ServerMetadata serverMetadata, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("previewsChat", serverMetadata.shouldPreviewChat());
        jsonObject.addProperty("enforcesSecureChat", serverMetadata.isSecureChatEnforced());
        if (serverMetadata.getDescription() != null) {
            jsonObject.add("description", jsonSerializationContext.serialize(serverMetadata.getDescription()));
        }
        if (serverMetadata.getPlayers() != null) {
            jsonObject.add("players", jsonSerializationContext.serialize(serverMetadata.getPlayers()));
        }
        if (serverMetadata.getVersion() != null) {
            jsonObject.add("version", jsonSerializationContext.serialize(serverMetadata.getVersion()));
        }
        if (serverMetadata.getFavicon() != null) {
            jsonObject.addProperty("favicon", serverMetadata.getFavicon());
        }

        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        jsonObject.addProperty("client_servertime_aliveandwell", ((IServerMetadataAssor)(Object)serverMetadata).aliveandwell$getTime());
        return jsonObject;
    }
}
