package io.fand.api.item.component;

import com.google.gson.JsonElement;

/** A typed API value that can be encoded as a vanilla item data component. */
public interface ItemComponentData {

    JsonElement toJson();
}
