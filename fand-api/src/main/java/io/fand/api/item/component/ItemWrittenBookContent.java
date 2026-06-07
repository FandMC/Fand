package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;

/** Typed value for {@code minecraft:written_book_content}. */
public record ItemWrittenBookContent(
        ItemFilterableText title,
        String author,
        int generation,
        List<ItemFilterableComponent> pages,
        boolean resolved) implements ItemComponentData {

    public ItemWrittenBookContent {
        title = Objects.requireNonNull(title, "title");
        author = Objects.requireNonNull(author, "author");
        pages = List.copyOf(Objects.requireNonNull(pages, "pages"));
        if (generation < 0 || generation > 3) {
            throw new IllegalArgumentException("generation must be in 0..3");
        }
    }

    public ItemWrittenBookContent(String title, String author, List<Component> pages) {
        this(ItemFilterableText.of(title), author, 0, pages.stream().map(ItemFilterableComponent::of).toList(), false);
    }

    public static ItemWrittenBookContent fromJson(JsonElement value) {
        var object = ItemComponentJson.object(value, "written book content");
        var pages = new java.util.ArrayList<ItemFilterableComponent>();
        var rawPages = object.get("pages");
        if (rawPages != null && rawPages.isJsonArray()) {
            for (var page : rawPages.getAsJsonArray()) {
                pages.add(ItemFilterableComponent.fromJson(page));
            }
        }
        return new ItemWrittenBookContent(
                ItemFilterableText.fromJson(object.get("title")),
                object.get("author").getAsString(),
                ItemComponentJson.intOr(object, "generation", 0),
                pages,
                ItemComponentJson.booleanOr(object, "resolved", false));
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        json.add("title", title.toJson());
        json.addProperty("author", author);
        if (generation != 0) {
            json.addProperty("generation", generation);
        }
        if (!pages.isEmpty()) {
            var pageArray = new JsonArray();
            pages.forEach(page -> pageArray.add(page.toJson()));
            json.add("pages", pageArray);
        }
        if (resolved) {
            json.addProperty("resolved", true);
        }
        return json;
    }
}
