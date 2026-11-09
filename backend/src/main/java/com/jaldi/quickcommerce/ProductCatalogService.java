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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductCatalogService {
    private static final Pattern NON_TAG_CHARACTERS = Pattern.compile("[^\\p{L}\\p{M}\\p{N} -]");
    private final ProductCatalogStore store;
    private final ProductSearchService productSearch;

    public ProductCatalogService(ProductCatalogStore store, ProductSearchService productSearch) {
        this.store = store;
        this.productSearch = productSearch;
    }

    static List<Product> seedProducts() {
        return List.of(
            new Product("banana", "Robusta Banana", "Farm Fresh", "Fruits & veg", "6 pcs", 42, 50, 4.8, 284, "FRESH PICK", "🍌", "#f7f0d7"),
            localizedSeedProduct("milk", "Full Cream Milk", "Amul", "Dairy & eggs", "500 ml",
                    34, 36, 4.9, 512, "BESTSELLER", "🥛", "#e7edf4",
                    List.of("milk", "full cream milk", "doodh", "dudh", "paal", "cow milk"),
                    List.of("doodh", "dudh", "dugdh", "paal"),
                    List.of("दूध", "दुग्ध")),
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
            new Product("dishwash", "Lemon Dishwash Bar", "Vim", "Cleaning & home", "200 g", 20, 23, 4.4, 107, "", "🧼", "#e6efd9"),
            localizedSeedProduct("basmati-rice", "Everyday Basmati Rice", "Daawat", "Atta, rice & dal", "1 kg",
                    115, 130, 4.7, 188, "PANTRY PICK", "🍚", "#f2ecdc",
                    List.of("rice", "basmati rice", "chawal", "basmati chawal"),
                    List.of("chawal", "basmati chawal"), List.of("चावल", "बासमती चावल")),
            localizedSeedProduct("onion", "Red Onions", "Farm Fresh", "Fruits & veg", "1 kg",
                    32, 40, 4.4, 246, "FRESH PICK", "🧅", "#f1e4e6",
                    List.of("onion", "red onion", "pyaaz", "pyaz"),
                    List.of("pyaaz", "pyaz"), List.of("प्याज", "लाल प्याज")),
            localizedSeedProduct("potato", "Fresh Potatoes", "Farm Fresh", "Fruits & veg", "1 kg",
                    29, 35, 4.5, 302, "FRESH PICK", "🥔", "#eee7d9",
                    List.of("potato", "potatoes", "aloo"),
                    List.of("aloo"), List.of("आलू")),
            localizedSeedProduct("cucumber", "Green Cucumbers", "Farm Fresh", "Fruits & veg", "500 g",
                    24, 30, 4.4, 91, "", "🥒", "#e2edda",
                    List.of("cucumber", "kheera", "salad cucumber"),
                    List.of("kheera"), List.of("खीरा", "ककड़ी")),
            localizedSeedProduct("apple", "Crisp Apples", "Farm Fresh", "Fruits & veg", "4 pcs",
                    119, 140, 4.6, 166, "JUST IN", "🍎", "#f4e3dc",
                    List.of("apple", "apples", "seb"),
                    List.of("seb"), List.of("सेब")),
            localizedSeedProduct("mango", "Seasonal Mangoes", "Farm Fresh", "Fruits & veg", "2 pcs",
                    99, 120, 4.7, 143, "SEASONAL", "🥭", "#f5eadb",
                    List.of("mango", "mangoes", "aam"),
                    List.of("aam"), List.of("आम")),
            localizedSeedProduct("curd", "Natural Set Curd", "Amul", "Dairy & eggs", "400 g",
                    39, 45, 4.6, 139, "", "🥣", "#e7edf4",
                    List.of("curd", "dahi", "yogurt", "yoghurt"),
                    List.of("dahi"), List.of("दही", "योगर्ट")),
            localizedSeedProduct("butter", "Salted Butter", "Amul", "Dairy & eggs", "100 g",
                    62, 65, 4.8, 411, "BESTSELLER", "🧈", "#f5eadb",
                    List.of("butter", "makhan", "salted butter"),
                    List.of("makhan"), List.of("मक्खन", "बटर")),
            localizedSeedProduct("sugar", "Fine White Sugar", "Madhur", "Atta, rice & dal", "1 kg",
                    48, 55, 4.5, 188, "", "🧂", "#f5f2e9",
                    List.of("sugar", "cheeni", "chini", "shakkar"),
                    List.of("cheeni", "chini", "shakkar"), List.of("चीनी", "शक्कर")),
            localizedSeedProduct("salt", "Iodized Table Salt", "Tata Salt", "Atta, rice & dal", "1 kg",
                    26, 30, 4.7, 293, "EVERYDAY ESSENTIAL", "🧂", "#e7edf4",
                    List.of("salt", "table salt", "namak", "iodized salt"),
                    List.of("namak"), List.of("नमक")),
            localizedSeedProduct("tea", "Everyday Assam Tea", "Tata Tea", "Beverages", "250 g",
                    125, 140, 4.6, 231, "BESTSELLER", "🍵", "#e9efd9",
                    List.of("tea", "chai", "assam tea", "tea leaves"),
                    List.of("chai"), List.of("चाय")),
            localizedSeedProduct("coffee", "Instant Coffee", "Continental", "Beverages", "100 g",
                    145, 165, 4.5, 121, "", "☕", "#eee3da",
                    List.of("coffee", "instant coffee", "kaapi"),
                    List.of("coffee", "kaapi"), List.of("कॉफी")),
            localizedSeedProduct("sunflower-oil", "Sunflower Cooking Oil", "Fortune", "Atta, rice & dal", "1 L",
                    139, 155, 4.5, 174, "", "🫗", "#f4e5cf",
                    List.of("cooking oil", "sunflower oil", "oil", "tel"),
                    List.of("tel"), List.of("तेल", "खाना पकाने का तेल")),
            localizedSeedProduct("ghee", "Pure Cow Ghee", "Amul", "Dairy & eggs", "500 ml",
                    315, 340, 4.8, 208, "TOP RATED", "🫙", "#f5eadb",
                    List.of("ghee", "cow ghee", "clarified butter", "घी"),
                    List.of("ghee"), List.of("घी")),
            localizedSeedProduct("masoor-dal", "Whole Masoor Dal", "Tata Sampann", "Atta, rice & dal", "500 g",
                    72, 82, 4.6, 96, "", "🫘", "#eee6d4",
                    List.of("masoor dal", "red lentils", "lentils", "masoor"),
                    List.of("masoor dal", "masoor"), List.of("मसूर दाल")),
            localizedSeedProduct("water", "Packaged Drinking Water", "Bisleri", "Beverages", "1 L",
                    20, 20, 4.6, 368, "EVERYDAY ESSENTIAL", "💧", "#e7edf4",
                    List.of("water", "drinking water", "mineral water", "paani", "pani"),
                    List.of("paani", "pani", "peene ka paani"), List.of("पानी", "पीने का पानी")),
            localizedSeedProduct("lemon", "Fresh Lemons", "Farm Fresh", "Fruits & veg", "4 pcs",
                    22, 28, 4.4, 81, "", "🍋", "#f3efd9",
                    List.of("lemon", "lemons", "nimbu"),
                    List.of("nimbu"), List.of("नींबू")),
            localizedSeedProduct("detergent", "Daily Wash Detergent Powder", "Surf Excel", "Cleaning & home", "1 kg",
                    115, 130, 4.6, 215, "", "🫧", "#e7edf4",
                    List.of("detergent", "washing powder", "laundry detergent", "kapde dhone ka powder"),
                    List.of("kapde dhone ka powder", "washing powder"), List.of("कपड़े धोने का पाउडर", "डिटर्जेंट")),
            localizedSeedProduct("bath-soap", "Moisturising Bath Soap", "Dove", "Personal care", "3 x 100 g",
                    145, 160, 4.7, 193, "", "🧼", "#e7edf4",
                    List.of("soap", "bath soap", "sabun"),
                    List.of("sabun"), List.of("साबुन")),
            localizedSeedProduct("shampoo", "Daily Care Shampoo", "Dove", "Personal care", "180 ml",
                    165, 185, 4.6, 142, "", "🧴", "#f1e4e6",
                    List.of("shampoo", "hair shampoo", "baalon ka shampoo"),
                    List.of("shampoo", "baalon ka shampoo"), List.of("शैम्पू", "बालों का शैम्पू")),
            localizedSeedProduct("toothpaste", "Mint Toothpaste", "Colgate", "Personal care", "150 g",
                    95, 110, 4.6, 287, "FAMILY PICK", "🪥", "#e2edda",
                    List.of("toothpaste", "tooth paste", "dant manjan"),
                    List.of("toothpaste", "dant manjan"), List.of("टूथपेस्ट", "दंत मंजन")),
            localizedSeedProduct("floor-cleaner", "Citrus Floor Cleaner", "Lizol", "Cleaning & home", "500 ml",
                    105, 120, 4.5, 102, "", "🧴", "#e6efd9",
                    List.of("floor cleaner", "floor disinfectant", "lizol", "pocha cleaner"),
                    List.of("pocha cleaner"), List.of("फर्श साफ करने वाला", "फ्लोर क्लीनर")),
            new Product(
                    "mortein-fast-card",
                    "Mortein Fast Card Mosquito Repellent",
                    "Mortein",
                    "Cleaning & home",
                    "10 cards",
                    75,
                    85,
                    4.6,
                    92,
                    "MOSQUITO PROTECTION",
                    "🦟",
                    "#e6efd9",
                    List.of(
                            "mosquito killer", "mosquito repellent", "mosquito control", "mosquitoes",
                            "mosquito bites", "insect repellent", "insect killer", "machar", "machhar",
                            "machar bhagane ka", "machhar bhagane ki dawa", "machhar maarne ka",
                            "kaat rahe hai", "mortein card"),
                    Map.of(
                            "english", List.of("mosquito repellent", "mosquito killer", "insect repellent"),
                            "hinglish", List.of(
                                    "machar", "machhar", "machar bhagane ka", "machhar bhagane ki dawa",
                                    "machhar maarne ka", "machhar se bachao"),
                            "hindi", List.of(
                                    "मच्छर", "मच्छर भगाने की दवा", "मच्छर मारने की दवा", "मच्छर से बचाव")),
                    List.of()),
            new Product(
                    "mortein-liquid-vaporizer",
                    "Mortein Mosquito Repellent Liquid Vaporizer",
                    "Mortein",
                    "Cleaning & home",
                    "45 ml",
                    115,
                    130,
                    4.5,
                    76,
                    "MOSQUITO PROTECTION",
                    "🦟",
                    "#e6efd9",
                    List.of(
                            "mosquito killer", "mosquito repellent",                             "mosquito liquid", "mosquito vaporizer",
                            "insect repellent", "machar", "machhar", "machar bhagane ka",
                            "machhar bhagane ki dawa", "machhar maarne ka", "kaat rahe hai",
                            "mortein liquid"),
                    Map.of(
                            "english", List.of("mosquito repellent", "mosquito killer", "insect repellent"),
                            "hinglish", List.of(
                                    "machar", "machhar", "machar bhagane ka", "machhar bhagane ki dawa",
                                    "machhar maarne ka", "machhar se bachao"),
                            "hindi", List.of(
                                    "मच्छर", "मच्छर भगाने की दवा", "मच्छर मारने की दवा", "मच्छर से बचाव")),
                    List.of()));
    }

    private static Product localizedSeedProduct(
            String id,
            String name,
            String brand,
            String category,
            String unit,
            int price,
            int mrp,
            double rating,
            int reviews,
            String badge,
            String emoji,
            String tint,
            List<String> tags,
            List<String> hinglishTags,
            List<String> hindiTags) {
        return new Product(
                id,
                name,
                brand,
                category,
                unit,
                price,
                mrp,
                rating,
                reviews,
                badge,
                emoji,
                tint,
                tags,
                Map.of("english", tags, "hinglish", hinglishTags, "hindi", hindiTags),
                List.of());
    }

    private final List<Category> categories = List.of(
            new Category("Fruits & veg", "🥬", "#e8f0df"),
            new Category("Dairy & eggs", "🥛", "#e7edf4"),
            new Category("Atta, rice & dal", "🌾", "#f0e9d9"),
            new Category("Snacks & munchies", "🍿", "#f5eadb"),
            new Category("Beverages", "🧃", "#f3e7d8"),
            new Category("Bakery", "🥖", "#f0e6d8"),
            new Category("Cleaning & home", "🧼", "#e8eee1"),
            new Category("Personal care", "🧴", "#f1e4e6"));

    public List<Product> search(String query, String category, String sort) {
        String searchTerm = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String selectedCategory = category == null ? "" : category.trim();
        Comparator<Product> comparator = switch (sort == null ? "popular" : sort) {
            case "price-low" -> Comparator.comparingInt(Product::price);
            case "price-high" -> Comparator.comparingInt(Product::price).reversed();
            default -> Comparator.comparingInt(Product::reviews).reversed();
        };

        List<Product> products = store.allProducts();
        List<String> matchingProductIds = productSearch.searchIds(searchTerm, products);
        return products.stream()
                .filter(product -> selectedCategory.isBlank()
                        || selectedCategory.equalsIgnoreCase("All items")
                        || product.category().equalsIgnoreCase(selectedCategory))
                .filter(product -> searchTerm.isBlank() || matchingProductIds.contains(product.id()))
                .sorted(comparator)
                .toList();
    }

    public List<Product> allProducts() {
        return store.allProducts().stream()
                .sorted(Comparator.comparing(Product::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Product addProduct(ProductDraft draft) {
        Product savedProduct = store.save(createProduct(draft));
        productSearch.index(savedProduct);
        return savedProduct;
    }

    public Product createProduct(ProductDraft draft) {
        String baseId = draft.name().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        String id = baseId + "-" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, LinkedHashSet<String>> mergedLanguageTags = new HashMap<>();
        draft.languageTags().forEach((language, tags) -> mergedLanguageTags.put(
                language,
                tags.stream().map(String::trim).filter(tag -> !tag.isEmpty())
                        .collect(Collectors.toCollection(LinkedHashSet::new))));
        previewTags(draft.name(), draft.brand(), draft.category()).forEach((language, tags) ->
                mergedLanguageTags.computeIfAbsent(language, ignored -> new LinkedHashSet<>()).addAll(tags));
        Map<String, List<String>> languageTags = mergedLanguageTags.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
        LinkedHashSet<String> allTags = draft.tags().stream()
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        languageTags.values().forEach(allTags::addAll);
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
                List.copyOf(allTags),
                languageTags,
                draft.stockCities().stream().map(String::trim).filter(city -> !city.isEmpty()).distinct().toList());
        return product;
    }

    public Product saveProduct(Product product) {
        return store.save(product);
    }

    public boolean productExists(String id) {
        return store.existsById(id);
    }

    public void deleteProduct(String id) {
        if (!store.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product was not found.");
        }
        productSearch.delete(id);
        store.deleteById(id);
    }

    public static Product addKnownSearchAliases(Product product) {
        Map<String, LinkedHashSet<String>> mergedLanguageTags = new HashMap<>();
        product.languageTags().forEach((language, tags) -> mergedLanguageTags.put(
                language,
                tags.stream().map(String::trim).filter(tag -> !tag.isEmpty())
                        .collect(Collectors.toCollection(LinkedHashSet::new))));
        previewTags(product.name(), product.brand(), product.category()).forEach((language, tags) ->
                mergedLanguageTags.computeIfAbsent(language, ignored -> new LinkedHashSet<>()).addAll(tags));
        Map<String, List<String>> languageTags = mergedLanguageTags.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
        LinkedHashSet<String> allTags = product.tags().stream()
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        languageTags.values().forEach(allTags::addAll);
        return new Product(
                product.id(), product.name(), product.brand(), product.category(), product.unit(),
                product.price(), product.mrp(), product.rating(), product.reviews(), product.badge(),
                product.emoji(), product.tint(), List.copyOf(allTags), languageTags, product.stockCities());
    }

    public static Map<String, List<String>> previewTags(String name, String brand, String category) {
        Map<String, LinkedHashSet<String>> tagsByLanguage = new HashMap<>();
        for (String language : List.of(
                "english", "hinglish", "hindi", "marathi", "bengali", "assamese", "gujarati",
                "punjabi", "odia", "tamil", "telugu", "kannada", "malayalam", "typos", "aiWords")) {
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

    private static void addKnownTranslations(String name, Map<String, LinkedHashSet<String>> tagsByLanguage) {
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
                        Map.entry("malayalam", "ചീസ്")),
                "milk", Map.ofEntries(
                        Map.entry("hindi", "दूध"), Map.entry("marathi", "दूध"),
                        Map.entry("bengali", "দুধ"), Map.entry("assamese", "গাখীৰ"),
                        Map.entry("gujarati", "દૂધ"), Map.entry("punjabi", "ਦੁੱਧ"),
                        Map.entry("odia", "ଦୁଧ"), Map.entry("tamil", "பால்"),
                        Map.entry("telugu", "పాలు"), Map.entry("kannada", "ಹಾಲು"),
                        Map.entry("malayalam", "പാൽ")),
                "water", Map.ofEntries(
                        Map.entry("hindi", "पानी"), Map.entry("marathi", "पाणी"),
                        Map.entry("bengali", "জল"), Map.entry("assamese", "পানী"),
                        Map.entry("gujarati", "પાણી"), Map.entry("punjabi", "ਪਾਣੀ"),
                        Map.entry("odia", "ପାଣି"), Map.entry("tamil", "தண்ணீர்"),
                        Map.entry("telugu", "నీరు"), Map.entry("kannada", "ನೀರು"),
                        Map.entry("malayalam", "വെള്ളം")),
                "banana", Map.ofEntries(
                        Map.entry("hindi", "केला"), Map.entry("marathi", "केळी"),
                        Map.entry("bengali", "কলা"), Map.entry("assamese", "কল"),
                        Map.entry("gujarati", "કેળું"), Map.entry("punjabi", "ਕੇਲਾ"),
                        Map.entry("odia", "କଦଳୀ"), Map.entry("tamil", "வாழைப்பழம்"),
                        Map.entry("telugu", "అరటిపండు"), Map.entry("kannada", "ಬಾಳೆಹಣ್ಣು"),
                        Map.entry("malayalam", "വാഴപ്പഴം")),
                "tomato", Map.ofEntries(
                        Map.entry("hindi", "टमाटर"), Map.entry("marathi", "टोमॅटो"),
                        Map.entry("bengali", "টমেটো"), Map.entry("assamese", "বিলাহী"),
                        Map.entry("gujarati", "ટામેટું"), Map.entry("punjabi", "ਟਮਾਟਰ"),
                        Map.entry("odia", "ଟମାଟୋ"), Map.entry("tamil", "தக்காளி"),
                        Map.entry("telugu", "టమాటా"), Map.entry("kannada", "ಟೊಮೇಟೊ"),
                        Map.entry("malayalam", "തക്കാളി")),
                "mango", Map.ofEntries(
                        Map.entry("hindi", "आम"), Map.entry("marathi", "आंबा"),
                        Map.entry("bengali", "আম"), Map.entry("assamese", "আম"),
                        Map.entry("gujarati", "કેરી"), Map.entry("punjabi", "ਅੰਬ"),
                        Map.entry("odia", "ଆମ୍ବ"), Map.entry("tamil", "மாம்பழம்"),
                        Map.entry("telugu", "మామిడి"), Map.entry("kannada", "ಮಾವು"),
                        Map.entry("malayalam", "മാങ്ങ")));
        String normalizedName = name.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Map<String, String>> term : translations.entrySet()) {
            if (normalizedName.contains(term.getKey())) {
                term.getValue().forEach((language, translation) -> tagsByLanguage.get(language).add(translation));
                tagsByLanguage.get("hinglish").add(switch (term.getKey()) {
                    case "milk" -> "doodh";
                    case "water" -> "pani";
                    case "banana" -> "kela";
                    case "tomato" -> "tamatar";
                    case "mango" -> "aam";
                    default -> "cheez";
                });
                if (term.getKey().equals("water")) {
                    tagsByLanguage.get("hinglish").addAll(List.of("paani", "peene ka pani"));
                }
            }
        }
    }

    public List<Category> categories() {
        return categories;
    }
}
