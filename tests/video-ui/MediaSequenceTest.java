package app.morphe.extension.twitter.patches;

import java.util.ArrayList;
import java.util.List;

public final class MediaSequenceTest {
    record Item(String post, String media, String payload) { String key() { return post + ":" + media; } }
    public static void main(String[] args) {
        Item a = new Item("one", "photo1", "old");
        Item b = new Item("one", "video2", "playing");
        Item c = new Item("two", "photo1", "other post");
        ArrayList<Item> original = new ArrayList<>(List.of(a, b));
        List<Item> result = MediaSequence.appendNew(original,
                List.of(new Item("one", "video2", "duplicate"), c, c), Item::key);
        require(result.equals(List.of(a, b, c)));
        require(result.get(original.indexOf(b)) == b); // the playing item and its index survive pagination
        require(original.equals(List.of(a, b))); // caller-owned/native list is not mutated
        require(MediaSequence.appendNew(result, List.of(c, a), Item::key).equals(result)); // repeated page is idempotent
        require(MediaSequence.appendNew(List.<Item>of(), List.of(a, a, b), Item::key).equals(List.of(a, b)));
        require(MediaSequence.appendNew(result, List.<Item>of(), Item::key).equals(result));
    }
    static void require(boolean value) { if (!value) throw new AssertionError(); }
}

