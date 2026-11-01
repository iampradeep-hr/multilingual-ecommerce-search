package com.jaldi.quickcommerce;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ProductCatalogService {
    private static final Pattern NON_TAG_CHARACTERS = Pattern.compile("[^\\p{L}\\p{N} -]");
    private final ProductCatalogStore store;

    public ProductCatalogService(ProductCatalogStore store) {
        this.store = store;
    }

    static List<Product> seedProducts() {
        return List.of(
            new Product("banana", "Robusta Banana", "Farm Fresh", "Fruits & veg", "6 pcs", 42, 50, 4.8, 284, "FRESH PICK", "🍌", "#f7f0d7"),
            new Product("milk", "Full Cream Milk", "Amul", "Dairy & eggs", "500 ml", 34, 36, 4.9, 512, "BESTSELLER", "🥛", "#e7edf4"),
            new Product("avocado", "Premium Avocado", "Farm Fresh", "Fruits & veg", "1 pc", 89, 110, 4.6, 96, "JUST IN", "🥑", "#e9efd9"),
            new Product("eggs", "Classic White Eggs", "Happy Hens", "Dairy & eggs", "6 pcs", 54, 60, 4.7, 203, "", "🥚", "#f5eadb"),
            new Product("atta", "Whole Wheat Atta", "Aashirvaad", "Atta, rice & dal", "1 kg", 68, 75, 4.8, 391, "TOP RATED", "🌾", "#eee7d9"),
            new Product("tomato", "Local Red Tomatoes", "Farm Fresh", "Fruits & veg", "500 g", 28, 35, 4.5, 147, "FRESH PICK", "🍅", "#f4e3dc"),
            new Product("bread", "Whole Wheat Bread", "Harvest Gold", "Bakery", "400 g", 45, 50, 4.6, 118, "", "🍞", "#f2e7d6"),
            new Product("chips", "Classic Salted Chips", "Lay's", "Snacks & munchies", "52 g", 20, 20, 4.4, 328, "QUICK BITE", "🥔", "#f3ead4"),
            new Product("orange-juice", "Mosambi Juice", "Paper Boat", "Beverages", "250 ml", 35, 40, 4.6, 83, "", "🧃", "#f4e5cf"),
            new Product("paneer", "Fresh Malai Paneer", "Amul", "Dairy & eggs", "200 g", 89, 95, 4.8, 176, "FRESH PICK", "🧀", "#f2ecdc"),
            new Product("spinach", "Baby Spinach", "Farm Fresh", "Fruits & veg", "100 g", 29, 35, 4.5, 74, "", "🥬", "#e2edda"),
            new Product("dal", "Toor Dal", "Tata Sampann", "Atta, rice & dal", "500 g", 82, 90, 4.7, 156, "", "🫘", "#eee6d4"),
            new Product("cookies", "Choco Chip Cookies", "Sunfeast", "Snacks & munchies", "120 g", 35, 40, 4.5, 221, "", "🍪", "#f2e4d8"),
            new Product("dishwash", "Lemon Dishwash Bar", "Vim", "Cleaning & home", "200 g", 20, 23, 4.4, 107, "", "🧼", "#e6efd9"));
    }

    private final List<Category> categories = List.of(
            new Category("Fruits & veg", "🥬", "#e8f0df"),
            new Category("Dairy & eggs", "🥛", "#e7edf4"),
            new Category("Atta, rice & dal", "🌾", "#f0e9d9"),
            new Category("Snacks & munchies", "🍿", "#f5eadb"),
            new Category("Beverages", "🧃", "#f3e7d8"),
            new Category("Bakery", "🥖", "#f0e6d8"),
            new Category("Cleaning & home", "🧼", "#e8eee1"));

    public List<Product> search(String query, String category, String sort) {
        String searchTerm = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String selectedCategory = category == null ? "" : category.trim();
        Comparator<Product> comparator = switch (sort == null ? "popular" : sort) {
            case "price-low" -> Comparator.comparingInt(Product::price);
            case "price-high" -> Comparator.comparingInt(Product::price).reversed();
            default -> Comparator.comparingInt(Product::reviews).reversed();
        };

        return store.allProducts().stream()
                .filter(product -> selectedCategory.isBlank()
                        || selectedCategory.equalsIgnoreCase("All items")
                        || product.category().equalsIgnoreCase(selectedCategory))
                .filter(product -> searchTerm.isBlank()
                        || (product.name() + " " + product.brand() + " " + product.category() + " "
                                + String.join(" ", product.tags()) + " "
                                + String.join(" ", product.languageTags().values().stream().flatMap(List::stream).toList()))
                                .toLowerCase(Locale.ROOT)
                                .contains(searchTerm))
                .sorted(comparator)
                .toList();
    }

    public List<Product> allProducts() {
        return store.allProducts().stream()
                .sorted(Comparator.comparing(Product::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Product addProduct(ProductDraft draft) {
        String baseId = draft.name().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        String id = baseId + "-" + UUID.randomUUID().toString().substring(0, 8);
        Product product = new Product(
                id,
                draft.name().trim(),
                draft.brand() == null || draft.brand().isBlank() ? "Local store" : draft.brand().trim(),
                draft.category().trim(),
                draft.unit().trim(),
                draft.price(),
                draft.mrp(),
                0,
                0,
                draft.badge() == null ? "" : draft.badge().trim(),
                draft.emoji().trim(),
                draft.tint(),
                draft.tags().stream().map(String::trim).filter(tag -> !tag.isEmpty()).distinct().toList(),
                draft.languageTags().entrySet().stream()
                        .collect(Collectors.toUnmodifiableMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().stream().map(String::trim).filter(tag -> !tag.isEmpty()).distinct().toList())),
                draft.stockCities().stream().map(String::trim).filter(city -> !city.isEmpty()).distinct().toList());
        return store.save(product);
    }

    public Map<String, List<String>> previewTags(String name, String brand, String category) {
        Map<String, LinkedHashSet<String>> tagsByLanguage = new HashMap<>();
        for (String language : List.of(
                "english", "hinglish", "hindi", "marathi", "bengali", "assamese", "gujarati",
                "punjabi", "odia", "tamil", "telugu", "kannada", "malayalam", "urdu", "typos", "aiWords")) {
            tagsByLanguage.put(language, new LinkedHashSet<>());
        }

        for (String value : new String[] {name, brand, category}) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String cleaned = NON_TAG_CHARACTERS.matcher(value).replaceAll(" ").toLowerCase(Locale.ROOT);
            for (String tag : cleaned.split("\\s+")) {
                if (tag.length() > 1) {
                    tagsByLanguage.get("english").add(tag);
                    tagsByLanguage.get("hinglish").add(tag);
                }
            }
            tagsByLanguage.get("english").add(cleaned.trim().replaceAll("\\s+", " "));
        }

        addKnownTranslations(name, tagsByLanguage);
        if (name != null && name.toLowerCase(Locale.ROOT).contains("cheese")) {
            tagsByLanguage.get("hinglish").addAll(List.of("chiz", "cheese slices", "cheese cubes", "processed cheese"));
            tagsByLanguage.get("typos").addAll(List.of("chees", "cheez", "cheeze"));
        }

        return tagsByLanguage.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> entry.getValue().stream().limit(12).toList()));
    }

    private void addKnownTranslations(String name, Map<String, LinkedHashSet<String>> tagsByLanguage) {
        if (name == null) {
            return;
        }
        Map<String, Map<String, String>> translations = Map.of(
                "cheese", Map.ofEntries(
                        Map.entry("hindi", "चीज़"), Map.entry("marathi", "चीज"),
                        Map.entry("bengali", "চিজ"), Map.entry("assamese", "চিজ"),
                        Map.entry("gujarati", "ચીઝ"), Map.entry("punjabi", "ਚੀਜ਼"),
                        Map.entry("odia", "ଚିଜ"), Map.entry("tamil", "சீஸ்"),
                        Map.entry("telugu", "చీజ్"), Map.entry("kannada", "ಚೀಸ್"),
                        Map.entry("malayalam", "ചീസ്"), Map.entry("urdu", "چیز")),
                "milk", Map.ofEntries(
                        Map.entry("hindi", "दूध"), Map.entry("marathi", "दूध"),
                        Map.entry("bengali", "দুধ"), Map.entry("assamese", "গাখীৰ"),
                        Map.entry("gujarati", "દૂધ"), Map.entry("punjabi", "ਦੁੱਧ"),
                        Map.entry("odia", "ଦୁଧ"), Map.entry("tamil", "பால்"),
                        Map.entry("telugu", "పాలు"), Map.entry("kannada", "ಹಾಲು"),
                        Map.entry("malayalam", "പാൽ"), Map.entry("urdu", "دودھ")),
                "banana", Map.ofEntries(
                        Map.entry("hindi", "केला"), Map.entry("marathi", "केळी"),
                        Map.entry("bengali", "কলা"), Map.entry("assamese", "কল"),
                        Map.entry("gujarati", "કેળું"), Map.entry("punjabi", "ਕੇਲਾ"),
                        Map.entry("odia", "କଦଳୀ"), Map.entry("tamil", "வாழைப்பழம்"),
                        Map.entry("telugu", "అరటిపండు"), Map.entry("kannada", "ಬಾಳೆಹಣ್ಣು"),
                        Map.entry("malayalam", "വാഴപ്പഴം"), Map.entry("urdu", "کیلا")),
                "tomato", Map.ofEntries(
                        Map.entry("hindi", "टमाटर"), Map.entry("marathi", "टोमॅटो"),
                        Map.entry("bengali", "টমেটো"), Map.entry("assamese", "বিলাহী"),
                        Map.entry("gujarati", "ટામેટું"), Map.entry("punjabi", "ਟਮਾਟਰ"),
                        Map.entry("odia", "ଟମାଟୋ"), Map.entry("tamil", "தக்காளி"),
                        Map.entry("telugu", "టమాటా"), Map.entry("kannada", "ಟೊಮೇಟೊ"),
                        Map.entry("malayalam", "തക്കാളി"), Map.entry("urdu", "ٹماٹر")),
                "mango", Map.ofEntries(
                        Map.entry("hindi", "आम"), Map.entry("marathi", "आंबा"),
                        Map.entry("bengali", "আম"), Map.entry("assamese", "আম"),
                        Map.entry("gujarati", "કેરી"), Map.entry("punjabi", "ਅੰਬ"),
                        Map.entry("odia", "ଆମ୍ବ"), Map.entry("tamil", "மாம்பழம்"),
                        Map.entry("telugu", "మామిడి"), Map.entry("kannada", "ಮಾವು"),
                        Map.entry("malayalam", "മാങ്ങ"), Map.entry("urdu", "آم")));
        String normalizedName = name.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Map<String, String>> term : translations.entrySet()) {
            if (normalizedName.contains(term.getKey())) {
                term.getValue().forEach((language, translation) -> tagsByLanguage.get(language).add(translation));
                tagsByLanguage.get("hinglish").add(switch (term.getKey()) {
                    case "milk" -> "doodh";
                    case "banana" -> "kela";
                    case "tomato" -> "tamatar";
                    case "mango" -> "aam";
                    default -> "cheez";
                });
            }
        }
    }

    public List<Category> categories() {
        return categories;
    }
}
