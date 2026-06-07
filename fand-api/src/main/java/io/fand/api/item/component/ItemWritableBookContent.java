package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;

/** Typed value for {@code minecraft:writable_book_content}. */
public record ItemWritableBookContent(List<ItemFilterableText> pages) implements ItemComponentData {

    public static final ItemWritableBookContent EMPTY = new ItemWritableBookContent(List.of());

    public ItemWritableBookContent {
        pages = List.copyOf(Objects.requireNonNull(pages, "pages"));
        if (pages.size() > 100) {
            throw new IllegalArgumentException("pages must contain at most 100 entries");
        }
    }

    public static ItemWritableBookContent fromJson(JsonElement value) {
        var object = ItemComponentJson.objectOrEmpty(value);
        var pages = new java.util.ArrayList<ItemFilterableText>();
        var rawPages = object.get("pages");
        if (rawPages != null && rawPages.isJsonArray()) {
            for (var page : rawPages.getAsJsonArray()) {
                pages.add(ItemFilterableText.fromJson(page));
            }
        }
        return new ItemWritableBookContent(pages);
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        if (!pages.isEmpty()) {
            var pageArray = new JsonArray();
            pages.forEach(page -> pageArray.add(page.toJson()));
            json.add("pages", pageArray);
        }
        return json;
    }
}
