package com.jaldi.quickcommerce;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "spring.security.user.name=admin",
        "spring.security.user.password=jaldi123",
        "jaldi.gemma.provider=remote"
})
@AutoConfigureMockMvc
class AdminProductControllerTest {
    private static final String ADMIN_AUTHORIZATION = "Basic " + Base64.getEncoder().encodeToString(
            "admin:jaldi123".getBytes(StandardCharsets.UTF_8));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminCatalogRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanAddProductAndItAppearsInShopSearch() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Alphonso Mango",
                                  "brand": "Farm Fresh",
                                  "category": "Fruits & veg",
                                  "unit": "3 pcs",
                                  "price": 199,
                                  "mrp": 220,
                                  "badge": "JUST IN",
                                  "emoji": "🥭",
                                  "tint": "#f7f0d7",
                                  "tags": ["mango", "seasonal"],
                                  "languageTags": {
                                    "english": ["mango"],
                                    "hindi": ["आम"]
                                  },
                                  "stockCities": ["Mumbai"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alphonso Mango"))
                .andExpect(jsonPath("$.tags[0]").value("mango"))
                .andExpect(jsonPath("$.languageTags.hindi[0]").value("आम"))
                .andExpect(jsonPath("$.stockCities[0]").value("Mumbai"));

        mockMvc.perform(get("/api/search").param("q", "Alphonso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alphonso Mango"));

        mockMvc.perform(get("/api/search").param("q", "seasonal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(hasItem("Alphonso Mango")));
    }

    @Test
    void adminCanDeleteProductFromCatalogAndSearch() throws Exception {
        var response = mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Temporary Delete Test Item",
                                  "brand": "Test",
                                  "category": "Fruits & veg",
                                  "unit": "1 pc",
                                  "price": 10,
                                  "mrp": 10,
                                  "badge": "",
                                  "emoji": "🍎",
                                  "tint": "#edf2e4",
                                  "tags": ["temporary-delete-test"],
                                  "languageTags": {},
                                  "stockCities": []
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String productId = objectMapper.readValue(response.getResponse().getContentAsString(), Product.class).id();

        mockMvc.perform(delete("/api/admin/products/{productId}", productId)
                        .header("Authorization", ADMIN_AUTHORIZATION))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", ADMIN_AUTHORIZATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.not(hasItem(productId))));
        mockMvc.perform(get("/api/search").param("q", "temporary-delete-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void productPriceMustNotExceedMrp() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test item",
                                  "brand": "",
                                  "category": "Fruits & veg",
                                  "unit": "1 pc",
                                  "price": 30,
                                  "mrp": 20,
                                  "badge": "",
                                  "emoji": "🍎",
                                  "tint": "#edf2e4",
                                  "tags": [],
                                  "languageTags": {},
                                  "stockCities": []
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void quickTagRulesSuggestKnownSearchTermsInMoreThanOneLanguage() throws Exception {
        mockMvc.perform(post("/api/admin/products/preview-tags")
                        .header("Authorization", ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Amul cheese slice",
                                  "brand": "Amul",
                                  "category": "Dairy & eggs"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.english").isArray())
                .andExpect(jsonPath("$.hinglish").isArray())
                .andExpect(jsonPath("$.hindi[0]").value("चीज़"))
                .andExpect(jsonPath("$.tamil[0]").value("சீஸ்"));
    }

    @Test
    void gemmaTagGenerationRequiresAdminAuthentication() throws Exception {
        mockMvc.perform(post("/api/admin/products/generate-tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Amul milk","brand":"Amul","category":"Dairy & eggs"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void gemmaTagGenerationReportsWhenServiceIsNotConfigured() throws Exception {
        mockMvc.perform(post("/api/admin/products/generate-tags")
                        .header("Authorization", ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Amul milk","brand":"Amul","category":"Dairy & eggs"}
                                """))
                .andExpect(status().isServiceUnavailable());
    }
}
